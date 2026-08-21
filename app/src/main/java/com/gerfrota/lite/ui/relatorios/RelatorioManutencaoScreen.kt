package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioManutencaoScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var total by remember { mutableStateOf(0.0) }
    var list by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var alertas by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { 
        withContext(Dispatchers.IO) {
            val r = RelatoriosDao.totalManutencoes(db); total = r.first; list = r.second
            alertas = RelatoriosDao.alertasVencimento(db)
        } 
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Manutenção e Vencimentos") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { CardDestaque("CUSTO TOTAL EM MANUTENÇÕES", DatabaseHelper.fmtBRL(total), Color(0xFFC62828), Icons.Default.Build) }
            item { Spacer(Modifier.height(10.dp)); Text("ALERTAS DE VENCIMENTO", fontWeight = FontWeight.Bold) }
            items(alertas) { a -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(a, Modifier.padding(12.dp), color = Color(0xFFB26A00), fontWeight = FontWeight.Bold) } }
            item { Spacer(Modifier.height(10.dp)); Text("SERVIÇOS REGISTRADOS", fontWeight = FontWeight.Bold) }
            items(list) { m -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text("${m["placa_veiculo"]} – ${m["sistema"]}", fontWeight = FontWeight.Bold); Text("${m["data_servico"]} | ${m["tipo_servico"]} | ${DatabaseHelper.fmtBRL(db.num(m["valor_servico"]))}") } } }
        }
    }
}
