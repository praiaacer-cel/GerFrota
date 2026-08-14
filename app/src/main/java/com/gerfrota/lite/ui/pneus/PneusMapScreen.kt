package com.gerfrota.lite.ui.pneus
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.core.VeiculoConstants
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.proximoCodigoPneu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class EixoLayout(val tag: String, val posicoes: List<String>)

fun eixosDoTipo(tipo: String?): List<EixoLayout> {
    val t = (tipo ?: "").lowercase()
    val diant = EixoLayout("1º EIXO - DIANTEIRO", listOf("Dianteiro Esquerdo", "Dianteiro Direito"))
    val trac1 = EixoLayout("2º EIXO - TRAÇÃO", listOf(
        "Tração 1° Eixo Esq. Fora", "Tração 1° Eixo Esq. Dentro",
        "Tração 1° Eixo Dir. Fora", "Tração 1° Eixo Dir. Dentro"))
    val truck = EixoLayout("3º EIXO - TRUCK", listOf(
        "Tração 2° Eixo Esq. Fora", "Tração 2° Eixo Esq. Dentro",
        "Tração 2° Eixo Dir. Fora", "Tração 2° Eixo Dir. Dentro"))
    val trac2 = EixoLayout("4º EIXO - TRUCK", truck.posicoes)
    return when {
        t.contains("bitruck") -> listOf(diant,
            EixoLayout("2º EIXO - BI-TRUCK", listOf("Bi-truck Dianteiro Esquerdo", "Bi-truck Dianteiro Direito")),
            EixoLayout("3º EIXO - TRAÇÃO", trac1.posicoes), trac2)
        t.contains("truck") || t.contains("trucado") -> listOf(diant, trac1, truck)
        t.contains("toco") -> listOf(diant, trac1)
        t.contains("reboque") -> {
            val n = when { t.contains("4") -> 4; t.contains("3") -> 3; t.contains("2") -> 2; else -> 1 }
            (1..n).map { i -> EixoLayout("${i}º EIXO", listOf(
                "$i° Eixo Esq. Fora", "$i° Eixo Esq. Dentro", "$i° Eixo Dir. Fora", "$i° Eixo Dir. Dentro")) }
        }
        else -> listOf(diant, trac1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PneusMapScreen(
    placa: String, tipo: String, modoSelecao: Boolean,
    onResult: (String?) -> Unit, onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var pneus by remember { mutableStateOf<Map<String, Map<String, Any?>>>(emptyMap()) }
    var editando by remember { mutableStateOf<Pair<String, Map<String, Any?>?>?>(null) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        pneus = db.queryAll("pneus")
            .filter { db.str(it["veiculo_id"]) == placa &&
                      db.str(it["posicao_atual"]).isNotBlank() &&
                      !db.str(it["status"]).equals("Descartado", true) }
            .associateBy { db.str(it["posicao_atual"]).uppercase() }
    }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (modoSelecao) "SELECIONE A POSIÇÃO" else "CADASTRO INTERATIVO DE PNEUS",
                fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            Box(Modifier.fillMaxWidth().background(Color(0xFF263238)).padding(14.dp)) {
                Text("Veículo: $placa | $tipo", color = Color.White, fontWeight = FontWeight.Bold)
            }
            LazyColumn(Modifier.padding(16.dp)) {
                items(eixosDoTipo(tipo).size) { i ->
                    val eixo = eixosDoTipo(tipo)[i]
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        if (eixo.posicoes.size == 2) {
                            PneuBotao(eixo.posicoes[0], pneus[eixo.posicoes[0].uppercase()], modoSelecao) { pos, pneu ->
                                if (modoSelecao) onResult("$pos - ${pneu?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}")
                                else editando = pos to pneu
                            }
                            TagEixo(eixo.tag, 110)
                            PneuBotao(eixo.posicoes[1], pneus[eixo.posicoes[1].uppercase()], modoSelecao) { pos, pneu ->
                                if (modoSelecao) onResult("$pos - ${pneu?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}")
                                else editando = pos to pneu
                            }
                        } else {
                            PneuBotao(eixo.posicoes[0], pneus[eixo.posicoes[0].uppercase()], modoSelecao) { pos, p -> if (modoSelecao) onResult("$pos - ${p?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}") else editando = pos to p }
                            PneuBotao(eixo.posicoes[1], pneus[eixo.posicoes[1].uppercase()], modoSelecao) { pos, p -> if (modoSelecao) onResult("$pos - ${p?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}") else editando = pos to p }
                            TagEixo(eixo.tag, 100)
                            PneuBotao(eixo.posicoes[2], pneus[eixo.posicoes[2].uppercase()], modoSelecao) { pos, p -> if (modoSelecao) onResult("$pos - ${p?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}") else editando = pos to p }
                            PneuBotao(eixo.posicoes[3], pneus[eixo.posicoes[3].uppercase()], modoSelecao) { pos, p -> if (modoSelecao) onResult("$pos - ${p?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"}") else editando = pos to p }
                        }
                    }
                }
            }
        }
    }

    editando?.let { (posicao, existente) ->
        DialogPneu(posicao, existente, placa, db,
            onDismiss = { editando = null },
            onSaved = { editando = null; carregar()
                Toast.makeText(ctx, "Pneu salvo!", Toast.LENGTH_SHORT).show() })
    }
}

@Composable
fun TagEixo(tag: String, largura: Int) {
    Box(Modifier.width(largura.dp).padding(horizontal = 6.dp)
        .background(Color(0xFF90A4AE), RoundedCornerShape(4.dp))
        .padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121),
            textAlign = TextAlign.Center)
    }
}

