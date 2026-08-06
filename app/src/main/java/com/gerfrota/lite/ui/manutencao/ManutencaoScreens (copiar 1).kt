// ui/ManutencaoScreens.kt
package com.gerfrota.lite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var rows by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var mostrarForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun carregar() = scope.launch(Dispatchers.IO) { rows = db.queryAll("manutencoes", "id DESC") }
    LaunchedEffect(Unit) { carregar() }

    val total = rows.sumOf { db.num(it["valor_servico"]) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manutenção dos Veículos") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { IconButton(onClick = { mostrarForm = true }) { Icon(Icons.Default.Add, null) } }) }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Card(Modifier.fillMaxWidth().padding(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(Modifier.padding(14.dp)) {
                    Text("Custo Total em Manutenções", fontSize = 13.sp)
                    Text(DatabaseHelper.fmtBRL(total), fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, color = VermelhoAlerta)
                }
            }
            LazyColumn(Modifier.padding(horizontal = 10.dp)) {
                items(rows) { m ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row { Text("${m["placa_veiculo"]} – ${m["data_servico"]}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(DatabaseHelper.fmtBRL(db.num(m["valor_servico"])), color = VermelhoAlerta, fontWeight = FontWeight.Bold) }
                            Text("${m["sistema"]}/${m["subsistema"]} – ${m["tipo_servico"]}", fontSize = 13.sp)
                            Text("Prestador: ${m["prestador"] ?: "-"} • NF: ${m["numero_nota"] ?: "-"}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (mostrarForm) {
        var placa by remember { mutableStateOf("") }; var servico by remember { mutableStateOf("") }
        var valor by remember { mutableStateOf("") }; var sistema by remember { mutableStateOf("Outros") }
        AlertDialog(onDismissRequest = { mostrarForm = false },
            title = { Text("Nova Manutenção") },
            text = { Column {
                OutlinedTextField(placa, { placa = it.uppercase() }, label = { Text("Placa") })
                OutlinedTextField(servico, { servico = it }, label = { Text("Serviço") })
                OutlinedTextField(valor, { valor = it }, label = { Text("Valor R$") })
            } },
            confirmButton = { TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    db.insert("manutencoes", mapOf(
                        "placa_veiculo" to placa,
                        "data_servico" to SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")).format(Date()),
                        "quilometragem" to 0, "sistema" to sistema, "subsistema" to "OUTROS",
                        "tipo_servico" to servico, "valor_servico" to DatabaseHelper.fmtBRL(DatabaseHelper.parseMoney(valor))))
                    carregar(); mostrarForm = false
                }
            }) { Text("SALVAR") } },
            dismissButton = { TextButton(onClick = { mostrarForm = false }) { Text("CANCELAR") } })
    }
}
