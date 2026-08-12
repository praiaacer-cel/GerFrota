package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MESES = listOf("Janeiro","Fevereiro","Março","Abril","Maio","Junho",
    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro")

@Composable
private fun CardDestaque(titulo: String, valor: String, cor: Color, icone: ImageVector) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = .12f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Row { Icon(icone, null, tint = cor); Spacer(Modifier.width(8.dp))
                Text(titulo, fontWeight = FontWeight.Bold, color = cor) }
            Spacer(Modifier.height(10.dp))
            Text(valor, fontSize = 30.sp, fontWeight = FontWeight.Black, color = cor)
        }
    }
}

@Composable
fun CardSecundario(
    titulo: String, 
    valor: String, 
    cor: Color, 
    modifier: Modifier = Modifier.fillMaxWidth() // Padrão para quando não estiver em uma Row
) {
    Card(
        modifier = modifier, 
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorMesAno(mes: Int, ano: Int, onMes: (Int) -> Unit, onAno: (Int) -> Unit) {
    var expMes by remember { mutableStateOf(false) }
    var expAno by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(expanded = expMes, onExpandedChange = { expMes = it }, Modifier.weight(1f)) {
            OutlinedTextField(MESES[mes - 1], {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expMes) })
            ExposedDropdownMenu(expMes, { expMes = false }) {
                MESES.forEachIndexed { i, m -> DropdownMenuItem({ Text(m) }, { onMes(i + 1) }) }
            }
        }
        ExposedDropdownMenuBox(expanded = expAno, onExpandedChange = { expAno = it }, Modifier.weight(1f)) {
            OutlinedTextField(ano.toString(), {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expAno) })
            ExposedDropdownMenu(expAno, { expAno = false }) {
                (2023..2030).forEach { a -> DropdownMenuItem({ Text(a.toString()) }, { onAno(a) }) }
            }
        }
    }
}

@Composable
fun RelatoriosScreen(nav: NavController) {
    val itens = listOf(
        Triple("Raio-X do Caminhão", "Lucro, despesas e faturamento por placa", "rel_rent"),
        Triple("Acerto de Motoristas", "Comissões, vales e saldo a pagar", "rel_acerto"),
        Triple("Desempenho de Pneus", "Ranking de custo por KM (CPK)", "rel_pneus"),
        Triple("Desempenho de Combustível", "Consumo médio (KM/L) por veículo", "rel_comb"),
        Triple("Manutenção e Vencimentos", "Alertas de documentos e serviços", "rel_manut"),
        Triple("Fluxo de Caixa Mensal", "Receitas vs. custos e saldo", "rel_fluxo"),
        Triple("Contas a Receber", "Fretes pendentes e atrasos", "rel_receber"),
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Relatórios Gerenciais", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(14.dp)) {
            items(itens) { (t, s, rota) ->
                // ✅ CORREÇÃO: clickable movido para o Card (envolve todo o bloco)
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { nav.navigate(rota) }
                ) {
                    ListItem(
                        headlineContent = { Text(t, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(s) },
                        leadingContent = { Icon(Icons.Default.BarChart, null, tint = Color(0xFF1976D2)) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
