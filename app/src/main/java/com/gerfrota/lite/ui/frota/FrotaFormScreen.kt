package com.gerfrota.lite.ui.frota

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color          // ✅ ADICIONADO (usado em AnexoBotao e no botão SALVAR)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp              // ✅ ADICIONADO (fontSize = 11.sp no AnexoBotao)
import com.gerfrota.lite.core.VeiculoConstants
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.ui.widgets.CampoData
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ... (mantenha TODO o resto do código do anexo: @OptIn, fun FrotaFormScreen(...) e fun AnexoBotao(...))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrotaFormScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()

    var placa by remember { mutableStateOf("") }; var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }; var cor by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf<String?>(null) }
    var carroceria by remember { mutableStateOf<String?>(null) }
    var anoFab by remember { mutableStateOf("") }; var anoMod by remember { mutableStateOf("") }
    var renavam by remember { mutableStateOf("") }; var vencLic by remember { mutableStateOf("") }
    var chassi by remember { mutableStateOf("") }; var antt by remember { mutableStateOf("") }
    var vencAntt by remember { mutableStateOf("") }; var qtdPneus by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var pathFotoVeiculo by remember { mutableStateOf<String?>(null) }
    var pathFotoPlaca by remember { mutableStateOf<String?>(null) }
    var pathCrlv by remember { mutableStateOf<String?>(null) }
    var pathAntt by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id >= 0) withContext(Dispatchers.IO) {
            db.queryAll("frota").firstOrNull { (it["id"] as? Long) == id }?.let {
                placa = db.str(it["placa"]); marca = db.str(it["marca"]); modelo = db.str(it["modelo"])
                cor = db.str(it["cor"]); tipo = db.str(it["tipo_veiculo"]).ifBlank { null }
                carroceria = db.str(it["carroceria"]).ifBlank { null }
                anoFab = db.str(it["ano_fabricacao"]); anoMod = db.str(it["ano_modelo"])
                renavam = db.str(it["renavam"]); vencLic = db.str(it["vencimento_licenciamento"])
                chassi = db.str(it["chassi"]); antt = db.str(it["antt"]); vencAntt = db.str(it["vencimento_antt"])
                qtdPneus = db.str(it["quantidade_pneus"]); obs = db.str(it["observacao"])
                pathFotoVeiculo = db.str(it["caminho_foto_veiculo"]).ifBlank { null }
                pathFotoPlaca = db.str(it["caminho_foto_placa"]).ifBlank { null }
                pathCrlv = db.str(it["caminho_foto_crlv"]).ifBlank { null }
                pathAntt = db.str(it["caminho_foto_antt"]).ifBlank { null }
            }
        }
    }
    // Carroceria e qtd de pneus reagem ao tipo (igual ao Flutter)
    LaunchedEffect(tipo) {
        tipo?.let { t ->
            qtdPneus = VeiculoConstants.quantidadePneus(t).toString()
            if (carroceria !in VeiculoConstants.carroceriasPorTipo(t)) carroceria = null
        }
    }

    val base = placa.ifBlank { "SEM_PLACA" }.uppercase().replace(" ", "_")
    val pickFotoVeic = rememberAnexoPicker("FotosVeiculos", "${base}_FOTO_VEICULO") { pathFotoVeiculo = it }
    val pickFotoPlaca = rememberAnexoPicker("FotosVeiculos", "${base}_FOTO_PLACA") { pathFotoPlaca = it }
    val pickCrlv = rememberAnexoPicker("DocumentosVeiculos", "${base}_CRLV") { pathCrlv = it }
    val pickAntt = rememberAnexoPicker("DocumentosVeiculos", "${base}_ANTT") { pathAntt = it }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mapOf(
            "placa" to placa.uppercase().trim(), "marca" to marca, "modelo" to modelo, "cor" to cor,
            "tipo_veiculo" to tipo, "ano_fabricacao" to anoFab, "ano_modelo" to anoMod,
            "renavam" to renavam, "vencimento_licenciamento" to vencLic, "chassi" to chassi,
            "antt" to antt, "vencimento_antt" to vencAntt, "carroceria" to carroceria,
            "quantidade_pneus" to qtdPneus, "observacao" to obs,
            "caminho_foto_veiculo" to pathFotoVeiculo, "caminho_foto_placa" to pathFotoPlaca,
            "caminho_foto_crlv" to pathCrlv, "caminho_foto_antt" to pathAntt)
        if (id >= 0) db.update("frota", id, row) else db.insert("frota", row)
        withContext(Dispatchers.Main) { onBack() }
    }

    @Composable
    fun AnexoBotao(label: String, path: String?, pick: () -> Unit) {
        Button(onClick = pick, colors = ButtonDefaults.buttonColors(
            containerColor = if (path != null) Color(0xFF2E7D32) else Color(0xFF546E7A)),
            modifier = Modifier.weight(1f).height(56.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (id >= 0) "Editar Veículo" else "Novo Veículo") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(placa, { placa = it.uppercase().take(7) }, label = { Text("Placa *") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(marca, { marca = it }, label = { Text("Marca") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(modelo, { modelo = it }, label = { Text("Modelo") }, modifier = Modifier.weight(1f)) }
            Row { OutlinedTextField(cor, { cor = it }, label = { Text("Cor") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(qtdPneus, { qtdPneus = it }, label = { Text("Qtd. Pneus") }, readOnly = true, modifier = Modifier.weight(1f)) }

            var expTipo by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expTipo, onExpandedChange = { expTipo = it }) {
                OutlinedTextField(tipo ?: "", {}, readOnly = true, label = { Text("Tipo de Veículo *") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expTipo) })
                ExposedDropdownMenu(expTipo, { expTipo = false }) {
                    VeiculoConstants.tiposVeiculo.forEach { t -> DropdownMenuItem({ Text(t) }, { tipo = t }) }
                }
            }
            var expCar by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expCar, onExpandedChange = { expCar = it }) {
                OutlinedTextField(carroceria ?: "", {}, readOnly = true, label = { Text("Carroceria") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expCar) })
                ExposedDropdownMenu(expCar, { expCar = false }) {
                    VeiculoConstants.carroceriasPorTipo(tipo).forEach { c -> DropdownMenuItem({ Text(c) }, { carroceria = c }) }
                }
            }
            Row { OutlinedTextField(anoFab, { anoFab = it }, label = { Text("Ano Fab.") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(anoMod, { anoMod = it }, label = { Text("Ano Modelo") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(renavam, { renavam = it }, label = { Text("Renavam") }, modifier = Modifier.fillMaxWidth())
            Row { CampoData("Venc. Licenciamento", vencLic, { vencLic = it }, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoData("Venc. ANTT", vencAntt, { vencAntt = it }, Modifier.weight(1f)) }
            OutlinedTextField(chassi, { chassi = it }, label = { Text("Chassi") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(antt, { antt = it }, label = { Text("ANTT") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(obs, { obs = it }, label = { Text("Observações") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

            Text("Anexos de Documentos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnexoBotao(if (pathFotoVeiculo != null) "Foto Veículo ✓" else "Foto Veículo", pathFotoVeiculo, pickFotoVeic)
                AnexoBotao(if (pathFotoPlaca != null) "Foto Placa ✓" else "Foto Placa", pathFotoPlaca, pickFotoPlaca)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnexoBotao(if (pathCrlv != null) "CRLV ✓" else "CRLV", pathCrlv, pickCrlv)
                AnexoBotao(if (pathAntt != null) "ANTT ✓" else "ANTT", pathAntt, pickAntt)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))) {
                Text("SALVAR VEÍCULO", fontWeight = FontWeight.Bold)
            }
        }
    }
}
