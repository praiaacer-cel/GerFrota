package com.gerfrota.lite.services

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.resume

class OcrService(private val context: Context) {

    /** Lê o texto de uma imagem (nota fiscal, recibo) usando ML Kit on-device. */
    suspend fun lerImagem(path: String): String = withContext(Dispatchers.IO) {
        try {
            val input = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
            suspendCancellableCoroutine { cont ->
                recognizer.process(input)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resume("") }
            }
        } catch (e: Exception) { "" }
    }
}
