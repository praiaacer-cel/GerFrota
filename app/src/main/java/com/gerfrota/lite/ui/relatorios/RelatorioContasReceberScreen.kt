package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RelatorioContasReceberScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var contas by remember { mutableStateOf<List<RelatoriosDao.ContaReceber>>(emptyList()) }
    
    LaunchedEffect(Unit) { 
        withContext(Dispatchers.IO) { contas = RelatoriosDao.contasReceber(db) } 
    }
    
    val total = contas.sumOf { it.valor }
    
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { 
                CardDestaque("TOTAL A RECEBER", DatabaseHelper.fmtBRL(total), Color(0xFF2E7D32), Icons.Default.Payments) 
            }
            items(contas) { c -> 
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(c.empresa.ifBlank { "Sem empresa" }, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Viagem: ${c.nro} | Data Carga: ${c.dataCarga}") },
                        trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                    )
                }
            }
        }
    }
}
