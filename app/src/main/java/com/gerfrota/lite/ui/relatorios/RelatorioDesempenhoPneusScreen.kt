package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun RelatorioDesempenhoPneusScreen(db: DatabaseHelper) {
    var ranking by remember { mutableStateOf<List<RelatoriosDao.RankPneu>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ranking = RelatoriosDao.desempenhoPneus(db) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Batalha das Marcas (CPK)") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            Surface(color = Color(0xFF1976D2)) { Text("O ranking mostra qual marca tem o MENOR Custo Por KM (CPK). Quanto menor, mais dinheiro no seu bolso.",
                color = Color.White, modifier = Modifier.padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            LazyColumn(Modifier.padding(12.dp)) {
                items(ranking.size) { i ->
                    val r = ranking[i]
                    val corCard = when (i) { 0 -> Color(0xFFFFF8E1); 1 -> Color(0xFFECEFF1); 2 -> Color(0xFFFFF3E0); else -> Color.White }
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = corCard)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.marca.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Amostra: ${r.qtd} pneu(s)", fontSize = 13.sp, color = Color(0xFF757575))
                                Text("Rodado: ${"%.0f".format(r.km)} km | Custo: ${DatabaseHelper.fmtBRL(r.custo)}", fontSize = 12.sp, color = Color(0xFF757575))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CPK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(DatabaseHelper.fmtBRL(r.cpk), fontSize = 20.sp, fontWeight = FontWeight.Black,
                                    color = if (i == 0) Color(0xFF2E7D32) else Color(0xFF212121))
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun RelatorioCombustivelScreen() {
    val ctx = LocalContext.current; val db = remember { DatabaseHelper.get(ctx) }
    var lista by remember { mutableStateOf<List<RelatoriosDao.Consumo>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(Dispatchers.IO) { lista = RelatoriosDao.consumoPorVeiculo(db) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Consumo de Combustível") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(lista) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(c.placa, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${"%.1f".format(c.km)} km | ${"%.1f".format(c.litros)} L") },
                        trailingContent = { Text("${"%.2f".format(c.media)} km/L",
                            fontWeight = FontWeight.Black,
                            color = if (c.media > 2.5) Color(0xFF2E7D32) else Color(0xFFC62828)) })
                }
            }
        }
    }
}

@Composable
fun RelatorioManutencaoScreen() {
    val ctx = LocalContext.current; val db = remember { DatabaseHelper.get(ctx) }
    var total by remember { mutableStateOf(0.0) }
    var list by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var alertas by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(Dispatchers.IO) {
        val r = RelatoriosDao.totalManutencoes(db); total = r.first; list = r.second
        alertas = RelatoriosDao.alertasVencimento(db)
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Manutenção e Vencimentos") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { CardDestaque("CUSTO TOTAL EM MANUTENÇÕES", DatabaseHelper.fmtBRL(total), Color(0xFFC62828), Icons.Default.Build) }
            item { Spacer(Modifier.height(10.dp)); Text("ALERTAS DE VENCIMENTO", fontWeight = FontWeight.Bold) }
            items(alertas) { a -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(a, Modifier.padding(12.dp), color = Color(0xFFB26A00), fontWeight = FontWeight.Bold) } }
            item { Spacer(Modifier.height(10.dp)); Text("SERVIÇOS REGISTRADOS", fontWeight = FontWeight.Bold) }
            items(list) { m -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${m["placa_veiculo"]} – ${m["sistema"]}", fontWeight = FontWeight.Bold)
                    Text("${m["data_servico"]} | ${m["tipo_servico"]} | ${DatabaseHelper.fmtBRL(db.num(m["valor_servico"]))}")
                } } }
        }
    }
}

@Composable
fun RelatorioFluxoCaixaScreen() {
    val ctx = LocalContext.current; val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var f by remember { mutableStateOf<RelatoriosDao.Fluxo?>(null) }
    LaunchedEffect(mes, ano) { scope.launch(Dispatchers.IO) { f = RelatoriosDao.fluxoCaixa(db, mes, ano) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Fluxo de Caixa") }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            f?.let { x ->
                Spacer(Modifier.height(14.dp))
                CardSecundario("Receitas (Viagens)", DatabaseHelper.fmtBRL(x.receitas), Color(0xFF2E7D32))
                Spacer(Modifier.height(10.dp))
                CardSecundario("Despesas (Manut./Comb./Adiant.)", DatabaseHelper.fmtBRL(x.despesas), Color(0xFFC62828))
                Spacer(Modifier.height(14.dp))
                CardDestaque("SALDO DO PERÍODO", DatabaseHelper.fmtBRL(x.saldo),
                    if (x.saldo >= 0) Color(0xFF1976D2) else Color(0xFFB26A00), Icons.Default.AttachMoney)
            }
        }
    }
}

@Composable
fun RelatorioContasReceberScreen() {
    val ctx = LocalContext.current; val db = remember { DatabaseHelper.get(ctx) }
    var contas by remember { mutableStateOf<List<RelatoriosDao.ContaReceber>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(Dispatchers.IO) { contas = RelatoriosDao.contasReceber(db) } }
    val total = contas.sumOf { it.valor }
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { CardDestaque("TOTAL A RECEBER", DatabaseHelper.fmtBRL(total), Color(0xFF2E7D32), Icons.Default.Payments) }
            items(contas) { c -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                ListItem(headlineContent = { Text(c.empresa.ifBlank { "Sem empresa" }, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Viagem ${c.nro} | ${c.dataCarga}") },
                    trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }) } }
        }
    }
}
