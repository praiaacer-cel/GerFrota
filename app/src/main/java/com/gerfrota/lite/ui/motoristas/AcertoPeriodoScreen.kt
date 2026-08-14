package com.gerfrota.lite.ui.motoristas
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcertoPeriodoScreen(motoristaId: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var historico by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var dataIni by remember { mutableStateOf("") }; var dataFim by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            historico = db.queryAll("acertos_historico").filter { (it["motorista_id"] as? Long) == motoristaId }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Período do Acerto de Contas", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(dataIni, { dataIni = it }, label = { Text("Data Inicial") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(dataFim, { dataFim = it }, label = { Text("Data Final") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val ini = DatabaseHelper.parseDataBR(dataIni)?.timeInMillis ?: return@Button
                        val fim = DatabaseHelper.parseDataBR(dataFim)?.timeInMillis ?: return@Button
                        nav.navigate("acerto_contas/$motoristaId/$ini/$fim")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Search, null); Text("ABRIR ACERTO DE CONTAS", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("Acertos Realizados:", fontWeight = FontWeight.Bold)
            LazyColumn {
                items(historico, key = { it["id"] as Long }) { h ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Período: ${h["data_inicio"]} a ${h["data_fim"]}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Líquido: ${DatabaseHelper.fmtBRL(db.num(h["liquido"]))}", color = androidx.compose.ui.graphics.Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
