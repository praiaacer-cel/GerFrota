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
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioRentabilidadeScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var veiculos by remember {
        mutableStateOf<List<Map<String, Any?>>>(emptyList())
    }
    var sel by remember { mutableStateOf<String?>(null) }
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
    var rx by remember { mutableStateOf<RelatoriosDao.RaioX?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            veiculos = db.queryAll("frota")
            sel = veiculos.firstOrNull()?.let { db.str(it["placa"]) }
        }
    }
    LaunchedEffect(sel, mes, ano) {
        if (sel != null) {
            withContext(Dispatchers.IO) {
                rx = RelatoriosDao.raioX(db, sel!!, mes, ano)
            }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Raio-X do Caminhão") })
    }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            DropdownSimples(
                "Veículo", sel,
                veiculos.map { db.str(it["placa"]) }
            ) { sel = it }
            Spacer(Modifier.height(8.dp))
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            rx?.let { r ->
                Spacer(Modifier.height(16.dp))
                CardDestaque(
                    "LUCRO LÍQUIDO",
                    DatabaseHelper.fmtBRL(r.liquido),
                    if (r.liquido >= 0) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    },
                    Icons.Default.AttachMoney
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario(
                        "Faturamento",
                        DatabaseHelper.fmtBRL(r.bruto),
                        Color(0xFF1976D2),
                        Modifier.weight(1f)
                    )
                    CardSecundario(
                        "Despesas",
                        DatabaseHelper.fmtBRL(r.despesas),
                        Color(0xFFC62828),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioAcertoMotoristaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var motoristas by remember {
        mutableStateOf<List<Map<String, Any?>>>(emptyList())
    }
    var sel by remember { mutableStateOf<Long?>(null) }
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
    var ac by remember { mutableStateOf<RelatoriosDao.Acerto?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            motoristas = db.queryAll("motoristas", "nome ASC")
            sel = (motoristas.firstOrNull()?.get("id") as? Long)
        }
    }
    LaunchedEffect(sel, mes, ano) {
        if (sel != null) {
            withContext(Dispatchers.IO) {
                ac = RelatoriosDao.acertoMotorista(db, sel!!, mes, ano)
            }
        }
    }
    val nomes = motoristas.map { db.str(it["nome"]) }
    val nomeSel = motoristas
        .firstOrNull { (it["id"] as? Long) == sel }
        ?.let { db.str(it["nome"]) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Acerto de Motorista") })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Motorista", nomeSel, nomes) { n: String ->
                sel = motoristas
                    .firstOrNull { db.str(it["nome"]) == n }
                    ?.get("id") as? Long
            }
            Spacer(Modifier.height(8.dp))
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            ac?.let { a ->
                Spacer(Modifier.height(16.dp))
                CardDestaque(
                    "SALDO A RECEBER",
                    DatabaseHelper.fmtBRL(a.saldo),
                    if (a.saldo >= 0) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    },
                    Icons.Default.AttachMoney
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario(
                        "Comissões",
                        DatabaseHelper.fmtBRL(a.comissoes),
                        Color(0xFF1976D2),
                        Modifier.weight(1f)
                    )
                    CardSecundario(
                        "Vales / Adiant.",
                        DatabaseHelper.fmtBRL(a.adiantamentos),
                        Color(0xFFC62828),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

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
            items(ranking) { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            r.marca.uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "CPK: " + DatabaseHelper.fmtBRL(r.cpk),
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
                            "Média: " +
                                "%.2f".format(c.media) + " km/L"
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
        withContext(Dispatchers.IO) {
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
    Scaffold(topBar = {
        TopAppBar(title = { Text("Contas a Receber") })
    }) { pad ->
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
