package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

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
