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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioRentabilidadeScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var veiculos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var sel by remember { mutableStateOf<String?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var rx by remember { mutableStateOf<RelatoriosDao.RaioX?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            veiculos = db.queryAll("frota")
            sel = veiculos.firstOrNull()?.let { db.str(it["placa"]) }
        }
    }
    LaunchedEffect(sel, mes, ano) {
        if (sel != null) withContext(Dispatchers.IO) { rx = RelatoriosDao.raioX(db, sel!!, mes, ano) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Raio-X do Caminhão") }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            DropdownSimples("Veículo", sel, veiculos.map { db.str(it["placa"]) }) { sel = it }
            Spacer(Modifier.height(8.dp))
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            rx?.let { r ->
                Spacer(Modifier.height(16.dp))
                CardDestaque("LUCRO LÍQUIDO", DatabaseHelper.fmtBRL(r.liquido),
                    if (r.liquido >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Icons.Default.TrendingUp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario("Faturamento Bruto", DatabaseHelper.fmtBRL(r.bruto), Color(0xFF1976D2), Modifier.weight(1f))
                    CardSecundario("Despesas Totais", DatabaseHelper.fmtBRL(r.despesas), Color(0xFFC62828), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text("Viagens no mês: ${r.qtd}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioDesempenhoPneusScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var ranking by remember { mutableStateOf<List<RelatoriosDao.RankPneu>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { ranking = RelatoriosDao.desempenhoPneus(db) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Batalha das Marcas (CPK)") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            Surface(color = Color(0xFF1976D2)) {
                Text("O ranking mostra qual marca tem o MENOR Custo Por KM (CPK).",
                    color = Color.White, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioCombustivelScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var lista by remember { mutableStateOf<List<RelatoriosDao.Consumo>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { lista = RelatoriosDao.consumoCombustivel(db) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Consumo de Combustível") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(lista) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(c.placa, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${"%.1f".format(c.km)} km | ${"%.1f".format(c.litros)} L") },
                        trailingContent = { Text("${"%.2f".format(c.media)} km/L", fontWeight = FontWeight.Black, color = if (c.media > 2.5) Color(0xFF2E7D32) else Color(0xFFC62828)) })
                }
            }
        }
    }
}

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
            val r = RelatoriosDao.custoTotalManutencoes(db)
            total = r.first
            list = r.second
            val hoje = java.util.Calendar.getInstance()
            alertas = db.queryAll("frota").flatMap { v ->
                val placa = db.str(v["placa"])
                listOf("vencimento_licenciamento" to "Licenciamento", "vencimento_antt" to "ANTT").mapNotNull { (col, nome) ->
                    val c = DatabaseHelper.parseDataBR(db.str(v[col])) ?: return@mapNotNull null
                    val dias = ((c.timeInMillis - hoje.timeInMillis) / 86400000).toInt()
                    when {
                        dias < 0 -> "🔴 $placa: $nome VENCIDO"
                        dias <= 30 -> "🟡 $placa: $nome vence em $dias dias"
                        else -> null
                    }
                }
            }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Manutenção e Vencimentos") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { CardDestaque("CUSTO TOTAL EM MANUTENÇÕES", DatabaseHelper.fmtBRL(total), Color(0xFFC62828), Icons.Default.Build) }
            item { Spacer(Modifier.height(10.dp)); Text("ALERTAS DE VENCIMENTO", fontWeight = FontWeight.Bold) }
            items(alertas) { a -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(a, Modifier.padding(12.dp), color = Color(0xFFB26A00), fontWeight = FontWeight.Bold) } }
            item { Spacer(Modifier.height(10.dp)); Text("SERVIÇOS REGISTRADOS", fontWeight = FontWeight.Bold) }
            items(list) { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${m["placa_veiculo"]} – ${m["sistema"]}", fontWeight = FontWeight.Bold)
                        Text("${m["data_servico"]} | ${m["tipo_servico"]} | ${DatabaseHelper.fmtBRL(db.num(m["valor_servico"]))}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioFluxoCaixaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioContasReceberScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var contas by remember { mutableStateOf<List<RelatoriosDao.ContaReceber>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { contas = RelatoriosDao.contasReceber(db) } }
    val total = contas.sumOf { it.valor }
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { CardDestaque("TOTAL A RECEBER", DatabaseHelper.fmtBRL(total), Color(0xFF2E7D32), Icons.Default.Payments) }
            items(contas) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(c.empresa.ifBlank { "Sem empresa" }, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Viagem ${c.nro} | ${c.dataCarga}") },
                        trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) })
                }
            }
        }
    }
}