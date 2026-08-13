package com.gerfrota.lite.ui.relatorios // (ou o pacote correspondente)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color              // ✅ Adicionar
import androidx.compose.ui.text.font.FontWeight     // ✅ Adicionar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RelatorioManutencaoScreen(db: DatabaseHelper) {
    var total by remember { mutableStateOf(0.0) }
    var list by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val r = RelatoriosDao.custoTotalManutencoes(db); total = r.first; list = r.second
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Relatório de Manutenção") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Custo Total em Manutenções", fontSize = 14.sp)
                    Text(DatabaseHelper.fmtBRL(total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(list.size) { i ->
                    val m = list[i]
                    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        ListItem(
                            headlineContent = { Text("Veículo: ${db.str(m["placa_veiculo"])} - ${db.str(m["sistema"])}", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Serviço: ${db.str(m["tipo_servico"])}\nData: ${db.str(m["data_servico"])}\nPrestador: ${db.str(m["prestador"])}") },
                            trailingContent = { Text(DatabaseHelper.fmtBRL(DatabaseHelper.parseMoney(db.str(m["valor_servico"]))),
                                fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) })
                    }
                }
            }
        }
    }
}
