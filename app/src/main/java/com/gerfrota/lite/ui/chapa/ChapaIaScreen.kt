package com.gerfrota.lite.ui.chapa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.ai.ChapaIAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapaIaScreen(vm: ChapaIAViewModel, onBack: () -> Unit) {
    var texto by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CHAPA IA", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Assistente offline da frota. Escreva sua pergunta e toque em enviar.")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Pergunta") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { vm.send(texto); texto = "" }) {
                    Icon(Icons.Default.Send, null)
                }
            }
        }
    }
}