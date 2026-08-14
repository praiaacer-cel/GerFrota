package com.gerfrota.lite.ui.pneus
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PneusRodizioScreen(placa: String, tipo: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var pneus by remember { mutableStateOf<Map<String, Map<String, Any?>>>(emptyMap()) }
    var origem by remember { mutableStateOf<String?>(null) }
    var destino by remember { mutableStateOf<String?>(null) }
    var dialogo by remember { mutableStateOf(false) }
    var km by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    fun carregar() = scope.launch(Dispatchers.IO) {
        pneus = db.queryAll("pneus")
            .filter { db.str(it["veiculo_id"]) == placa && db.str(it["posicao_atual"]).isNotBlank() }
            .associateBy { db.str(it["posicao_atual"]).uppercase() }
    }
    LaunchedEffect(Unit) { carregar() }

    fun tocar(pos: String) {
        val p = pos.uppercase()
        if (origem == null) origem = p
        else if (origem == p) origem = null
        else if (destino == null) { destino = p; dialogo = true }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("RODÍZIO DE PNEUS", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            Box(Modifier.fillMaxWidth().background(Color(0xFF263238)).padding(14.dp)) {
                Column {
                    Text("$placa | $tipo", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (origem == null) "1. Toque no pneu de ORIGEM" else "2. Toque no pneu de DESTINO",
                        color = if (origem == null) Color(0xFFFFCC80) else Color(0xFF90CAF9),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            LazyColumn(Modifier.padding(16.dp)) {
                items(eixosDoTipo(tipo).size) { i ->
                    val eixo = eixosDoTipo(tipo)[i]
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center) {
                        eixo.posicoes.forEachIndexed { idx, pos ->
                            val pu = pos.uppercase()
                            val pneu = pneus[pu]
                            val cor = when (pu) {
                                origem -> Color(0xFFE65100)
                                destino -> Color(0xFF1565C0)
                                else -> if (pneu != null) Color(0xFF2E7D32) else Color(0xFFBDBDBD)
                            }
                            Column(Modifier.width(54.dp).height(82.dp).padding(horizontal = 3.dp)
                                .background(cor, RoundedCornerShape(6.dp))
                                .clickable { tocar(pos) },
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(when (pu) {
                                    origem -> "ORIGEM"; destino -> "DESTINO"
                                    else -> pneu?.let { db.str(it["codigo_fogo"]) } ?: "Vazio"
                                }, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            if (idx == 1 && eixo.posicoes.size == 4) TagEixo(eixo.tag, 90)
                            if (idx == 0 && eixo.posicoes.size == 2) TagEixo(eixo.tag, 100)
                        }
                    }
                }
            }
        }
    }

    if (dialogo) {
        AlertDialog(onDismissRequest = { dialogo = false; origem = null; destino = null },
            title = { Text("Confirmar Rodízio") },
            text = {
                Column {
                    Text("De: $origem\nPara: $destino", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(km, { km = it }, label = { Text("KM do Veículo") })
                    OutlinedTextField(obs, { obs = it }, label = { Text("Observações") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val pO = db.queryAll("pneus").firstOrNull { db.str(it["veiculo_id"]) == placa && db.str(it["posicao_atual"]).equals(origem, true) }
                        val pD = db.queryAll("pneus").firstOrNull { db.str(it["veiculo_id"]) == placa && db.str(it["posicao_atual"]).equals(destino, true) }
                        if (pO != null) {
                            db.update("pneus", pO["id"] as Long, mapOf("posicao_atual" to destino))
                            if (pD != null) db.update("pneus", pD["id"] as Long, mapOf("posicao_atual" to origem))
                            db.insert("rodizios", mapOf(
                                "veiculo_id" to placa,
                                "pneu_origem_id" to pO["id"], "pneu_destino_id" to pD?.get("id"),
                                "posicao_origem" to origem, "posicao_destino" to destino,
                                "data_rodizio" to SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date()),
                                "km_rodizio" to (km.toDoubleOrNull() ?: 0.0), "observacoes" to obs))
                        }
                        dialogo = false; origem = null; destino = null; carregar()
                    }
                }) { Text("Salvar Rodízio") }
            },
            dismissButton = { TextButton(onClick = { dialogo = false; origem = null; destino = null }) { Text("Cancelar") } })
    }
}
