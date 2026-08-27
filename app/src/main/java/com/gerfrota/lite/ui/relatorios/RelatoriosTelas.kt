package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioDesempenhoPneusScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var ranking by remember {
        mutableStateOf<List<RelatoriosDao.RankPneu>>(emptyList())
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ranking = RelatoriosDao.desempenhoPneus(db)
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Batalha das Marcas (CPK)") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(ranking.size) { i ->
                val r = ranking[i]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = r.marca.uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CPK: " + DatabaseHelper.fmtBRL(r.cpk),
                            color = Color(0xFF1976D2)
                        )
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
    var lista by remember {
        mutableStateOf<List<RelatoriosDao.Consumo>>(emptyList())
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            lista = RelatoriosDao.consumoCombustivel(db)
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Consumo de Combustível") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(lista) { c ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.placa, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Média: " + "%.2f".format(c.media) + " km/L"
                        )
                    }
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
    var list by remember {
        mutableStateOf<List<Map<String, Any?>>>(emptyList())
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val r = RelatoriosDao.custoTotalManutencoes(db)
            total = r.first
            list = r.second
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Manutenção") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item {
                CardDestaque(
                    "CUSTO TOTAL",
                    DatabaseHelper.fmtBRL(total),
                    Color(0xFFC62828),
                    Icons.Default.Build
                )
            }
            items(list) { m ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            db.str(m["placa_veiculo"]) + " - " +
                                db.str(m["sistema"]),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            db.str(m["data_servico"]) + " | " +
                                db.str(m["tipo_servico"])
                        )
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
    var mes by remember {
        mutableStateOf(
            java.util.Calendar.getInstance()
                .get(java.util.Calendar.MONTH) + 1
        )
    }
    var ano by remember {
        mutableStateOf(
            java.util.Calendar.getInstance()
                .get(java.util.Calendar.YEAR)
        )
    }
    var f by remember { mutableStateOf<RelatoriosDao.Fluxo?>(null) }
    LaunchedEffect(mes, ano) {
        scope.launch(Dispatchers.IO) {
            f = RelatoriosDao.fluxoCaixa(db, mes, ano)
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Fluxo de Caixa") }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            f?.let { x ->
                Spacer(Modifier.height(14.dp))
                CardSecundario(
                    "Receitas",
                    DatabaseHelper.fmtBRL(x.receitas),
                    Color(0xFF2E7D32)
                )
                Spacer(Modifier.height(10.dp))
                CardSecundario(
                    "Despesas",
                    DatabaseHelper.fmtBRL(x.despesas),
                    Color(0xFFC62828)
                )
                Spacer(Modifier.height(14.dp))
                CardDestaque(
                    "SALDO DO PERÍODO",
                    DatabaseHelper.fmtBRL(x.saldo),
                    Color(0xFF1976D2),
                    Icons.Default.AttachMoney
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioContasReceberScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var contas by remember {
        mutableStateOf<List<RelatoriosDao.ContaReceber>>(emptyList())
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            contas = RelatoriosDao.contasReceber(db)
        }
    }
    val total = contas.sumOf { it.valor }
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item {
                CardDestaque(
                    "TOTAL A RECEBER",
                    DatabaseHelper.fmtBRL(total),
                    Color(0xFF2E7D32),
                    Icons.Default.Payments
                )
            }
            items(contas) { c ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.empresa, fontWeight = FontWeight.Bold)
                        Text("Viagem " + c.nro + " | " + c.dataCarga)
                        Text(
                            DatabaseHelper.fmtBRL(c.valor),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}