package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcertoContasScreen(motoristaId: Long, inicioMs: Long, fimMs: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var itens by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var motorista by remember { mutableStateOf<Map<String, Any?>?>(null) }

    data class Acerto(val viagem: Map<String, Any?>, val bruto: Double, val agenciador: Double,
                      val valorCalc: Double, val pct: Double, val comissaoBruta: Double,
                      val adiantamento: Double, val dataAdiantamento: String?)

    var acertos by remember { mutableStateOf<List<Acerto>>(emptyList()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            motorista = db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == motoristaId }
            val nome = db.str(motorista?.get("nome")).trim().lowercase()
            val pct = DatabaseHelper.parseMoney(db.str(motorista?.get("comissao")))
            val inicio = Calendar.getInstance().apply { timeInMillis = inicioMs }
            val fim = Calendar.getInstance().apply { timeInMillis = fimMs }
            val viagens = db.queryAll("viagens").filter { v ->
                val d = DatabaseHelper.parseDataBR(db.str(v["data_carga"])) ?: return@filter false
                db.str(v["motorista"]).trim().lowercase() == nome &&
                    !d.before(inicio.apply { add(Calendar.DAY_OF_YEAR, -1) }) &&
                    !d.after(fim.apply { add(Calendar.DAY_OF_YEAR, 1) })
            }.sortedBy { db.str(it["data_carga"]) }
            val adiantamentos = db.queryAll("adiantamentos").filter { (it["motorista_id"] as? Long) == motoristaId }.toMutableList()
            val resultado = mutableListOf<Acerto>()
            viagens.forEachIndexed { i, v ->
                val dCarga = DatabaseHelper.parseDataBR(db.str(v["data_carga"]))!!
                val limite = if (i < viagens.size - 1) DatabaseHelper.parseDataBR(db.str(viagens[i + 1]["data_carga"])) ?: fim else fim
                var adiViagem = 0.0; var dataAdi: String? = null
                adiantamentos.removeIf { a ->
                    val dA = DatabaseHelper.parseDataBR(db.str(a["data"])) ?: return@removeIf false
                    val pertence = !dA.before(dCarga) && !dA.after(limite)
                    if (pertence) { adiViagem += db.num(a["valor"]); dataAdi = db.str(a["data"]) }
                    pertence
                }
                val bruto = db.num(v["valor_bruto"]); val ag = db.num(v["agenciador_val"])
                val calc = bruto - ag; val cb = calc * (pct / 100)
                resultado.add(Acerto(v, bruto, ag, calc, pct, cb, adiViagem, dataAdi))
            }
            acertos = resultado
        }
    }

    val totalComissoes = acertos.sumOf { it.comissaoBruta - it.adiantamento }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Acerto de Contas", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val f = PdfService.gerarAcertoPdf(ctx, db.str(motorista?.get("nome")),
                            "Período", acertos.map { Triple("Viagem ${it.viagem["nro_viagem"]}", "Bruto ${DatabaseHelper.fmtBRL(it.bruto)} / Líquida ${DatabaseHelper.fmtBRL(it.comissaoBruta - it.adiantamento)}", "") },
                            DatabaseHelper.fmtBRL(totalComissoes))
                        PdfService.compartilhar(ctx, f)
                    }
                }) { Text("Salvar PDF") }
            })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(14.dp)) {
            item {
                Text("Motorista: ${db.str(motorista?.get("nome"))}", fontWeight = FontWeight.Bold)
            }
            items(acertos) { a ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Viagem Nro ${a.viagem["nro_viagem"]}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        Text("${a.viagem["cidade_partida"]} → ${a.viagem["cidade_destino"]}", fontSize = 14.sp)
                        Text("Valor Bruto: ${DatabaseHelper.fmtBRL(a.bruto)}")
                        Text("(-) Agenciador: ${DatabaseHelper.fmtBRL(a.agenciador)}", color = Color(0xFFC62828))
                        Text("Valor p/ Cálculo: ${DatabaseHelper.fmtBRL(a.valorCalc)}", fontWeight = FontWeight.Bold)
                        Text("Comissão Bruta (${a.pct}%): ${DatabaseHelper.fmtBRL(a.comissaoBruta)}")
                        Text("(-) Adiantamento: ${DatabaseHelper.fmtBRL(a.adiantamento)}", color = Color(0xFFF57C00))
                        Text("COMISSÃO LÍQUIDA: ${DatabaseHelper.fmtBRL(a.comissaoBruta - a.adiantamento)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Text("TOTAL DAS COMISSÕES LÍQUIDAS: ${DatabaseHelper.fmtBRL(totalComissoes)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
            }
        }
    }
}
