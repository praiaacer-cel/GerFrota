#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "LlamaCppEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Estrutura para manter os ponteiros nativos entre as chamadas JNI
struct NativeHandle {
    llama_model* model;
    llama_context* ctx;
    llama_sampler* sampler;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeInit(
        JNIEnv* env, jobject, jstring jpath, jint n_ctx, jint n_threads) {
    
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    LOGI("Carregando modelo de: %s", path);
    
    llama_backend_init();
    
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // Força uso de CPU (ideal para compatibilidade mobile)
    
    llama_model* model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jpath, path);
    
    if (!model) {
        LOGE("Falha ao carregar o modelo GGUF");
        return 0;
    }
    
    auto cparams = llama_context_default_params();
    cparams.n_ctx = n_ctx;
    cparams.n_threads = n_threads;
    
    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Falha ao criar o contexto");
        llama_model_free(model);
        return 0;
    }

    // Configuração do Sampler (Greedy para respostas diretas)
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    auto* h = new NativeHandle{model, ctx, smpl};
    LOGI("Modelo carregado com sucesso");
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

    // 1. Limpeza do Cache KV (Correção Copilot)
    llama_kv_cache_clear(h->ctx); 
    /* Nota: Se o seu commit do llama.cpp for extremamente recente (2025+) e 
       llama_kv_cache_clear der erro, use: llama_kv_self_clear(h->ctx); */

    // 2. Obter Vocabulário (Correção Copilot)
    const llama_vocab * vocab = llama_model_get_vocab(h->model);

    // 3. Tokenização do Prompt (Correção Copilot)
    std::vector<llama_token> tokens(prompt.size() + 64);
    int n = llama_tokenize(vocab, prompt.c_str(), (int) prompt.size(),
                           tokens.data(), (int) tokens.size(), false);
    if (n < 0) { 
        tokens.resize(-n);
        n = llama_tokenize(vocab, prompt.c_str(), (int) prompt.size(),
                           tokens.data(), (int) tokens.size(), false); 
    }
    tokens.resize(n);

    // Avaliar o prompt em lotes (batches)
    for (int i = 0; i < n; i += 512) {
        int cnt = std::min(512, n - i);
        if (llama_decode(h->ctx, llama_batch_get_one(tokens.data() + i, cnt)) != 0) {
            LOGE("Falha ao decodificar lote do prompt");
            env->CallVoidMethod(jcb, onComplete); 
            return;
        }
    }

    // Geração token-por-token com streaming para o Kotlin
    for (int i = 0; i < maxTokens; i++) {
        llama_token tok = llama_sampler_sample(h->sampler, h->ctx, -1);
        
        // 4. Verificar Fim de Geração (Correção Copilot)
        if (llama_token_is_eog(vocab, tok)) break;
        
        char buf[256];
        // 5. Converter Token para Texto (Correção Copilot)
        int32_t len = llama_token_to_piece(vocab, tok, buf, sizeof(buf), 0, true);
        if (len > 0) {
            jstring js = env->NewStringUTF(std::string(buf, len).c_str());
            env->CallVoidMethod(jcb, onToken, js);
            env->DeleteLocalRef(js);
        }
        
        if (llama_decode(h->ctx, llama_batch_get_one(&tok, 1)) != 0) {
            LOGE("Falha ao decodificar token gerado");
            break;
        }
    }
    env->CallVoidMethod(jcb, onComplete);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeFree(
        JNIEnv*, jobject, jlong jh) {
    auto* h = reinterpret_cast<NativeHandle*>(jh);
    if (h) {
        if (h->sampler) llama_sampler_free(h->sampler);
        if (h->ctx) llama_free(h->ctx);
        if (h->model) llama_model_free(h->model);
        delete h;
    }
    llama_backend_free();
}
