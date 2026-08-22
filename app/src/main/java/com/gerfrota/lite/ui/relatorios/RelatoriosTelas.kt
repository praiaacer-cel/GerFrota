package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioAcertoMotoristaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var sel by remember { mutableStateOf<Long?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var ac by remember { mutableStateOf<RelatoriosDao.Acerto?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            motoristas = db.queryAll("motoristas", "nome ASC")
            sel = (motoristas.firstOrNull()?.get("id") as? Long)
        }
    }
    LaunchedEffect(sel, mes, ano) {
        if (sel != null) withContext(Dispatchers.IO) { ac = RelatoriosDao.acertoMotorista(db, sel!!, mes, ano) }
    }
    val nomes = motoristas.map { db.str(it["nome"]) }
    val nomeSel = motoristas.firstOrNull { (it["id"] as? Long) == sel }?.let { db.str(it["nome"]) }

    Scaffold(topBar = { TopAppBar(title = { Text("Acerto de Motorista") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Motorista", nomeSel, nomes) { n: String ->
                sel = motoristas.firstOrNull { db.str(it["nome"]) == n }?.get("id") as? Long
            }
            Spacer(Modifier.height(8.dp))
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            ac?.let { a ->
                Spacer(Modifier.height(16.dp))
                CardDestaque(
                    "SALDO A RECEBER", DatabaseHelper.fmtBRL(a.saldo),
                    if (a.saldo >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    Icons.Default.AccountBalanceWallet
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario("Comissões (Produção)", DatabaseHelper.fmtBRL(a.comissoes), Color(0xFF1976D2), Modifier.weight(1f))
                    CardSecundario("Vales / Adiant. (Descontos)", DatabaseHelper.fmtBRL(a.adiantamentos), Color(0xFFC62828), Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    if (a.saldo >= 0) "A frota deve pagar ${DatabaseHelper.fmtBRL(a.saldo)} ao motorista."
                    else "Atenção! O motorista está devendo ${DatabaseHelper.fmtBRL(abs(a.saldo))} para a frota.",
                    fontSize = 14.sp
                )
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
                        trailingContent = { Text("${"%.2f".format(c.media)} km/L", fontWeight = FontWeight.Black, color = if (c.media > 2.5) Color(0xFF2E7D32) else Color(0xFFC62828)) }
                    )
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
            items(alertas) { a ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(a, Modifier.padding(12.dp), color = Color(0xFFB26A00), fontWeight = FontWeight.Bold) }
            }
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
                CardDestaque("SALDO DO PERÍODO", DatabaseHelper.fmtBRL(x.saldo), if (x.saldo >= 0) Color(0xFF1976D2) else Color(0xFFB26A00), Icons.Default.AttachMoney)
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
                        trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                    )
                }
            }
        }
    }
}