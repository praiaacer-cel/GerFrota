package com.gerfrota.lite.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import javax.net.ssl.HttpsURLConnection
import java.net.URL

object DriveUploader {
    /** Upload multipart p/ Drive v3. Retorna o ID do arquivo ou null. */
    suspend fun upload(zip: File, token: String): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "----gerfrota"
            val conn = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"; conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            val body = ByteArrayOutputStream()
            val w = OutputStreamWriter(body)
            w.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{\"name\":\"${zip.name}\"}\r\n")
            w.write("--$boundary\r\nContent-Type: application/zip\r\n\r\n"); w.flush()
            body.write(zip.readBytes()); body.write("\r\n--$boundary--".toByteArray())
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.inputStream.bufferedReader().readText()   // JSON com "id"
        } catch (e: Exception) { null }
    }
}
