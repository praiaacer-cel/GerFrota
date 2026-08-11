#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "LlamaCppEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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

    // Parâmetros do modelo
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;        // Forçar CPU no Android
    mparams.vocab_only   = false;

    llama_model* model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jpath, path);

    if (!model) {
        LOGE("Falha ao carregar o modelo GGUF");
        llama_backend_free();
        return 0;
    }

    // Parâmetros do contexto
    auto cparams = llama_context_default_params();
    cparams.n_ctx          = n_ctx;
    cparams.n_threads      = n_threads;
    cparams.n_threads_batch = n_threads;  // IMPORTANTE: threads para batch
    cparams.embeddings     = false;       // Modelo causal, não embedding

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Falha ao criar o contexto");
        llama_model_free(model);
        llama_backend_free();
        return 0;
    }

    // Sampler chain
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    auto* h = new NativeHandle{model, ctx, smpl};
    LOGI("Modelo carregado com sucesso (ctx=%d, threads=%d)", n_ctx, n_threads);
    return reinterpret_cast<jlong>(h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeGenerate(
        JNIEnv* env, jobject, jlong jh, jstring jprompt, jint maxTokens, jobject jcb) {

    auto* h = reinterpret_cast<NativeHandle*>(jh);
    if (!h || !h->ctx || !h->model) {
        env->CallVoidMethod(jcb, env->GetMethodID(
            env->GetObjectClass(jcb), "onComplete", "()V"));
        return;
    }

    jclass cbCls       = env->GetObjectClass(jcb);
    jmethodID onToken  = env->GetMethodID(cbCls, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(cbCls, "onComplete", "()V");

    const char* p = env->GetStringUTFChars(jprompt, nullptr);
    std::string prompt(p);
    env->ReleaseStringUTFChars(jprompt, p);

    // 1. Limpar cache KV (API NOVA: Substitui llama_kv_cache_clear)
    llama_memory_clear(llama_get_memory(h->ctx), true);

    // 2. Obter vocabulário
    const llama_vocab* vocab = llama_model_get_vocab(h->model);
    if (!vocab) {
        LOGE("Falha ao obter vocabulário");
        env->CallVoidMethod(jcb, onComplete);
        return;
    }

    // 3. Tokenizar o prompt
    std::vector<llama_token> tokens(prompt.size() + 64);
    int n = llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                           tokens.data(), (int32_t)tokens.size(),
                           /*add_special=*/true, /*parse_special=*/true);
    if (n < 0) {
        tokens.resize(-n);
        n = llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                           tokens.data(), (int32_t)tokens.size(), true, true);
    }
    if (n < 0) {
        LOGE("Falha ao tokenizar prompt");
        env->CallVoidMethod(jcb, onComplete);
        return;
    }
    tokens.resize(n);

    // 4. Processar prompt em lotes (batch simples, posição = 0..n-1)
    const int BATCH_SIZE = 512;
    for (int i = 0; i < n; i += BATCH_SIZE) {
        int cnt = std::min(BATCH_SIZE, n - i);
        // API: llama_batch_get_one não aloca memória, apenas aponta para o array
        struct llama_batch batch = llama_batch_get_one(tokens.data() + i, cnt);
        if (llama_decode(h->ctx, batch) != 0) {
            LOGE("Falha ao decodificar lote do prompt (i=%d, cnt=%d)", i, cnt);
            env->CallVoidMethod(jcb, onComplete);
            return;
        }
    }

    // 5. Geração token por token
    for (int i = 0; i < maxTokens; i++) {
        llama_token tok = llama_sampler_sample(h->sampler, h->ctx, -1);

        // Verifica Fim de Geração (End of Generation)
        if (llama_vocab_is_eog(vocab, tok)) {
            LOGI("Fim de geração (EOG) após %d tokens", i);
            break;
        }

        // Converter token em string
        char buf[256];
        int32_t len = llama_token_to_piece(vocab, tok, buf, sizeof(buf), 0, true);
        if (len > 0) {
            jstring js = env->NewStringUTF(std::string(buf, len).c_str());
            env->CallVoidMethod(jcb, onToken, js);
            env->DeleteLocalRef(js);
        }

        // Construir batch manual com posição correta (n + i)
        struct llama_batch next_batch = llama_batch_init(1, 0, 1);
        next_batch.token   [0] = tok;
        next_batch.pos     [0] = n + i;       // posição absoluta real
        next_batch.n_seq_id[0] = 1;
        
        // CORREÇÃO: llama_batch_init já aloca seq_id[0]. Não use malloc!
        next_batch.seq_id  [0][0] = 0;
        next_batch.logits  [0] = true;
        next_batch.n_tokens = 1;

        if (llama_decode(h->ctx, next_batch) != 0) {
            LOGE("Falha ao decodificar token gerado (i=%d)", i);
            // CORREÇÃO: llama_batch_free cuida de liberar todas as matrizes internas
            llama_batch_free(next_batch);
            break;
        }
        // CORREÇÃO: Sem free manual, apenas o free oficial da struct
        llama_batch_free(next_batch);
    }

    env->CallVoidMethod(jcb, onComplete);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gerfrota_lite_ai_LlamaCppEngine_nativeFree(
        JNIEnv*, jobject, jlong jh) {
    auto* h = reinterpret_cast<NativeHandle*>(jh);
    if (h) {
        if (h->sampler) llama_sampler_free(h->sampler);
        if (h->ctx)     llama_free(h->ctx);
        if (h->model)   llama_model_free(h->model);
        delete h;
    }
    llama_backend_free();
}