@Composable
fun PneuBotao(
    posicao: String, pneu: Map<String, Any?>?, modoSelecao: Boolean,
    onClick: (String, Map<String, Any?>?) -> Unit
) {
    val preenchido = pneu != null
    val fundo = if (preenchido) Color(0xFF2E7D32) else Color(0xFFBDBDBD)
    val borda = if (preenchido) Color(0xFF1B5E20) else Color(0xFF757575)
    Column(Modifier.width(54.dp).height(82.dp).padding(horizontal = 4.dp)
        .clip(RoundedCornerShape(6.dp)).background(fundo)
        .border(2.dp, borda, RoundedCornerShape(6.dp))
        .clickable { onClick(posicao, pneu) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Layers, null, tint = if (preenchido) Color(0xFFB2DFDB) else Color(0xFF616161), modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(if (preenchido) "PN" else "Vazio", color = if (preenchido) Color.White else Color(0xFF212121),
            fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(posicao.replace("DENTRO", "DT.").replace("FORA", "FR.")
            .replace("Esquerdo", "Esq.").replace("Direito", "Dir.")
            .replace("Tração", "Traç.").replace("Eixo", "Eix"),
            color = if (preenchido) Color(0xFFB2DFDB) else Color(0xFF616161),
            fontSize = 7.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun DialogPneu(
    posicao: String, existente: Map<String, Any?>?, placa: String,
    db: DatabaseHelper, onDismiss: () -> Unit, onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var codigo by remember { mutableStateOf(existente?.let { db.str(it["codigo_fogo"]) } ?: "") }
    var marca by remember { mutableStateOf(existente?.let { db.str(it["marca"]) } ?: "Michelin") }
    var modelo by remember { mutableStateOf(existente?.let { db.str(it["modelo"]) } ?: "LISO") }
    var medida by remember { mutableStateOf(existente?.let { db.str(it["medida"]) } ?: "295/80 R22.5") }
    var valor by remember { mutableStateOf(existente?.let { db.num(it["valor_compra"]).toString() } ?: "") }
    var status by remember { mutableStateOf(existente?.let { db.str(it["status"]) } ?: "Novo") }
    var kmInst by remember { mutableStateOf(existente?.let { db.num(it["km_instalacao"]).toInt().toString() } ?: "") }
    var kmAtu by remember { mutableStateOf(existente?.let { db.num(it["km_atual"]).toInt().toString() } ?: "") }

    if (codigo.isEmpty()) {
        scope.launch(Dispatchers.IO) {
            val proximoCodigo = db.proximoCodigoPneu()
            withContext(Dispatchers.Main) {
                codigo = proximoCodigo
            }
        }
    }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Pneu — Posição: $posicao", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(remember { androidx.compose.foundation.rememberScrollState() })) {
                OutlinedTextField(codigo, { codigo = it }, label = { Text("Código do Pneu") },
                    readOnly = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(marca, { marca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(modelo, { modelo = it }, label = { Text("Modelo (LISO/BORRACHUDO/MISTO)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(medida, { medida = it }, label = { Text("Medida") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(valor, { valor = it }, label = { Text("Valor de Aquisição (R$)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(status, { status = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(kmInst, { kmInst = it }, label = { Text("KM Instalação") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(kmAtu, { kmAtu = it }, label = { Text("KM Atual") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val mapa = mapOf(
                        "codigo_fogo" to codigo, "marca" to marca, "modelo" to modelo,
                        "medida" to medida, "status" to status,
                        "posicao_atual" to posicao.uppercase(), "veiculo_id" to placa,
                        "valor_compra" to (valor.toDoubleOrNull() ?: 0.0),
                        "km_instalacao" to (kmInst.toDoubleOrNull() ?: 0.0),
                        "km_atual" to (kmAtu.toDoubleOrNull() ?: 0.0),
                        "data_instalacao" to SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                    )
                    val id = existente?.get("id") as? Long
                    if (id != null) db.update("pneus", id, mapa) else db.insert("pneus", mapa)
                    onSaved()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (existente != null) {
                    TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828)),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                (existente["id"] as? Long)?.let { db.delete("pneus", it) }
                                onSaved()
                            }
                        }) { Text("Excluir") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        })
}
