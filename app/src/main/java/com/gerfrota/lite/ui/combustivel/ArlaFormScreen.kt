package com.gerfrota.lite.ui.combustivel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
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
import com.gerfrota.lite.services.ProntuarioService
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArlaFormScreen(placa: String, veiculoId: Long, registroId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()

    var nro by remember { mutableStateOf("00001") }
    var data by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")).format(Date())) }
    var kmInicial by remember { mutableStateOf("") }
    var kmAtual by remember { mutableStateOf("") }
    var kmRodado by remember { mutableStateOf("") }
    var litros by remember { mutableStateOf("") }
    var valorTotal by remember { mutableStateOf("") }
    var valorLitro by remember { mutableStateOf("") }
    var posto by remember { mutableStateOf("") }
    var nf by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var pathNota by remember { mutableStateOf<String?>(null) }

    val marcaModelo = remember {
        db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
            ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
    }

    LaunchedEffect(Unit) {
        if (registroId >= 0) {
            db.queryAll("arla").firstOrNull { (it["id"] as? Long) == registroId }?.let { r ->
                nro = db.str(r["id_abastecimento"]); data = db.str(r["data_registro"])
                kmInicial = db.num(r["km_inicial"]).let { if (it > 0) "%.1f".format(it) else "" }
                kmAtual = db.num(r["km_final"]).let { if (it > 0) "%.1f".format(it) else "" }
                kmRodado = db.num(r["km_rodado"]).let { if (it > 0) "%.1f".format(it) else "" }
                litros = db.num(r["litros"]).let { if (it > 0) "%.1f".format(it) else "" }
                valorTotal = db.num(r["valor_total"]).let { if (it > 0) "%.2f".format(it) else "" }
                valorLitro = db.num(r["valor_litro"]).let { if (it > 0) "%.2f".format(it) else "" }
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
        val i = kmInicial.replace(",", ".").toDoubleOrNull() ?: 0.0
        val a = kmAtual.replace(",", ".").toDoubleOrNull() ?: 0.0
        kmRodado = if (a > i) "%.1f".format(a - i) else "0"
    }
    LaunchedEffect(litros, valorTotal) {
        val l = litros.replace(",", ".").toDoubleOrNull() ?: 0.0
        val v = valorTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (l > 0 && v > 0) valorLitro = "%.2f".format(v / l)
    }
    val kmR = kmRodado.replace(",", ".").toDoubleOrNull() ?: 0.0
    val lit = litros.replace(",", ".").toDoubleOrNull() ?: 0.0
    val vTot = valorTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
    val consumo = if (lit > 0) kmR / lit else null
    val custoKm = if (kmR > 0) vTot / kmR else null

    val pickerNota = rememberAnexoPicker("Abastecimentoarla", "${nro}_${placa.sanitized()}") { pathNota = it }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mapOf(
            "id_abastecimento" to nro, "id_unidade" to veiculoId.toString(),
            "placa_principal" to placa, "data_registro" to data,
            "km_inicial" to (kmInicial.replace(",",".").toDoubleOrNull() ?: 0.0),
            "km_final" to (kmAtual.replace(",",".").toDoubleOrNull() ?: 0.0),
            "km_rodado" to kmR, "litros" to lit, "valor_total" to vTot,
            "valor_litro" to (valorLitro.replace(",",".").toDoubleOrNull() ?: 0.0),
            "posto" to posto, "nota_fiscal" to nf, "observacoes" to obs,
            "consumo_km_l" to consumo, "custo_km" to custoKm, "path_nota" to pathNota)
        if (registroId >= 0) db.update("arla", registroId, row)
        else db.insert("arla", row)
        ProntuarioService.exportarArla(ctx, db, placa, marcaModelo)
        onBack()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (registroId >= 0) "Editar Abastecimento ARLA" else "Novo Abastecimento ARLA") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Placa: $placa", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("ID: $nro", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(data, { data = it }, label = { Text("Data do Abastecimento") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(kmInicial, { kmInicial = it }, label = { Text("KM Inicial") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(kmAtual, { kmAtual = it }, label = { Text("KM Atual") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(kmRodado, { kmRodado = it }, label = { Text("KM Rodado") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(litros, { litros = it }, label = { Text("Litros ARLA") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(valorTotal, { valorTotal = it }, label = { Text("Valor Total R$") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(valorLitro, { valorLitro = it }, label = { Text("Valor por Litro R$") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(posto, { posto = it }, label = { Text("Posto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nf, { nf = it }, label = { Text("Nº Nota Fiscal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(obs, { obs = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            Spacer(Modifier.height(16.dp))
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Consumo Km/L", fontSize = 12.sp, color = Color.Gray)
                        Text(consumo?.let { "%.2f km/L".format(it) } ?: "—",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Custo Km R$", fontSize = 12.sp, color = Color.Gray)
                        Text(custoKm?.let { DatabaseHelper.fmtBRL(it) } ?: "—",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = pickerNota, modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (pathNota != null) Color(0xFF2E7D32) else Color(0xFF546E7A))) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (pathNota != null) "Nota Fiscal Anexada ✓" else "Anexar Nota Fiscal", color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                Text("SALVAR ARLA 32", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
