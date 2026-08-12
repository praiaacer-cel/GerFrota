// ui/ViagensScreens.kt
package com.gerfrota.lite.ui.viagens // (ou com.gerfrota.lite.ui, dependendo de onde você salvou o arquivo)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // ✅ Correção 1: Importação da unidade 'sp'
import com.gerfrota.lite.data.DatabaseHelper
// ✅ Correção 2: Importação das cores a partir do pacote raiz 'ui' (onde elas foram declaradas no Theme.kt)
import com.gerfrota.lite.ui.AzulCard
import com.gerfrota.lite.ui.VermelhoAlerta
import com.gerfrota.lite.ui.VerdeSucesso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ViagensScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var rows by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    
    LaunchedEffect(Unit) { 
        withContext(Dispatchers.IO) {
            rows = db.queryAll("viagens", "id DESC")
        } 
    }
    
    Scaffold(topBar = { 
        TopAppBar(
            title = { Text("Viagens e Fretes") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        ) 
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(10.dp)) {
            items(rows) { v ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row { 
                            Text("Nro ${v["nro_viagem"]}", fontWeight = FontWeight.Bold, color = AzulCard)
                            Spacer(Modifier.weight(1f))
                            Text("${v["data_carga"] ?: ""} → ${v["data_descarga"] ?: ""}", fontSize = 12.sp) 
                        }
                        Text("${v["cidade_partida"] ?: "-"} → ${v["cidade_destino"] ?: "-"} • ${v["empresa"] ?: "-"}", fontSize = 13.sp)
                        Text("Carga: ${v["carga"] ?: "-"} | NF: ${v["nota_fiscal"] ?: "-"}", fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bruto: ${DatabaseHelper.fmtBRL(db.num(v["valor_bruto"]))}", fontSize = 12.sp)
                            Text("Saldos: ${DatabaseHelper.fmtBRL(db.num(v["total_saldos"]))}", fontSize = 12.sp, color = VermelhoAlerta)
                            Text("Líquido: ${DatabaseHelper.fmtBRL(db.num(v["valor_liquido"]))}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = VerdeSucesso)
                        }
                    }
                }
            }
        }
    }
}
