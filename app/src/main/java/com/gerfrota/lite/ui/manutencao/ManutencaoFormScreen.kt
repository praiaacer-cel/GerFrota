package com.gerfrota.lite.ui.manutencao

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.gerfrota.lite.core.EstruturaManutencao
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.core.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManutencaoFormScreen(
    placa: String, tipo: String, manutencaoId: Long, pneuId: Long,
    resultadoPneu: String?, onConsumirResultado: () -> Unit,
    nav: NavController
) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())) }
    var km by remember { mutableStateOf("") }
    var sistema by remember { mutableStateOf<String?>(null) }
    var subsistema by remember { mutableStateOf<String?>(null) }
    var tipoServico by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var nf by remember { mutableStateOf("") }
    var prestador by remember { mutableStateOf("") }
    var pathNota by remember { mutableStateOf<String?>(null) }
    // rotinas de pneus
    var pneuTipo by remember { mutableStateOf<String?>(null) }
    var pneuRetirada by remember { mutableStateOf<String?>(null) }
    var nomeBorracharia by remember { mutableStateOf("") }
    var nomeRecapadora by remember { mutableStateOf("") }
    var descricaoConserto by remember { mutableStateOf("") }

    val estrutura = remember { EstruturaManutencao.estruturaPara(tipo) }
    val subsistemas = sistema?.let { estrutura[it] ?: emptyList() } ?: emptyList()

    // edição
    LaunchedEffect(manutencaoId) {
        if (manutencaoId >= 0) withContext(Dispatchers.IO) {
            db.queryAll("manutencoes").firstOrNull { (it["id"] as? Long) == manutencaoId }?.let { m ->
                data = db.str(m["data_servico"]); km = db.num(m["quilometragem"]).toInt().toString()
                sistema = db.str(m["sistema"]); subsistema = db.str(m["subsistema"])
                tipoServico = db.str(m["tipo_servico"]); obs = db.str(m["observacao"])
                valor = db.str(m["valor_servico"]); nf = db.str(m["numero_nota"])
                prestador = db.str(m["prestador"]); pathNota = db.str(m["caminho_nota_arquivo"]).ifBlank { null }
            }
        }
        // instalação vinda do estoque
        if (pneuId >= 0) withContext(Dispatchers.IO) {
            db.queryAll("pneus").firstOrNull { (it["id"] as? Long) == pneuId }?.let { p ->
                sistema = "Pneus"; pneuTipo = "Montagem"
                descricaoConserto = "Montagem de pneu ${db.str(p["codigo_fogo"])} retirado do estoque"
                tipoServico = descricaoConserto
            }
        }
    }

    // resultado do mapa de pneus (posição - código)
    LaunchedEffect(resultadoPneu) {
        resultadoPneu?.let { subsistema = it; onConsumirResultado() }
    }

    val pickNota = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
       uri?.let {
           scope.launch(Dispatchers.IO) {
               val pasta = PathHelper.pastaNotasServicos(ctx)          // ✅ era File(db.baseDir(), "NotasdosServicos")
               val ext = ctx.contentResolver.getType(it)?.substringAfter('/') ?: "jpg"
               val nome = "${placa}_${nf.ifBlank { "SEM_NOTA" }}.$ext"
               val dest = File(pasta, nome)
               ctx.contentResolver.openInputStream(it)?.use { inp -> dest.outputStream().use { o -> inp.copyTo(o) } }
               pathNota = dest.absolutePath
           }
       }
   }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val valorDouble = DatabaseHelper.parseMoney(valor)
        val valorDb = DatabaseHelper.fmtBRL(valorDouble)
        val dados = mapOf(
            "placa_veiculo" to placa, "data_servico" to data,
            "quilometragem" to (km.toIntOrNull() ?: 0),
            "sistema" to (sistema ?: ""), "subsistema" to (subsistema ?: ""),
            "tipo_servico" to tipoServico, "observacao" to obs,
            "valor_servico" to valorDb, "numero_nota" to nf,
            "prestador" to prestador, "caminho_nota_arquivo" to pathNota)
        val idOS = if (manutencaoId >= 0) { db.update("manutencoes", manutencaoId, dados); manutencaoId }
        else db.insert("manutencoes", dados)

        // ---- rotinas de pneus ----
        if (sistema == "Pneus" && subsistema != null) {
            val sub = subsistema!!
            val codigo = if (sub.contains(" - ")) sub.substringAfter(" - ").trim() else null
            val posicao = if (sub.contains(" - ")) sub.substringBefore(" - ").trim() else sub
            val acumulado = (codigo?.let { db.valorAcumuladoPneu(it) } ?: 0.0) + valorDouble
            when {
                pneuTipo == "Montagem" && codigo != null ->
                    db.updatePneuByCodigo(codigo, mapOf("status" to "Em uso", "posicao_atual" to posicao,
                        "veiculo_id" to placa, "data_instalacao" to data,
                        "km_instalacao" to (km.toDoubleOrNull() ?: 0.0),
                        "observacao" to "Montado na posição $posicao"))
                pneuTipo == "Montagem" && pneuId >= 0 ->
                    db.update("pneus", pneuId, mapOf("status" to "Em uso", "posicao_atual" to posicao,
                        "veiculo_id" to placa, "data_instalacao" to data,
                        "km_instalacao" to (km.toDoubleOrNull() ?: 0.0)))
                pneuTipo == "Retirada" && codigo != null -> when (pneuRetirada) {
                    "para o Estoque" -> db.updatePneuByCodigo(codigo, mapOf("status" to "Estoque",
                        "posicao_atual" to "Estoque", "veiculo_id" to null, "valor_compra" to acumulado,
                        "observacao" to if (nomeBorracharia.isBlank()) "Sede" else "Borracharia: $nomeBorracharia"))
                    "para o Descarte" -> {
                        val p = db.queryAll("pneus").firstOrNull { db.str(it["codigo_fogo"]) == codigo }
                        p?.let { db.insert("descartes", mapOf("pneu_id" to it["id"], "data_descarte" to data,
                            "motivo" to "Retirada via Manutenção", "km_descarte" to (km.toDoubleOrNull() ?: 0.0),
                            "observacao" to "Valor Acumulado no Descarte: ${DatabaseHelper.fmtBRL(acumulado)}", "cpk_final" to 0.0)) }
                        db.updatePneuByCodigo(codigo, mapOf("status" to "Descartado",
                            "posicao_atual" to "Descartado", "veiculo_id" to null, "valor_compra" to acumulado))
                    }
                    "para o Recapagem" -> db.updatePneuByCodigo(codigo, mapOf("status" to "Em Recapagem",
                        "posicao_atual" to "Estoque", "veiculo_id" to null, "valor_compra" to acumulado,
                        "observacao" to "Recapadora: $nomeRecapadora"))
                }
            }
        }

        // ---- prontuário .txt ----
        val os = idOS.toString().padStart(5, '0')
        val bloco = "OS: $os\nData: $data | KM: $km | $sistema - $subsistema\n" +
            "Serviço: $tipoServico | Obs: $obs\nValor: $valorDb | Prestador: $prestador | Nota: $nf\n"
        val f = PathHelper.prontuarioPlaca(ctx, placa)   // ✅ era File(File(db.baseDir(), "ProntuarioPlaca")...)
        val sep = "-".repeat(60) + "\n"
        val existentes = if (f.exists()) f.readText().split(sep).map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains("OS: $os") } else emptyList()
        f.writeText((existentes + bloco.trim()).joinToString(sep) { it + "\n" } + sep)

        nav.popBackStack()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("CADASTRO DE MANUTENÇÃO") },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp).verticalScroll(rememberScrollState())) {
            Row { Campo("Data", data) { data = it }; Spacer(Modifier.width(8.dp)); Campo("KM", km) { km = it } }
            DropdownSistema(sistema, estrutura.keys.toList()) { sistema = it; subsistema = null; pneuTipo = null }
            if (sistema == "Pneus") {
                OutlinedButton(onClick = {
                    nav.navigate("pneus_map/$placa/${java.net.URLEncoder.encode(tipo, "UTF-8")}/1")
                }) { Text(subsistema ?: "Toque para selecionar a posição no layout") }
                if (subsistema != null) {
                    DropdownSimples("Tipo de Serviço (Pneus)", pneuTipo,
                        listOf("Conserto", "Retirada", "Montagem")) { v ->
                        pneuTipo = v; pneuRetirada = null
                        tipoServico = when (v) {
                            "Conserto" -> "Conserto"; "Montagem" -> descricaoConserto.ifBlank { "Montagem" }
                            else -> tipoServico
                        }
                    }
                    if (pneuTipo == "Conserto" || pneuTipo == "Montagem")
                        Campo("Descrição", descricaoConserto) { descricaoConserto = it; tipoServico = it }
                    if (pneuTipo == "Retirada") {
                        DropdownSimples("Destino da Retirada", pneuRetirada,
                            listOf("para o Estoque", "para o Descarte", "para o Recapagem")) { v ->
                            pneuRetirada = v; tipoServico = "Retirada $v"
                        }
                        if (pneuRetirada == "para o Estoque") Campo("Borracharia (opcional)", nomeBorracharia) { nomeBorracharia = it }
                        if (pneuRetirada == "para o Recapagem") Campo("Recapadora", nomeRecapadora) { nomeRecapadora = it }
                    }
                }
            } else if (sistema != null) {
                DropdownSimples("SUBSISTEMA", subsistema, subsistemas) { subsistema = it }
                Campo("Tipo de Serviço", tipoServico) { tipoServico = it }
            }
            Campo("Observação", obs) { obs = it }
            Row { Campo("Valor R$", valor) { valor = it }; Spacer(Modifier.width(8.dp)); Campo("NF", nf) { nf = it } }
            Campo("Prestador", prestador) { prestador = it }
            Row(Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { pickNota.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pathNota != null) Color(0xFF2E7D32) else Color(0xFF1976D2))) {
                    Text(if (pathNota != null) "NOTA ✓" else "NOTA SERVIÇO")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { salvar() }) { Text("SALVAR SERVIÇO") }
            }
        }
    }
}

@Composable fun Campo(label: String, value: String, on: (String) -> Unit) {
    OutlinedTextField(value, on, label = { Text(label) }, modifier = Modifier.weight(1f))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DropdownSistema(value: String?, opcoes: List<String>, on: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(value ?: "", {}, readOnly = true, label = { Text("SISTEMA") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
        ExposedDropdownMenu(exp, { exp = false }) { opcoes.forEach { o -> DropdownMenuItem({ Text(o) }, { on(o) }) } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DropdownSimples(label: String, value: String?, opcoes: List<String>, on: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
        OutlinedTextField(value ?: "", {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
        ExposedDropdownMenu(exp, { exp = false }) { opcoes.forEach { o -> DropdownMenuItem({ Text(o) }, { on(o) }) } }
    }
}
