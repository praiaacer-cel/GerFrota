package com.gerfrota.lite.ai

import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrService {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun lerImagem(imagePath: String): String {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return ""
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = recognizer.process(image)
            val result = Tasks.await(task) // Aguarda o ML Kit processar a imagem
            result.text
        } catch (e: Exception) {
            ""
        }
    }
}
