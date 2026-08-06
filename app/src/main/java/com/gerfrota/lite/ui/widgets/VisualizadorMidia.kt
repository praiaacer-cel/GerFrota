package com.gerfrota.lite.ui.widgets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.core.AnexoHelper
import java.io.File

@Composable
fun VisualizadorMidia(path: String?, altura: Dp = 160.dp) {
    val ctx = LocalContext.current
    if (path.isNullOrBlank()) return
    val file = File(path)
    if (!file.exists()) {
        Text("Arquivo não encontrado.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        return
    }

    if (file.extension.equals("pdf", true)) {
        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().height(altura)) {
            Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Text(file.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { AnexoHelper.abrirPdf(ctx, file) }) { Text("Abrir") }
            }
        }
    } else {
        var dialogo by remember { mutableStateOf(false) }
        val bmp = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(altura)
                    .clip(RoundedCornerShape(8.dp)).clickable { dialogo = true },
                contentScale = ContentScale.Crop
            )
            if (dialogo) {
                AlertDialog(
                    onDismissRequest = { dialogo = false },
                    title = { Text(file.name) },
                    text = { ZoomableImage(bmp) },
                    confirmButton = { TextButton(onClick = { dialogo = false }) { Text("Fechar") } }
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(bmp: android.graphics.Bitmap) {
    var scale by remember { mutableStateOf(1f) }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth().height(380.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                }
            }
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentScale = ContentScale.Fit
    )
}
