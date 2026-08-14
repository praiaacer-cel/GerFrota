package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RelatorioCombustivelScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var lista by remember { mutableStateOf<List<RelatoriosDao.Consumo>>(emptyList()) }
    
    LaunchedEffect(Unit) { 
        withContext(Dispatchers.IO) { 
            lista = RelatoriosDao.consumoPorVeiculo(db) 
        } 
    }
    
    Scaffold(topBar = { TopAppBar(title = { Text("Consumo de Combustível") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(lista) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(c.placa, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${"%.1f".format(c.km)} km | ${"%.1f".format(c.litros)} L") },
                        trailingContent = { 
                            Text(
                                "${"%.2f".format(c.media)} km/L",
                                fontWeight = FontWeight.Black,
                                color = if (c.media > 2.5) Color(0xFF2E7D32) else Color(0xFFC62828)
                            ) 
                        }
                    )
                }
            }
        }
    }
}
