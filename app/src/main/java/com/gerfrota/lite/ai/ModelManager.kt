// ai/ModelManager.kt
package com.gerfrota.lite.ai

import android.content.Context
import com.gerfrota.lite.core.PathHelper
import java.io.File

object ModelManager {
    const val MODEL_NAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf"

    /** Procura o modelo em vários locais; se existir em assets, copia para storage interno. */
    fun resolve(context: Context): File? {
        val dirModelos = PathHelper.pastaModelosIA(context)
        val candidatos = listOf(                              // ✅ fechar o listOf
            File(dirModelos, MODEL_NAME),
            File(PathHelper.base(context), "models/$MODEL_NAME")
        )                                                     // ← este ")" estava faltando
        for (c in candidatos) if (c.exists() && c.length() > 10_000_000) return c
        return try {
            val dest = candidatos[0].also { it.parentFile?.mkdirs() }
            context.assets.open("models/$MODEL_NAME").use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            dest
        } catch (e: Exception) { null }   // sem modelo → modo simulado
    }
}
