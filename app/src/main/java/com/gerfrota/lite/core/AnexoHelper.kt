package com.gerfrota.lite.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object AnexoHelper {
    /** Copia um Uri (galeria/arquivo) para o destino. Retorna true se ok. */
    fun copiarUri(ctx: Context, uri: Uri, destino: File): Boolean = try {
        ctx.contentResolver.openInputStream(uri)?.use { inp ->
            destino.outputStream().use { out -> inp.copyTo(out) }
        } ?: return false
        true
    } catch (e: Exception) { false }

    /** Cria arquivo temporário p/ câmera e retorna (File, Uri do FileProvider). */
    fun criarArquivoCamera(ctx: Context): Pair<File, Uri> {
        val f = PathHelper.arquivoTemporario(ctx, "camera", "jpg")   // ✅ era File(ctx.cacheDir, ...)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        return f to uri
    }

    /** Abre PDF com app externo via FileProvider. */
    fun abrirPdf(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(intent, "Abrir PDF"))
    }

    fun extensaoDeMime(ctx: Context, uri: Uri): String {
        val mime = ctx.contentResolver.getType(uri) ?: return "jpg"
        return mime.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "jpg"
    }
}
