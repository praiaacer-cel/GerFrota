// ui/RelatoriosScreen.kt
package com.gerfrota.lite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var mes by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var saida by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun calcular(rel: String) = scope.launch(Dispatchers.IO) {
        saida = when (rel) {
            "FLUXO DE CAIXA" -> {
                val (r, d, s) = db.resumoFinanceiroMes(mes, 2026)
                "Receitas: ${DatabaseHelper.fmtBRL(r)}\nDespesas: ${DatabaseHelper.fmtBRL(d)}\nSALDO: ${DatabaseHelper.fmtBRL(s)}"
            }
            "CONTAS A RECEBER" -> {
                val cr = db.contasReceber()
                val tot = cr.sumOf { db.num(it["valor_bruto"]) - db.num(it["pago_adiantamento_val"]) }
                buildString {
                    append("Pendentes: ${cr.size} viagem(ns) — ${DatabaseHelper.fmtBRL(tot)}\n")
                    cr.take(10).forEach { append("• Nro ${it["nro_viagem"]} ${it["empresa"]}: " +
                        "${DatabaseHelper.fmtBRL(db.num(it["valor_bruto"]) - db.num(it["pago_adiantamento_val"]))}\n") }
                }
            }
            "ALERTAS DE VENCIMENTO" -> db.alertasVencimento().joinToString("\n") { "• $it" }
                .ifBlank { "Nenhum documento vencendo em 30 dias." }
            else -> "Relatório em construção."
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Relatórios Gerenciais") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Slider(value = mes.toFloat(), onValueChange = { mes = it.toInt() },
                valueRange = 1f..12f, steps = 10)
            Text("Mês de referência: $mes", fontWeight = FontWeight.Bold)
            listOf("FLUXO DE CAIXA", "CONTAS A RECEBER", "ALERTAS DE VENCIMENTO").forEach { r ->
                Button(onClick = { calcular(r) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(r)
                }
            }
            if (saida.isNotBlank()) Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(saida, Modifier.padding(14.dp))
            }
        }
    }
}

