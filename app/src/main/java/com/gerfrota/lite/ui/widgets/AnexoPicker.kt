package com.gerfrota.lite.ui.widgets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.gerfrota.lite.core.AnexoHelper
import com.gerfrota.lite.core.PathHelper
import android.content.Context
import java.io.File

/**
 * Retorna uma função que abre o seletor (Câmera / Galeria).
 * Ao salvar, chama [onSalvo] com o caminho final.
 */
@Composable
fun rememberAnexoPicker(
    pasta: String,
    nomeBase: String,
    onSalvo: (String) -> Unit
): () -> Unit {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var mostrar by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val ext = AnexoHelper.extensaoDeMime(ctx, uri)
            val dest = File(PathHelper.pasta(ctx, pasta), "$nomeBase.$ext")
            if (AnexoHelper.copiarUri(ctx, uri, dest)) onSalvo(dest.absolutePath)
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.exists()) {
            val dest = File(PathHelper.pasta(ctx, pasta), "$nomeBase.jpg")
            f.copyTo(dest, overwrite = true); f.delete()
            onSalvo(dest.absolutePath)
        }
    }

    if (mostrar) {
        AlertDialog(
            onDismissRequest = { mostrar = false },
            title = { Text("Anexar documento") },
            text = {
                androidx.compose.foundation.layout.Column {
                    TextButton(onClick = {
                        mostrar = false
                        val (f, u) = AnexoHelper.criarArquivoCamera(ctx)
                        cameraFile = f; camera.launch(u)
                    }) { Text("📷 Tirar foto com a câmera") }
                    TextButton(onClick = { mostrar = false; pick.launch("*/*") }) {
                        Text("📁 Selecionar arquivo do dispositivo")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrar = false }) { Text("Cancelar") } }
        )
    }
    return { mostrar = true }
}
