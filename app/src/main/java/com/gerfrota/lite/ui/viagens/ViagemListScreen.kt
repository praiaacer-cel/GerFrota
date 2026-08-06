package com.gerfrota.lite.ui.viagens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.services.PdfService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViagemListScreen(unidadeId: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var viagens by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var detalhes by remember { mutableStateOf<Map<String, Any?>?>(null) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        viagens = db.queryAll("viagens").filter { db.str(it["unidade_id"]) == unidadeId.toString() }
            .sortedBy { db.str(it["nro_viagem"]) }
    }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Relação de Viagens", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { IconButton(onClick = { nav.navigate("viagem_form/$unidadeId/-1") }) { Icon(Icons.Default.Add, null) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (viagens.isEmpty()) item { Text("Nenhuma viagem registrada.", color = Color.Gray) }
            items(viagens, key = { it["id"] as Long }) { v ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nro: ${v["nro_viagem"]}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                            Text("${v["data_carga"]} → ${v["data_descarga"]}", fontSize = 12.sp)
                        }
                        Text("${v["cidade_partida"]} → ${v["cidade_destino"]} | ${v["empresa"]}", fontSize = 13.sp)
                        Text("Carga: ${v["carga"]} | NF: ${db.str(v["nota_fiscal"]).ifBlank { "-" }}", fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bruto: ${DatabaseHelper.fmtBRL(db.num(v["valor_bruto"]))}", fontSize = 12.sp)
                            Text("Saldos: ${DatabaseHelper.fmtBRL(db.num(v["total_saldos"]))}", fontSize = 12.sp, color = Color(0xFFC62828))
                            Text("Líquido: ${DatabaseHelper.fmtBRL(db.num(v["valor_liquido"]))}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { detalhes = v }) { Icon(Icons.Default.Visibility, null, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val f = PdfService.gerarRelatorioViagem(ctx, v)
                                    PdfService.compartilhar(ctx, f)
                                }
                            }) { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = { nav.navigate("viagem_form/$unidadeId/${v["id"]}") }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) { db.delete("viagens", v["id"] as Long); carregar() }
                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }

    detalhes?.let { v ->
        AlertDialog(onDismissRequest = { detalhes = null },
            title = { Text("Viagem Nro ${v["nro_viagem"]}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Veículo: ${v["marca"]} ${v["modelo"]} - Placas: ${v["placas"]}")
                    Text("Motorista: ${v["motorista"]}")
                    Text("Rota: ${v["cidade_partida"]} → ${v["cidade_destino"]}")
                    Text("Valor Bruto: ${DatabaseHelper.fmtBRL(db.num(v["valor_bruto"]))}")
                    Text("Total Reembolsos: ${DatabaseHelper.fmtBRL(db.num(v["resumo_reembolsos"]))}")
                    Text("Total Despesas: ${DatabaseHelper.fmtBRL(db.num(v["resumo_despesas"]))}")
                    Text("Total Saldos: ${DatabaseHelper.fmtBRL(db.num(v["total_saldos"]))}", color = Color(0xFFC62828))
                    Text("Valor Líquido: ${DatabaseHelper.fmtBRL(db.num(v["valor_liquido"]))}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { TextButton(onClick = { detalhes = null }) { Text("FECHAR") } })
    }
}
