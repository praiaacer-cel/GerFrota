package com.gerfrota.lite.ui.combustivel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.core.sanitized
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.proximoIdAbastecimento
import com.gerfrota.lite.data.ultimoAbastecimento
import com.gerfrota.lite.data.abastecimentosPorPlaca
import com.gerfrota.lite.services.ProntuarioService
import com.gerfrota.lite.ui.widgets.VisualizadorMidia
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArlaFormScreen(placa: String, veiculoId: Long, registroId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()

    var nro by remember { mutableStateOf("00001") }
    var data by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")).format(Date())) }
    var kmInicial by remember { mutableStateOf("") }; var kmAtual by remember { mutableStateOf("") }
    var kmRodado by remember { mutableStateOf("") }; var litros by remember { mutableStateOf("") }
    var valorTotal by remember { mutableStateOf("") }; var valorLitro by remember { mutableStateOf("") }
    var posto by remember { mutableStateOf("") }; var nf by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }; var pathNota by remember { mutableStateOf<String?>(null) }

    val marcaModelo = remember {
        db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
            ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
    }

    LaunchedEffect(Unit) {
        if (registroId >= 0) {
            db.queryAll("arla").firstOrNull { (it["id"] as? Long) == registroId }?.let { r ->
                nro = db.str(r["id_abastecimento"]); data = db.str(r["data_registro"])
                kmInicial = db.num(r["km_inicial"]).let { if (it>0) "%.1f".format(it) else "" }
                kmAtual = db.num(r["km_final"]).let { if (it>0) "%.1f".format(it) else "" }
                kmRodado = db.num(r["km_rodado"]).let { if (it>0) "%.1f".format(it) else "" }
                litros = db.num(r["litros"]).let { if (it>0) "%.1f".format(it) else "" }
                valorTotal = db.num(r["valor_total"]).let { if (it>0) "%.2f".format(it) else "" }
                valorLitro = db.num(r["valor_litro"]).let { if (it>0) "%.2f".format(it) else "" }
                posto = db.str(r["posto"]); nf = db.str(r["nota_fiscal"]); obs = db.str(r["observacoes"])
                pathNota = db.str(r["path_nota"]).ifBlank { null }
            }
        } else {
            nro = db.proximoIdAbastecimento("arla")
            db.ultimoAbastecimento("arla", placa)?.let { u ->
                val km = db.num(u["km_final"]); if (km > 0) kmInicial = "%.1f".format(km)
            }
        }
    }
    LaunchedEffect(kmInicial, kmAtual) {
        val i = kmInicial.replace(",",".").toDoubleOrNull() ?: 0.0
        val a = kmAtual.replace(",",".").toDoubleOrNull() ?: 0.0
        kmRodado = if (a > i) "%.1f".format(a - i) else "0"
    }
    LaunchedEffect(litros, valorTotal) {
        val l = litros.replace(",",".").toDoubleOrNull() ?: 0.0
        val v = valorTotal.replace(",",".").toDoubleOrNull() ?: 0.0
        if (l > 0 && v > 0) valorLitro = "%.2f".format(v / l)
    }
    val kmR = kmRodado.replace(",",".").toDoubleOrNull() ?: 0.0
    val lit = litros.replace(",",".").toDoubleOrNull() ?: 0.0
    val vTot = valorTotal.replace(",",".").toDoubleOrNull() ?: 0.0
    val consumo = if (lit > 0) kmR / lit else null
    val custoKm = if (kmR > 0) vTot / kmR else null

    val pickerNota = rememberAnexoPicker("Abastecimentoarla", "${nro}_${placa.sanitized()}") { pathNota = it }

    Scaffold(topBar = { TopAppBar(title = { Text(if (registroId>=0) "Editar ARLA" else "Novo ARLA 32") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(data, { data = it }, label = { Text("Data") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(kmInicial, { kmInicial = it }, label = { Text("KM Inicial") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(kmAtual, { kmAtual = it }, label = { Text("KM Atual") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(kmRodado, { kmRodado = it }, label = { Text("KM Rodado") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(litros, { litros = it }, label = { Text("Litros ARLA") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(valorTotal, { valorTotal = it }, label = { Text("Valor Total R$") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(valorLitro, { valorLitro = it }, label = { Text("Valor por Litro R$") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(posto, { posto = it }, label = { Text("Posto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nf, { nf = it }, label = { Text("Nº Nota Fiscal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(obs, { obs = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f)) { Text("Consumo Km/L", fontSize = 12.sp, color = Color.Gray)
                        Text(consumo?.let { "%.2f km/L".format(it) } ?: "—", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                    Column(Modifier.weight(1f)) { Text("Custo Km R$", fontSize = 12.sp, color = Color.Gray)
                        Text(custoKm?.let { DatabaseHelper.fmtBRL(it) } ?: "—", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) }
                }
            }
            Button(onClick = pickerNota, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (pathNota != null) Color(0xFF2E7D32) else Color(0xFF546E7A))) {
                Text(if (pathNota != null) "Nota Anexada ✓" else "Anexar Nota Fiscal")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val row = mapOf(
                        "id_abastecimento" to nro, "id_unidade" to veiculoId.toString(),
                        "placa_principal" to placa, "data_registro" to data,
                        "km_inicial" to (kmInicial.replace(",",".").toDoubleOrNull() ?: 0.0),
                        "km_final" to (kmAtual.replace(",",".").toDoubleOrNull() ?: 0.0),
                        "km_rodado" to kmR, "litros" to lit, "valor_total" to vTot,
                        "valor_litro" to (valorLitro.replace(",",".").toDoubleOrNull() ?: 0.0),
                        "posto" to posto, "nota_fiscal" to nf, "observacoes" to obs,
                        "consumo_km_l" to consumo, "custo_km" to custoKm, "path_nota" to pathNota)
                    if (registroId >= 0) db.update("arla", registroId, row) else db.insert("arla", row)
                    ProntuarioService.exportarArla(ctx, db, placa, marcaModelo)
                    onBack()
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                Text("SALVAR ARLA 32", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArlaHistoricoScreen(
    placa: String, marcaModelo: String,
    onNovo: () -> Unit, onEditar: (Long) -> Unit, onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var registros by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    fun carregar() = scope.launch(Dispatchers.IO) { registros = db.abastecimentosPorPlaca("arla", placa) }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("Histórico de ARLA 32", fontSize = 14.sp)
            Text(placa, fontWeight = FontWeight.Bold) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        floatingActionButton = { FloatingActionButton(onClick = onNovo) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (registros.isEmpty()) item { Text("Nenhum abastecimento de ARLA registrado.", color = Color.Gray) }
            items(registros, key = { (it["id"] as? Long) ?: 0L }) { r ->
                val id = (r["id"] as? Long) ?: 0L
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Data: ${r["data_registro"]}", fontWeight = FontWeight.Bold)
                            Text("ID: ${r["id_abastecimento"]}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Litros: ${"%.2f".format(db.num(r["litros"]))} L")
                            Text("Total: ${DatabaseHelper.fmtBRL(db.num(r["valor_total"]))}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Text("KM: ${"%.1f".format(db.num(r["km_rodado"]))} | Consumo: ${r["consumo_km_l"]?.let { "%.2f".format(db.num(it)) + " Km/L" } ?: "-"}", fontSize = 12.sp)
                        if (!db.str(r["path_nota"]).isBlank()) { Spacer(Modifier.height(8.dp)); VisualizadorMidia(db.str(r["path_nota"])) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { onEditar(id) }) { Icon(Icons.Default.Edit, null, tint = Color(0xFF1565C0)) }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.str(r["path_nota"]).takeIf { it.isNotBlank() }?.let { File(it).delete() }
                                    db.delete("arla", id)
                                    ProntuarioService.exportarArla(ctx, db, placa, marcaModelo)
                                    carregar()
                                }
                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828)) }
                        }
                    }
                }
            }
        }
    }
}
