package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun RelatorioDesempenhoPneusScreen() {
    // ✅ db instanciado internamente (padrão usado pelo Nav.kt, que chama sem parâmetros)
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var ranking by remember { mutableStateOf<List<RelatoriosDao.RankPneu>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ranking = RelatoriosDao.desempenhoPneus(db)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Batalha das Marcas (CPK)") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            Surface(color = Color(0xFF1976D2)) {
                Text(
                    "O ranking mostra qual marca tem o MENOR Custo Por KM (CPK). Quanto menor, mais dinheiro no seu bolso.",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            LazyColumn(Modifier.padding(12.dp)) {
                items(ranking.size) { i ->
                    val r = ranking[i]
                    val corCard = when (i) {
                        0 -> Color(0xFFFFF8E1)
                        1 -> Color(0xFFECEFF1)
                        2 -> Color(0xFFFFF3E0)
                        else -> Color.White
                    }
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = corCard)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.marca.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Amostra: ${r.qtd} pneu(s)", fontSize = 13.sp, color = Color(0xFF757575))
                                Text(
                                    "Rodado: ${"%.0f".format(r.km)} km | Custo: ${DatabaseHelper.fmtBRL(r.custo)}",
                                    fontSize = 12.sp, color = Color(0xFF757575)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CPK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    DatabaseHelper.fmtBRL(r.cpk),
                                    fontSize = 20.sp, fontWeight = FontWeight.Black,
                                    color = if (i == 0) Color(0xFF2E7D32) else Color(0xFF212121)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
