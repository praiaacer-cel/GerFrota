package com.gerfrota.lite.ui.combustivel

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
import com.gerfrota.lite.core.sanitized
import com.gerfrota.lite.data.*
import com.gerfrota.lite.services.ProntuarioService
import com.gerfrota.lite.ui.widgets.VisualizadorMidia
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class TipoAbastecimento { COMBUSTIVEL, ARLA }

private val TIPOS_PERMITIDOS = setOf(
    "Cavalo Mecânico Toco", "Cavalo Mecânico Trucado",
    "Caminhão Toco", "Caminhão Truck", "Caminhão BiTruck")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeiculosSelecaoScreen(onSelecionar: (String, Long) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var termo by remember { mutableStateOf("") }
    var veiculos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    LaunchedEffect(Unit) {
        veiculos = db.queryAll("frota", "placa ASC").filter { db.str(it["tipo_veiculo"]) in TIPOS_PERMITIDOS }
    }
    val filtrados = veiculos.filter { termo.isBlank() ||
        listOf("placa","marca","modelo","tipo_veiculo").any { db.str(it).contains(termo, true) } }

    Scaffold(topBar = { TopAppBar(title = { Text("Selecionar Veículo") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(termo, { termo = it }, leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar por placa, marca ou modelo") },
                modifier = Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(filtrados) { v ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        onClick = { onSelecionar(db.str(v["placa"]), (v["id"] as? Long) ?: 0L) }) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = Color(0xFFE65100), modifier = Modifier.size(36.dp)) },
                            headlineContent = { Text(db.str(v["placa"]), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                            supportingContent = { Text("${db.str(v["marca"])} ${db.str(v["modelo"])} | ${db.str(v["tipo_veiculo"])}") },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) })
                    }
                }
            }
        }
    }
}

