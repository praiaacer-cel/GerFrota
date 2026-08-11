package com.gerfrota.lite.ui.widgets

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.Modifier
import com.gerfrota.lite.data.DatabaseHelper
import java.util.Calendar

@Composable
fun CampoData(label: String, value: String, onSel: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    OutlinedTextField(value, {}, readOnly = true, label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = {
                val c = DatabaseHelper.parseDataBR(value) ?: Calendar.getInstance()
                DatePickerDialog(ctx, { _, y, m, d ->
                    onSel(String.format("%02d/%02d/%04d", d, m + 1, y))
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }) { Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp)) }
        },
        modifier = modifier)
}
