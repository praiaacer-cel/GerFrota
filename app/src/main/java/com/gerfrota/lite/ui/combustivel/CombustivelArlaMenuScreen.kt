package com.gerfrota.lite.ui.combustivel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombustivelArlaMenuScreen(
    placa: String, marcaModelo: String,
    onCombustivel: () -> Unit, onArla: () -> Unit, onBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Controle de Abastecimento", fontSize = 14.sp)
                Text("$placa - $marcaModelo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp)) {
            Text("O que deseja registrar?", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            Card(onClick = onCombustivel, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(24.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalGasStation, null, tint = Color(0xFFE65100), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Combustível", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text("Registre abastecimentos de diesel e controle consumo.", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(onClick = onArla, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(24.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.OilBarrel, null, tint = Color(0xFF1565C0), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("ARLA 32", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text("Registre abastecimentos de ARLA e controle a nota.", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
