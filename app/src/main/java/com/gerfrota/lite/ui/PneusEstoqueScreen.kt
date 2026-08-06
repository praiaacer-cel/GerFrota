package com.gerfrota.lite.ui.pneus

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.core.VeiculoConstants
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.valorAcumuladoPneu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PneusEstoqueScreen(placa: String, tipo: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var estoque by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var expandidos by remember { mutableStateOf(setOf<String>()) }
    var prontuario by remember { mutableStateOf<Map<String, Any?>?>(null) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        estoque = db.queryAll("pneus").filter {
            val vid = db.str(it["veiculo_id"])
            (vid.isBlank() || db.str(it["posicao_atual"]).equals("Estoque", true)) &&
            !db.str(it["status"]).equals("Descartado", true)
        }
    }
    LaunchedEffect(Unit) { carregar() }

    if (prontuario != null) {
        ProntuarioPneu(prontuario!!, placa, tipo, db,
            onVoltar = { prontuario = null; carregar() })
        return
    }

    val grupos = estoque.groupBy { "${db.str(it["modelo"])} | Medida: ${db.str(it["medida"])}" }

    Scaffold(topBar = {
        TopAppBar(title = { Text("ESTOQUE DE PNEUS", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (grupos.isEmpty()) item { Text("Nenhum pneu em estoque.", color = Color.Gray) }
            grupos.forEach { (chave, lista) ->
                item {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column {
                            Row(Modifier.fillMaxWidth().clickable {
                                expandidos = if (expandidos.contains(chave)) expandidos - chave else expandidos + chave
                            }.padding(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(chave, fontWeight = FontWeight.Bold)
                                    Text("Quantidade: ${lista.size} unidade(s)", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(if (expandidos.contains(chave)) "▲" else "▼")
                            }
                            if (expandidos.contains(chave)) {
                                lista.forEach { pneu ->
                                    ListItem(
                                        headlineContent = { Text("Código: ${db.str(pneu["codigo_fogo"])} | Marca: ${db.str(pneu["marca"])}") },
                                        supportingContent = { Text("Status: ${db.str(pneu["status"])} | Valor: ${DatabaseHelper.fmtBRL(db.num(pneu["valor_compra"]))} | Local: ${db.str(pneu["observacao"]).ifBlank { "Não definido" }}") },
                                        modifier = Modifier.clickable { prontuario = pneu })
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProntuarioPneu(
    pneu: Map<String, Any?>, placa: String, tipo: String,
    db: DatabaseHelper, onVoltar: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val codigo = db.str(pneu["codigo_fogo"])
    var historico by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var total by remember { mutableStateOf(0.0) }
    var dialogInstalar by remember { mutableStateOf(false) }
    var posSel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            total = db.valorAcumuladoPneu(codigo)
            historico = db.queryAll("manutencoes").filter { m ->
                db.str(m["sistema"]).equals("Pneus", true) &&
                    (db.str(m["subsistema"]).contains(codigo, true) ||
                     db.str(m["tipo_servico"]).contains(codigo, true) ||
                     db.str(m["observacao"]).contains(codigo, true))
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Prontuário: $codigo", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Default.ArrowBack, null) } },
            actions = {
                IconButton(onClick = { dialogInstalar = true }) { Icon(Icons.Default.Share, null) }
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        (pneu["id"] as? Long)?.let { db.delete("pneus", it) }
                        onVoltar()
                    }
                }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828)) }
            })
    }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Código: $codigo", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${db.str(pneu["marca"])} | ${db.str(pneu["modelo"])} | ${db.str(pneu["medida"])}")
                    Text("Status: ${db.str(pneu["status"])}")
                    Text("Custo Total Acumulado: ${DatabaseHelper.fmtBRL(total)}",
                        fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(historico) { h ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${db.str(h["data_servico"])} - ${db.str(h["tipo_servico"])}", fontWeight = FontWeight.Bold)
                            Text("Veículo: ${db.str(h["placa_veiculo"])} | KM: ${db.num(h["quilometragem"]).toInt()}")
                            Text("Prestador: ${db.str(h["prestador"])} | NF: ${db.str(h["numero_nota"])}")
                            Text("Valor: ${DatabaseHelper.fmtBRL(db.num(h["valor_servico"]))}",
                                color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (dialogInstalar) {
        val posicoes = VeiculoConstants.posicoesPneus(tipo)
        AlertDialog(onDismissRequest = { dialogInstalar = false },
            title = { Text("Instalar pneu em:") },
            text = {
                LazyColumn {
                    items(posicoes) { p ->
                        Row(Modifier.fillMaxWidth().clickable { posSel = p }.padding(12.dp)) {
                            RadioButton(selected = posSel == p, onClick = { posSel = p })
                            Spacer(Modifier.width(8.dp))
                            Text(p)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = posSel != null, onClick = {
                    scope.launch(Dispatchers.IO) {
                        db.update("pneus", pneu["id"] as Long, mapOf(
                            "status" to "Em uso", "posicao_atual" to posSel,
                            "veiculo_id" to placa, "observacao" to "Montado na posição $posSel"))
                        dialogInstalar = false
                        Toast.makeText(ctx, "Pneu instalado em $posSel", Toast.LENGTH_SHORT).show()
                        onVoltar()
                    }
                }) { Text("Instalar") }
            },
            dismissButton = { TextButton(onClick = { dialogInstalar = false }) { Text("Cancelar") } })
    }
}
