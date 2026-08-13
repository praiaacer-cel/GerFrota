package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RelatorioCombustivelScreen(db: DatabaseHelper) {
    // ... (mantenha o resto do seu código original abaixo)    var lista by remember { mutableStateOf<List<RelatoriosDao.Consumo>>(emptyList()) }
    var sel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        lista = RelatoriosDao.consumoCombustivel(db); sel = lista.firstOrNull()?.placa
    } }
    val atual = lista.firstOrNull { it.placa == sel }
    Scaffold(topBar = { TopAppBar(title = { Text("Consumo de Combustível") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Veículo", sel, lista.map { it.placa }) { sel = it }
            Spacer(Modifier.height(30.dp))
            atual?.let { c ->
                Card(colors = CardDefaults.cardColors(containerColor = if (c.media > 2.5) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Média de Consumo", fontSize = 18.sp)
                        Text("${"%.2f".format(c.media)} KM/L", fontSize = 48.sp, fontWeight = FontWeight.Black,
                            color = if (c.media > 2.5) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardSecundario("Total KM", "${"%.1f".format(c.km)} km", Color(0xFF212121))
                    CardSecundario("Total Litros", "${"%.1f".format(c.litros)} L", Color(0xFF212121))
                }
            }
        }
    }
}
