package com.gerfrota.lite.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun ChapaIaScreen(viewModel: ChapaIAViewModel, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Chapa IA") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Tela de Chapa IA (Em desenvolvimento)")
        }
    }
}