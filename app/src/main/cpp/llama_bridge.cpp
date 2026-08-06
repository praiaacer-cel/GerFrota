// app/src/main/cpp/llama_bridge.cpp
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "llama.h"

#define TAG "GerFrotaLlama"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct NativeHandle {
    llama_model*   model  = nullptr;
    llama_context* ctx    = nullptr;
    llama_sampler* sampler= nullptr;
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    llama_backend_init();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeInit(
        JNIEnv* env, jobject, jstring jpath, jint nCtx, jint nThreads) {
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    auto* h = new NativeHandle();

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // CPU (mobile)
    h->model = llama_model_load_from_file(path, mp);   // versões antigas: llama_load_model_from_file
    env->ReleaseStringUTFChars(jpath, path);
    if (!h->model) { LOGE("Falha ao carregar o modelo GGUF"); delete h; return 0; }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx     = (uint32_t) nCtx;
    cp.n_threads = nThreads;
    cp.n_batch   = 512;
    cp.no_perf   = true;
    h->ctx = llama_init_from_model(h->model, cp);
    if (!h->ctx) { llama_model_free(h->model); delete h; return 0; }

    h->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(h->sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(h->sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(h->sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(h->sampler, llama_sampler_init_dist(42));
    return reinterpret_cast<jlong>(h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeGenerate(
        JNIEnv* env, jobject, jlong jh, jstring jprompt, jint maxTokens, jobject jcb) {
    auto* h = reinterpret_cast<NativeHandle*>(jh);
    if (!h || !h->ctx || !h->model) return;

    jclass cbCls   = env->GetObjectClass(jcb);
    jmethodID onToken    = env->GetMethodID(cbCls, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(cbCls, "onComplete", "()V");

    const char* p = env->GetStringUTFChars(jprompt, nullptr);
    std::string prompt(p);
    env->ReleaseStringUTFChars(jprompt, p);

    // Limpa KV-cache da conversa anterior
    // (em versões antigas do llama.cpp use: llama_kv_self_clear(h->ctx);)
    llama_memory_t mem = llama_get_memory(h->ctx);
    if (mem) llama_memory_clear(mem);

    // Tokeniza o prompt ChatML
    std::vector<llama_token> tokens(prompt.size() + 64);
    int n = llama_tokenize(h->model, prompt.c_str(), (int) prompt.size(),
                           tokens.data(), (int) tokens.size(), true, true);
    if (n < 0) { tokens.resize(-n);
        n = llama_tokenize(h->model, prompt.c_str(), (int) prompt.size(),
                           tokens.data(), (int) tokens.size(), true, true); }
    tokens.resize(n);

    // Avalia o prompt em lotes
    for (int i = 0; i < n; i += 512) {
        int cnt = std::min(512, n - i);
        if (llama_decode(h->ctx, llama_batch_get_one(tokens.data() + i, cnt)) != 0) {
            env->CallVoidMethod(jcb, onComplete); return;
        }
    }

    // Geração token a token com streaming para o Kotlin
    for (int i = 0; i < maxTokens; i++) {
        llama_token tok = llama_sampler_sample(h->sampler, h->ctx, -1);
        if (llama_token_is_eog(h->model, tok)) break;
        char buf[256];
        int32_t len = llama_token_to_piece(h->model, tok, buf, sizeof(buf), 0, true);
        if (len > 0) {
            jstring js = env->NewStringUTF(std::string(buf, len).c_str());
            env->CallVoidMethod(jcb, onToken, js);
            env->DeleteLocalRef(js);
        }
        if (llama_decode(h->ctx, llama_batch_get_one(&tok, 1)) != 0) break;
    }
    env->CallVoidMethod(jcb, onComplete);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeFree(JNIEnv*, jobject, jlong jh) {
    auto* h = reinterpret_cast<NativeHandle*>(jh);
    if (!h) return;
    if (h->sampler) llama_sampler_free(h->sampler);
    if (h->ctx)     llama_free(h->ctx);
    if (h->model)   llama_model_free(h->model);
    delete h;
}
