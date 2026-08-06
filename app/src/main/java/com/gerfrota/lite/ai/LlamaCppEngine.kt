// ai/LlamaCppEngine.kt
package com.gerfrota.lite.ai

class LlamaCppEngine {
    interface GenerateCallback {
        fun onToken(token: String)
        fun onComplete()
    }

    private var handle: Long = 0
    val isLoaded get() = handle != 0L

    fun init(modelPath: String, nCtx: Int = 2048, nThreads: Int = 4): Boolean {
        if (isLoaded) return true
        handle = nativeInit(modelPath, nCtx, nThreads)
        return isLoaded
    }

    fun generate(prompt: String, maxTokens: Int = 512, cb: GenerateCallback) {
        if (isLoaded) nativeGenerate(handle, prompt, maxTokens, cb)
        else cb.onComplete()
    }

    fun release() { if (isLoaded) nativeFree(handle); handle = 0 }

    private external fun nativeInit(path: String, nCtx: Int, nThreads: Int): Long
    private external fun nativeGenerate(h: Long, prompt: String, maxTokens: Int, cb: GenerateCallback)
    private external fun nativeFree(h: Long)

    companion object { init { System.loadLibrary("gerfrota_llama") } }
}
