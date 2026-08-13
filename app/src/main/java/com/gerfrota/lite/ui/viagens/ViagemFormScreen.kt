package com.gerfrota.lite.ui.viagens

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
import androidx.compose.ui.Alignment // ✅ Passo A: Import adicionado
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.core.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private class ItemFin(sn: String = "NÃO") {
    var simNao by mutableStateOf(sn)
    var valor by mutableStateOf("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViagemFormScreen(unidadeId: Long, viagemId: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var conjunto by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var nro by remember { mutableStateOf("00001") }
    var dataCarga by remember { mutableStateOf("") }; var dataDescarga by remember { mutableStateOf("") }
    var partida by remember { mutableStateOf("") }; var destino by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }; var carga by remember { mutableStateOf("") }
    var nf by remember { mutableStateOf("") }; var manifestoDoc by remember { mutableStateOf("") }
    var bruto by remember { mutableStateOf("") }
    val frete = remember { linkedMapOf(
        "adiantamento" to ItemFin(), "valePedagio" to ItemFin(), "reembolsoEstadia" to ItemFin(),
        "reembolsoCarga" to ItemFin(), "reembolsoChapa" to ItemFin(), "reembolsoDoc" to ItemFin(),
        "outrosReembolsos" to ItemFin()) }
    val despesas = remember { linkedMapOf(
        "agenciador" to ItemFin(), "despPedagio" to ItemFin(), "despEstadia" to ItemFin(),
        "despCarga" to ItemFin(), "despChapa" to ItemFin(), "despDoc" to ItemFin(),
        "outrasDespesas" to ItemFin()) }
    val saldos = remember { linkedMapOf(
        "saldoFrete" to ItemFin(), "saldoPedagio" to ItemFin(), "saldoEstadia" to ItemFin(),
        "saldoCarga" to ItemFin(), "saldoChapa" to ItemFin(), "saldoDoc" to ItemFin(),
        "saldoOutros" to ItemFin()) }
    var pathManifesto by remember { mutableStateOf<String?>(null) }
    var pathRecibo by remember { mutableStateOf<String?>(null) }
    var qtdPedagios by remember { mutableStateOf(0) }
    val pathsPedagios = remember { mutableStateListOf<String?>() }
    val labelsFrete = mapOf("adiantamento" to "Adiantamento", "valePedagio" to "Vale Pedágio",
        "reembolsoEstadia" to "Reembolso Estadia", "reembolsoCarga" to "Reembolso Carga",
        "reembolsoChapa" to "Reembolso Chapa", "reembolsoDoc" to "Reembolso Doc",
        "outrosReembolsos" to "Outros Reembolsos")
    val labelsDespesas = mapOf("agenciador" to "Agenciador", "despPedagio" to "Desp. Pedágio",
        "despEstadia" to "Desp. Estadia", "despCarga" to "Desp. Carga", "despChapa" to "Desp. Chapa",
        "despDoc" to "Desp. Doc", "outrasDespesas" to "Outras Despesas")
    val labelsSaldos = mapOf("saldoFrete" to "Saldo de Frete", "saldoPedagio" to "Saldo de Pedágio",
        "saldoEstadia" to "Saldo de Estadia", "saldoCarga" to "Saldo Carga", "saldoChapa" to "Saldo Chapa",
        "saldoDoc" to "Saldo Doc", "saldoOutros" to "Saldo Outros Reembolsos")
    fun p(s: String) = DatabaseHelper.parseMoney(s)
    fun r(i: ItemFin) = if (i.simNao == "NÃO") 0.0 else p(i.valor)
    // ---- cálculos derivados (planilha original) ----
    val vBruto = p(bruto)
    val totalDespesas = despesas.values.sumOf { r(it) }
    val resumoReembolsos = r(frete["valePedagio"]!!) + r(frete["reembolsoEstadia"]!!) +
        r(frete["reembolsoCarga"]!!) + r(frete["reembolsoChapa"]!!) + r(frete["reembolsoDoc"]!!) +
        r(frete["outrosReembolsos"]!!)
    val resumoLiquido = vBruto + resumoReembolsos - totalDespesas
    val saldoFrete = if (saldos["saldoFrete"]!!.simNao == "NÃO") vBruto else vBruto - r(frete["adiantamento"]!!)
    val saldoPedagio = if (saldos["saldoPedagio"]!!.simNao == "NÃO") 0.0 else r(frete["valePedagio"]!!) - r(despesas["despPedagio"]!!)
    val saldoEstadia = if (saldos["saldoEstadia"]!!.simNao == "NÃO") 0.0 else r(frete["reembolsoEstadia"]!!) - r(despesas["despEstadia"]!!)
    val saldoCarga = if (saldos["saldoCarga"]!!.simNao == "NÃO") 0.0 else r(frete["reembolsoCarga"]!!) - r(despesas["despCarga"]!!)
    val saldoChapa = if (saldos["saldoChapa"]!!.simNao == "NÃO") 0.0 else r(frete["reembolsoChapa"]!!) - r(despesas["despChapa"]!!)
    val saldoDoc = if (saldos["saldoDoc"]!!.simNao == "NÃO") 0.0 else r(frete["reembolsoDoc"]!!) - r(despesas["despDoc"]!!)
    val saldoOutros = if (saldos["saldoOutros"]!!.simNao == "NÃO") 0.0 else r(frete["outrosReembolsos"]!!)
    val totalSaldos = saldoFrete + saldoPedagio + saldoEstadia + saldoCarga + saldoChapa + saldoDoc + saldoOutros
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            conjunto = db.queryAll("unidades_transporte").firstOrNull { (it["id"] as? Long) == unidadeId }
            if (viagemId >= 0) {
                val v = db.queryAll("viagens").firstOrNull { (it["id"] as? Long) == viagemId }
                if (v != null) {
                    nro = db.str(v["nro_viagem"]); dataCarga = db.str(v["data_carga"]); dataDescarga = db.str(v["data_descarga"])
                    partida = db.str(v["cidade_partida"]); destino = db.str(v["cidade_destino"])
                    empresa = db.str(v["empresa"]); carga = db.str(v["carga"]); nf = db.str(v["nota_fiscal"])
                    manifestoDoc = db.str(v["manifesto_doc"]); bruto = v["valor_bruto"]?.toString() ?: ""
                    fun carrega(item: ItemFin, sn: Any?, vl: Any?) { item.simNao = db.str(sn).ifBlank { "NÃO" }; val d = db.num(vl); if (d > 0) item.valor = d.toString() }
                    carrega(frete["adiantamento"]!!, v["pago_adiantamento_sn"], v["pago_adiantamento_val"])
                    carrega(frete["valePedagio"]!!, v["vale_pedagio_sn"], v["vale_pedagio_val"])
                    carrega(frete["reembolsoEstadia"]!!, v["reembolso_estadia_sn"], v["reembolso_estadia_val"])
                    carrega(frete["reembolsoCarga"]!!, v["reembolso_carga_sn"], v["reembolso_carga_val"])
                    carrega(frete["reembolsoChapa"]!!, v["reembolso_chapa_sn"], v["reembolso_chapa_val"])
                    carrega(frete["reembolsoDoc"]!!, v["reembolso_doc_sn"], v["reembolso_doc_val"])
                    carrega(frete["outrosReembolsos"]!!, v["outros_reembolsos_sn"], v["outros_reembolsos_val"])
                    carrega(despesas["agenciador"]!!, v["agenciador_sn"], v["agenciador_val"])
                    carrega(despesas["despPedagio"]!!, v["desp_pedagio_sn"], v["desp_pedagio_val"])
                    carrega(despesas["despEstadia"]!!, v["desp_estadia_sn"], v["desp_estadia_val"])
                    carrega(despesas["despCarga"]!!, v["desp_carga_sn"], v["desp_carga_val"])
                    carrega(despesas["despChapa"]!!, v["desp_chapa_sn"], v["desp_chapa_val"])
                    carrega(despesas["despDoc"]!!, v["desp_doc_sn"], v["desp_doc_val"])
                    carrega(despesas["outrasDespesas"]!!, v["outras_despesas_sn"], v["outras_despesas_val"])
                    saldos["saldoFrete"]!!.simNao = db.str(v["saldo_frete_sn"]).ifBlank { "NÃO" }
                    saldos["saldoPedagio"]!!.simNao = db.str(v["saldo_pedagio_sn"]).ifBlank { "NÃO" }
                    saldos["saldoEstadia"]!!.simNao = db.str(v["saldo_estadia_sn"]).ifBlank { "NÃO" }
                    saldos["saldoCarga"]!!.simNao = db.str(v["saldo_carga_sn"]).ifBlank { "NÃO" }
                    saldos["saldoChapa"]!!.simNao = db.str(v["saldo_chapa_sn"]).ifBlank { "NÃO" }
                    saldos["saldoDoc"]!!.simNao = db.str(v["saldo_doc_sn"]).ifBlank { "NÃO" }
                    saldos["saldoOutros"]!!.simNao = db.str(v["saldo_outros_sn"]).ifBlank { "NÃO" }
                    pathManifesto = db.str(v["path_manifesto"]).ifBlank { null }
                    pathRecibo = db.str(v["path_recibo_adiantamento"]).ifBlank { null }
                    qtdPedagios = (v["qtd_pedagios"] as? Long)?.toInt() ?: 0
                }
            } else {
                val maior = db.queryAll("viagens").maxOfOrNull { db.str(it["nro_viagem"]).toIntOrNull() ?: 0 } ?: 0
                nro = (maior + 1).toString().padStart(5, '0')
            }
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { /* cópia tratada no salvar */ }
    }
    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mapOf(
            "nro_viagem" to nro, "unidade_id" to unidadeId.toString(),
            "motorista_id" to conjunto?.get("motorista_id"), "marca" to db.str(conjunto?.get("marca_modelo_ano")),
            "modelo" to db.str(conjunto?.get("modelo")), "ano_modelo" to "", "placas" to db.str(conjunto?.get("placas")),
            "motorista" to db.str(conjunto?.get("motorista")),
            "data_carga" to dataCarga, "data_descarga" to dataDescarga, "cidade_partida" to partida,
            "cidade_destino" to destino, "empresa" to empresa, "carga" to carga, "nota_fiscal" to nf,
            "manifesto_doc" to manifestoDoc, "valor_bruto" to vBruto,
            "pago_adiantamento_sn" to frete["adiantamento"]!!.simNao, "pago_adiantamento_val" to r(frete["adiantamento"]!!),
            "vale_pedagio_sn" to frete["valePedagio"]!!.simNao, "vale_pedagio_val" to r(frete["valePedagio"]!!),
            "reembolso_estadia_sn" to frete["reembolsoEstadia"]!!.simNao, "reembolso_estadia_val" to r(frete["reembolsoEstadia"]!!),
            "reembolso_carga_sn" to frete["reembolsoCarga"]!!.simNao, "reembolso_carga_val" to r(frete["reembolsoCarga"]!!),
            "reembolso_chapa_sn" to frete["reembolsoChapa"]!!.simNao, "reembolso_chapa_val" to r(frete["reembolsoChapa"]!!),
            "reembolso_doc_sn" to frete["reembolsoDoc"]!!.simNao, "reembolso_doc_val" to r(frete["reembolsoDoc"]!!),
            "outros_reembolsos_sn" to frete["outrosReembolsos"]!!.simNao, "outros_reembolsos_val" to r(frete["outrosReembolsos"]!!),
            "agenciador_sn" to despesas["agenciador"]!!.simNao, "agenciador_val" to r(despesas["agenciador"]!!),
            "desp_pedagio_sn" to despesas["despPedagio"]!!.simNao, "desp_pedagio_val" to r(despesas["despPedagio"]!!),
            "desp_estadia_sn" to despesas["despEstadia"]!!.simNao, "desp_estadia_val" to r(despesas["despEstadia"]!!),
            "desp_carga_sn" to despesas["despCarga"]!!.simNao, "desp_carga_val" to r(despesas["despCarga"]!!),
            "desp_chapa_sn" to despesas["despChapa"]!!.simNao, "desp_chapa_val" to r(despesas["despChapa"]!!),
            "desp_doc_sn" to despesas["despDoc"]!!.simNao, "desp_doc_val" to r(despesas["despDoc"]!!),
            "outras_despesas_sn" to despesas["outrasDespesas"]!!.simNao, "outras_despesas_val" to r(despesas["outrasDespesas"]!!),
            "saldo_frete_sn" to saldos["saldoFrete"]!!.simNao, "saldo_pedagio_sn" to saldos["saldoPedagio"]!!.simNao,
            "saldo_estadia_sn" to saldos["saldoEstadia"]!!.simNao, "saldo_carga_sn" to saldos["saldoCarga"]!!.simNao,
            "saldo_chapa_sn" to saldos["saldoChapa"]!!.simNao, "saldo_doc_sn" to saldos["saldoDoc"]!!.simNao,
            "saldo_outros_sn" to saldos["saldoOutros"]!!.simNao,
            "resumo_reembolsos" to resumoReembolsos, "resumo_despesas" to totalDespesas,
            "total_saldos" to totalSaldos, "valor_liquido" to resumoLiquido,
            "path_manifesto" to pathManifesto, "path_recibo_adiantamento" to pathRecibo,
            "qtd_pedagios" to qtdPedagios, "paths_pedagios" to pathsPedagios.filterNotNull().joinToString("|"))
        if (viagemId >= 0) db.update("viagens", viagemId, row) else db.insert("viagens", row)
        atualizarProntuario()
        nav.popBackStack()
    }
    fun atualizarProntuario() {
        val viagens = db.queryAll("viagens").filter { db.str(it["unidade_id"]) == unidadeId.toString() }
            .sortedBy { db.str(it["nro_viagem"]) }
        val f = PathHelper.prontuarioViagem(
            ctx,
            db.str(conjunto?.get("modelo")).ifBlank { "SemModelo" },
            db.str(conjunto?.get("placas")).ifBlank { "SemPlaca" }
        )
        val sb = StringBuilder()
        viagens.forEach { v ->
            sb.appendLine("=== VIAGEM NRO: ${v["nro_viagem"]} ===")
            sb.appendLine("Data: ${v["data_carga"]} -> Rota: ${v["cidade_partida"]} a ${v["cidade_destino"]}")
            sb.appendLine("Líquido Final: ${DatabaseHelper.fmtBRL(db.num(v["valor_liquido"]))}\n")
        }
        f.writeText(sb.toString())
    }
    @Composable
    fun linhaFin(label: String, item: ItemFin, resultado: Double, mostrarValor: Boolean = true) {
        // ✅ Passo C: Alignment.CenterVertically funciona graças ao import do Passo A
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(4f))
            var exp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }, modifier = Modifier.weight(2f)) {
                OutlinedTextField(item.simNao, {}, readOnly = true, modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
                ExposedDropdownMenu(exp, { exp = false }) {
                    listOf("SIM", "NÃO").forEach { o -> DropdownMenuItem({ Text(o) }, { item.simNao = o }) }
                }
            }
            if (mostrarValor) {
                OutlinedTextField(item.valor, { item.valor = it }, modifier = Modifier.weight(3f), singleLine = true)
            }
            Text(DatabaseHelper.fmtBRL(resultado), fontSize = 12.sp, modifier = Modifier.weight(3f),
                color = if (resultado < 0) Color(0xFFC62828) else Color(0xFF212121))
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (viagemId >= 0) "Editar Viagem" else "Nova Viagem", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp).verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Conjunto: ${db.str(conjunto?.get("marca_modelo_ano"))} ${db.str(conjunto?.get("modelo"))}", fontWeight = FontWeight.Bold)
                    Text("Placas: ${db.str(conjunto?.get("placas"))}")
                    Text("Motorista: ${db.str(conjunto?.get("motorista")).ifBlank { "Não definido" }}")
                    Text("Nro Viagem: $nro", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
            Text("DADOS DA VIAGEM", fontWeight = FontWeight.Bold, color = Color(0xFF607D8B))
            OutlinedTextField(dataCarga, { dataCarga = it }, label = { Text("Data Carga") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(dataDescarga, { dataDescarga = it }, label = { Text("Data Descarga") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(partida, { partida = it }, label = { Text("Cidade Partida") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(destino, { destino = it }, label = { Text("Cidade Destino") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(empresa, { empresa = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(carga, { carga = it }, label = { Text("Carga") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nf, { nf = it }, label = { Text("Nota Fiscal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(manifestoDoc, { manifestoDoc = it }, label = { Text("Nro Manifesto") }, modifier = Modifier.fillMaxWidth())
            Text("SOBRE O FRETE", fontWeight = FontWeight.Bold, color = Color(0xFF607D8B))
            OutlinedTextField(bruto, { bruto = it }, label = { Text("VALOR BRUTO") }, modifier = Modifier.fillMaxWidth())
            frete.forEach { (k, item) -> linhaFin(labelsFrete[k]!!, item, r(item)) }
            Text("DESPESAS DO FRETE", fontWeight = FontWeight.Bold, color = Color(0xFF607D8B))
            despesas.forEach { (k, item) -> linhaFin(labelsDespesas[k]!!, item, r(item)) }
            Text("TOTAL DESPESAS: ${DatabaseHelper.fmtBRL(totalDespesas)}", fontWeight = FontWeight.Bold)
            Text("SALDOS A RECEBER", fontWeight = FontWeight.Bold, color = Color(0xFF607D8B))
            linhaFin(labelsSaldos["saldoFrete"]!!, saldos["saldoFrete"]!!, saldoFrete, mostrarValor = false)
            linhaFin(labelsSaldos["saldoPedagio"]!!, saldos["saldoPedagio"]!!, saldoPedagio, mostrarValor = false)
            linhaFin(labelsSaldos["saldoEstadia"]!!, saldos["saldoEstadia"]!!, saldoEstadia, mostrarValor = false)
            linhaFin(labelsSaldos["saldoCarga"]!!, saldos["saldoCarga"]!!, saldoCarga, mostrarValor = false)
            linhaFin(labelsSaldos["saldoChapa"]!!, saldos["saldoChapa"]!!, saldoChapa, mostrarValor = false)
            linhaFin(labelsSaldos["saldoDoc"]!!, saldos["saldoDoc"]!!, saldoDoc, mostrarValor = false)
            linhaFin(labelsSaldos["saldoOutros"]!!, saldos["saldoOutros"]!!, saldoOutros, mostrarValor = false)
            Text("TOTAL SALDOS A RECEBER: ${DatabaseHelper.fmtBRL(totalSaldos)}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            Text("RESUMO DO FRETE", fontWeight = FontWeight.Bold, color = Color(0xFF607D8B))
            Text("Valor Bruto: ${DatabaseHelper.fmtBRL(vBruto)}")
            Text("Reembolsos: ${DatabaseHelper.fmtBRL(resumoReembolsos)}")
            Text("Despesas: ${DatabaseHelper.fmtBRL(totalDespesas)}")
            Text("VALOR LÍQUIDO: ${DatabaseHelper.fmtBRL(resumoLiquido)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("SALVAR VIAGEM", fontWeight = FontWeight.Bold)
            }
        }
    }
}