@Composable
fun CombustivelArlaMenuScreen(placa: String, marcaModelo: String,
    onCombustivel: () -> Unit, onArla: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Column { Text("Controle de Abastecimento", fontSize = 14.sp)
        Text("$placa - $marcaModelo", fontWeight = FontWeight.Bold) } },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp)) {
            Text("O que deseja registrar?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Card(onClick = onCombustivel, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(24.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalGasStation, null, tint = Color(0xFFE65100), modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(20.dp))
                    Column { Text("Combustível", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text("Registre abastecimentos de diesel e controle consumo.", fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(onClick = onArla, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(24.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.OilBarrel, null, tint = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(20.dp))
                    Column { Text("ARLA 32", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text("Registre abastecimentos de ARLA e controle a nota.", fontSize = 13.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbastecimentoFormScreen(tipo: TipoAbastecimento, placa: String, veiculoId: Long, registroId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    val tabela = if (tipo == TipoAbastecimento.COMBUSTIVEL) "combustivel" else "arla"
    val pastaAnexo = if (tipo == TipoAbastecimento.COMBUSTIVEL) "Abastecimentocombustivel" else "Abastecimentoarla"

    var nro by remember { mutableStateOf("00001") }
    var data by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("pt","BR")).format(Date())) }
    var kmInicial by remember { mutableStateOf("") }; var kmAtual by remember { mutableStateOf("") }
    var litros by remember { mutableStateOf("") }; var valorTotal by remember { mutableStateOf("") }
    var valorLitro by remember { mutableStateOf("") }; var posto by remember { mutableStateOf("") }
    var uf by remember { mutableStateOf("") }; var rota by remember { mutableStateOf("") }
    var nf by remember { mutableStateOf("") }; var obs by remember { mutableStateOf("") }
    var pathNota by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (registroId >= 0) {
            db.queryAll(tabela).firstOrNull { (it["id"] as? Long) == registroId }?.let { r ->
                nro = db.str(r["id_abastecimento"]); data = db.str(r["data_registro"])
                kmInicial = db.num(r["km_inicial"]).let { if (it > 0) "%.1f".format(it) else "" }
                kmAtual = db.num(r["km_final"]).let { if (it > 0) "%.1f".format(it) else "" }
                litros = db.num(r["litros"]).let { if (it > 0) "%.1f".format(it) else "" }
                valorTotal = db.num(r["valor_total"]).let { if (it > 0) "%.2f".format(it) else "" }
                valorLitro = db.num(r["valor_litro"]).let { if (it > 0) "%.2f".format(it) else "" }
                posto = db.str(r["posto"]); nf = db.str(r["nota_fiscal"]); obs = db.str(r["observacoes"])
                if (tipo == TipoAbastecimento.COMBUSTIVEL) { uf = db.str(r["uf"]); rota = db.str(r["rota"]) }
                pathNota = db.str(r["path_nota"]).ifBlank { null }
            }
        } else {
            nro = db.proximoIdAbastecimento(tabela)
            db.ultimoAbastecimento(tabela, placa)?.let { u ->
                val km = db.num(u["km_final"]); if (km > 0) kmInicial = "%.1f".format(km)
            }
        }
    }

    // Cálculos automáticos (mesma lógica da planilha Flutter)
    val kmI = kmInicial.replace(",", ".").toDoubleOrNull() ?: 0.0
    val kmA = kmAtual.replace(",", ".").toDoubleOrNull() ?: 0.0
    val kmRodado = maxOf(0.0, kmA - kmI)
    val lit = litros.replace(",", ".").toDoubleOrNull() ?: 0.0
    val vTot = valorTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
    val vLit = if (lit > 0 && vTot > 0) vTot / lit else (valorLitro.replace(",", ".").toDoubleOrNull() ?: 0.0)
    val consumo = if (lit > 0) kmRodado / lit else null
    val custoKm = if (kmRodado > 0) vTot / kmRodado else null

    val picker = rememberAnexoPicker(pastaAnexo, "${nro}_${placa.sanitized()}") { pathNota = it }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mutableMapOf<String, Any?>(
            "id_abastecimento" to nro, "id_unidade" to veiculoId.toString(),
            "placa_principal" to placa, "data_registro" to data,
            "km_inicial" to kmI, "km_final" to kmA, "km_rodado" to kmRodado,
            "litros" to lit, "valor_total" to vTot, "valor_litro" to vLit,
            "posto" to posto, "nota_fiscal" to nf, "observacoes" to obs,
            "consumo_km_l" to consumo, "custo_km" to custoKm, "path_nota" to pathNota)
        if (tipo == TipoAbastecimento.COMBUSTIVEL) {
            row["combustivel"] = "Diesel S10"; row["uf"] = uf; row["rota"] = rota
        }
        if (registroId >= 0) db.update(tabela, registroId, row) else db.insert(tabela, row)
        val mm = db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa }
            ?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}".trim() } ?: ""
        if (tipo == TipoAbastecimento.COMBUSTIVEL) ProntuarioService.exportarCombustivel(ctx, db, placa, mm)
        else ProntuarioService.exportarArla(ctx, db, placa, mm)
        onBack()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (tipo == TipoAbastecimento.COMBUSTIVEL) "Abastecimento" else "ARLA 32") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Placa: $placa", fontWeight = FontWeight.Bold)
                    Text("ID: $nro", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(data, { data = it }, label = { Text("Data") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(kmInicial, { kmInicial = it }, label = { Text("KM Inicial") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(kmAtual, { kmAtual = it }, label = { Text("KM Atual") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField("%.1f".format(kmRodado), {}, label = { Text("KM Rodado") }, readOnly = true, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(litros, { litros = it }, label = { Text("Litros") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(valorTotal, { valorTotal = it }, label = { Text("Valor Total R$") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField("%.2f".format(vLit), { valorLitro = it }, label = { Text("Valor por Litro R$") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(posto, { posto = it }, label = { Text("Posto") }, modifier = Modifier.fillMaxWidth())
            if (tipo == TipoAbastecimento.COMBUSTIVEL) {
                Row { OutlinedTextField(uf, { uf = it }, label = { Text("UF") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(rota, { rota = it }, label = { Text("Rota") }, modifier = Modifier.weight(3f)) }
            }
            OutlinedTextField(nf, { nf = it }, label = { Text("Nº Nota Fiscal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(obs, { obs = it }, label = { Text("Observações") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Consumo Km/L", fontSize = 12.sp, color = Color.Gray)
                        Text(consumo?.let { "%.2f km/L".format(it) } ?: "—", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Custo Km R$", fontSize = 12.sp, color = Color.Gray)
                        Text(custoKm?.let { DatabaseHelper.fmtBRL(it) } ?: "—", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = picker, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (pathNota != null) Color(0xFF2E7D32) else Color(0xFF546E7A))) {
                Text(if (pathNota != null) "Nota Fiscal Anexada ✓" else "Anexar Nota Fiscal", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))) {
                Text("SALVAR", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbastecimentoHistoricoScreen(tipo: TipoAbastecimento, placa: String, veiculoId: Long,
    onNovo: () -> Unit, onEditar: (Long) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    val tabela = if (tipo == TipoAbastecimento.COMBUSTIVEL) "combustivel" else "arla"
    var registros by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    fun carregar() = scope.launch(Dispatchers.IO) { registros = db.abastecimentosPorPlaca(tabela, placa) }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(topBar = { TopAppBar(title = { Text(if (tipo == TipoAbastecimento.COMBUSTIVEL) "Histórico de Combustível" else "Histórico de ARLA 32") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        floatingActionButton = { FloatingActionButton(onClick = onNovo, containerColor = Color(0xFFE65100)) { Icon(Icons.Default.Add, null) } }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(registros, key = { (it["id"] as? Long) ?: 0L }) { r ->
                val id = (r["id"] as? Long) ?: 0L
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Data: ${db.str(r["data_registro"])}", fontWeight = FontWeight.Bold)
                            Text("ID: ${db.str(r["id_abastecimento"])}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Posto: ${db.str(r["posto"]).ifBlank { "-" }}")
                            Text("Litros: ${"%.2f".format(db.num(r["litros"]))} L")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("R$/L: ${DatabaseHelper.fmtBRL(db.num(r["valor_litro"]))}")
                            Text("Total: ${DatabaseHelper.fmtBRL(db.num(r["valor_total"]))}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("KM: ${"%.1f".format(db.num(r["km_rodado"]))}", fontSize = 12.sp)
                            Text("Consumo: ${r["consumo_km_l"]?.let { "%.2f Km/L".format(db.num(it)) } ?: "-"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("Custo: ${r["custo_km"]?.let { "${DatabaseHelper.fmtBRL(db.num(it))} /Km" } ?: "-"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        if (db.str(r["path_nota"]).isNotBlank()) {
                            Spacer(Modifier.height(8.dp)); VisualizadorMidia(db.str(r["path_nota"]))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { onEditar(id) }) { Icon(Icons.Default.Edit, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.str(r["path_nota"]).takeIf { it.isNotBlank() }?.let { File(it).delete() }
                                    db.delete(tabela, id); carregar()
                                }
                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}
