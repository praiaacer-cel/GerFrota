package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    Scaffold(topBar = { TopAppBar(title = { Text("Raio-X do Caminhão") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Selecione um veículo na tela anterior para ver o Raio-X.", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioAcertoMotoristaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { motoristas = db.queryAll("motoristas") } }
    Scaffold(topBar = { TopAppBar(title = { Text("Acerto de Motoristas") }) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(motoristas) { m ->
                ListItem(
                    headlineContent = { Text(db.str(m["nome"])) },
                    supportingContent = { Text("Comissões e vales do período") }
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
    val ranking = remember { RelatoriosDao.consumoCombustivel(db) }
    Scaffold(topBar = { TopAppBar(title = { Text("Desempenho de Combustível") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp)) {
            items(ranking) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.placa, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Média: ${"%.2f".format(c.media)} KM/L", color = Color(0xFF1976D2))
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
    val (total, list) = remember { RelatoriosDao.custoTotalManutencoes(db) }
    Scaffold(topBar = { TopAppBar(title = { Text("Manutenção e Vencimentos") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Custo Total: ${DatabaseHelper.fmtBRL(total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(list.take(20)) { m ->
                    ListItem(
                        headlineContent = { Text(db.str(m["tipo_servico"])) },
                        supportingContent = { Text("${db.str(m["placa_veiculo"])} - ${db.str(m["data_servico"])}") }
                    )
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
    val mes = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    val ano = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val fluxo = remember { RelatoriosDao.fluxoCaixa(db, mes, ano) }
    Scaffold(topBar = { TopAppBar(title = { Text("Fluxo de Caixa Mensal") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Receitas: ${DatabaseHelper.fmtBRL(fluxo.receitas)}", color = Color(0xFF2E7D32), fontSize = 18.sp)
            Text("Despesas: ${DatabaseHelper.fmtBRL(fluxo.despesas)}", color = Color(0xFFC62828), fontSize = 18.sp)
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Saldo: ${DatabaseHelper.fmtBRL(fluxo.saldo)}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioContasReceberScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val contas = remember { RelatoriosDao.contasReceber(db) }
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber") }) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(contas) { c ->
                ListItem(
                    headlineContent = { Text(c.empresa) },
                    supportingContent = { Text("Viagem ${c.nro} - ${c.dataCarga}") },
                    trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF1976D2)) }
                )
            }
        }
    }
}