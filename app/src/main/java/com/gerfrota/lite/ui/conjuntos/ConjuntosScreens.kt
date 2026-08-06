package com.gerfrota.lite.ui.conjuntos

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import com.gerfrota.lite.core.VeiculoConstants
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConjuntosListScreen(onNovo: () -> Unit, onEditar: (Long) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var conjuntos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            conjuntos = db.queryAll("unidades_transporte", "placas ASC")
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Conjuntos", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = {
                Button(onClick = onNovo, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Text("Novo Conjunto", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (conjuntos.isEmpty()) item { Text("Nenhum conjunto configurado.", color = Color.Gray) }
            items(conjuntos, key = { it["id"] as Long }) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF1976D2), modifier = Modifier.size(36.dp)) },
                        headlineContent = { Text(db.str(c["marca_modelo_ano"]).ifBlank { "Sem identificação" }, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        supportingContent = {
                            Column {
                                Text("Placas: ${db.str(c["placas"])}")
                                Text("Motorista: ${db.str(c["motorista"]).ifBlank { "Não definido" }}", fontSize = 13.sp, color = Color(0xFF607D8B))
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEditar(c["id"] as Long) }) { Icon(Icons.Default.Edit, null, tint = Color(0xFFF57C00)) }
                                IconButton(onClick = {
                                    scope.launch(Dispatchers.IO) { db.delete("unidades_transporte", c["id"] as Long) }
                                }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828)) }
                            }
                        })
                }
            }
        }
    }
}

enum class Slot { TRACAO, REB1, REB2, REB3 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConjuntoFormScreen(conjuntoId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var frota by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var tracao by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var reb1 by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var reb2 by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var reb3 by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var motorista by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var picker by remember { mutableStateOf<Slot?>(null) }
    
    // ✅ CORREÇÃO: Estado movido para dentro do Composable
    var showMotoristaPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            frota = db.queryAll("frota", "placa ASC")
            motoristas = db.queryAll("motoristas", "nome ASC")
            if (conjuntoId >= 0) {
                val c = db.queryAll("unidades_transporte").firstOrNull { it["id"] == conjuntoId }
                if (c != null) {
                    tracao = frota.firstOrNull { it["id"] == c["veiculo_id"] }
                    reb1 = frota.firstOrNull { it["id"] == c["reboque1_id"] }
                    reb2 = frota.firstOrNull { it["id"] == c["reboque2_id"] }
                    reb3 = frota.firstOrNull { it["id"] == c["reboque3_id"] }
                    motorista = motoristas.firstOrNull { it["id"] == c["motorista_id"] }
                }
            }
        }
    }

    fun usados(): List<Long?> = listOf(tracao, reb1, reb2, reb3).map { it?.get("id") as? Long }

    fun candidatos(slot: Slot): List<Map<String, Any?>> = frota.filter { v ->
        val id = v["id"] as? Long
        val ehTr = VeiculoConstants.ehTracao(db.str(v["tipo_veiculo"]))
        val tipoOk = if (slot == Slot.TRACAO) ehTr else !ehTr
        val naoUsado = !usados().contains(id) ||
            (slot == Slot.TRACAO && tracao?.get("id") == id) ||
            (slot == Slot.REB1 && reb1?.get("id") == id) ||
            (slot == Slot.REB2 && reb2?.get("id") == id) ||
            (slot == Slot.REB3 && reb3?.get("id") == id)
        tipoOk && naoUsado
    }

    fun salvar() = scope.launch(Dispatchers.IO) {
        if (tracao == null) {
            Toast.makeText(ctx, "Selecione ao menos a Tração.", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val placas = listOfNotNull(tracao, reb1, reb2, reb3).joinToString(" / ") { db.str(it["placa"]) }
        val t = tracao!!
        val dados = mapOf(
            "veiculo_id" to t["id"], "reboque1_id" to reb1?.get("id"),
            "reboque2_id" to reb2?.get("id"), "reboque3_id" to reb3?.get("id"),
            "motorista_id" to motorista?.get("id"),
            "marca_modelo_ano" to "${db.str(t["marca"])} ${db.str(t["modelo"])} (${db.str(t["ano_modelo"])})".trim(),
            "modelo" to (reb1?.let { "${db.str(it["marca"])} ${db.str(it["modelo"])}" } ?: "Nenhum"),
            "placas" to placas,
            "motorista" to (motorista?.let { db.str(it["nome"]) } ?: "Não definido"))
        if (conjuntoId >= 0) db.update("unidades_transporte", conjuntoId, dados)
        else db.insert("unidades_transporte", dados)
        onBack()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (conjuntoId >= 0) "Editar Conjunto" else "Novo Conjunto", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Monte a composição acoplando a tração aos semi reboques.",
                fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            SlotCard("Cavalo Mecânico / Caminhão", tracao, db) { picker = Slot.TRACAO }
            Spacer(Modifier.height(12.dp))
            SlotCard("Semi Reboque 1", reb1, db) { picker = Slot.REB1 }
            Spacer(Modifier.height(12.dp))
            SlotCard("Semi Reboque 2 (Opcional)", reb2, db) { picker = Slot.REB2 }
            Spacer(Modifier.height(12.dp))
            SlotCard("Semi Reboque 3 (Opcional)", reb3, db) { picker = Slot.REB3 }
            Spacer(Modifier.height(16.dp))
            SlotCard("Motorista Vinculado", motorista, db, ehMotorista = true) {
                // picker de motorista: reutiliza bottom sheet simples
                picker = null
                // abre diálogo de motorista
                showMotoristaPicker = true
            }
            Spacer(Modifier.height(32.dp))
            Row {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp)) { Text("CANCELAR", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { salvar() }, modifier = Modifier.weight(2f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))) {
                    Text("SALVAR CONJUNTO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Bottom sheet de veículos
    picker?.let { slot ->
        ModalBottomSheet(onDismissRequest = { picker = null }) {
            Column(Modifier.padding(16.dp)) {
                Text("Selecionar ${slot.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                LazyColumn {
                    items(candidatos(slot)) { v ->
                        ListItem(
                            leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF1976D2)) },
                            headlineContent = { Text("${db.str(v["placa"])} - ${db.str(v["marca"])} ${db.str(v["modelo"])}") },
                            supportingContent = { Text(db.str(v["tipo_veiculo"])) },
                            modifier = Modifier.clickable {
                                when (slot) {
                                    Slot.TRACAO -> tracao = v; Slot.REB1 -> reb1 = v
                                    Slot.REB2 -> reb2 = v; Slot.REB3 -> reb3 = v
                                }
                                picker = null
                            })
                    }
                }
            }
        }
    }

    // Diálogo de motoristas
    if (showMotoristaPicker) {
        AlertDialog(onDismissRequest = { showMotoristaPicker = false },
            title = { Text("Selecionar Motorista") },
            text = {
                LazyColumn {
                    items(motoristas) { m ->
                        ListItem(headlineContent = { Text(db.str(m["nome"])) },
                            supportingContent = { Text("CPF: ${db.str(m["cpf"]).ifBlank { "—" }}") },
                            modifier = Modifier.clickable { motorista = m; showMotoristaPicker = false })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMotoristaPicker = false }) { Text("Fechar") } })
    }
}

// ❌ REMOVIDO: A declaração global abaixo foi deletada, pois o estado agora pertence ao ConjuntoFormScreen
// var showMotoristaPicker by mutableStateOf(false)

@Composable
fun SlotCard(label: String, veiculo: Map<String, Any?>?, db: DatabaseHelper,
               ehMotorista: Boolean = false, onClick: () -> Unit) {
    val preenchido = veiculo != null
    Card(colors = CardDefaults.cardColors(
        containerColor = if (preenchido) (if (ehMotorista) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)) else Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp)) {
            Icon(if (ehMotorista) Icons.Default.Person else Icons.Default.LocalShipping, null,
                tint = if (preenchido) (if (ehMotorista) Color(0xFF2E7D32) else Color(0xFF1565C0)) else Color(0xFF9E9E9E),
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, color = Color(0xFF616161))
                Text(
                    if (preenchido) {
                        if (ehMotorista) db.str(veiculo["nome"])
                        else "${db.str(veiculo["placa"])} - ${db.str(veiculo["marca"])} ${db.str(veiculo["modelo"])}"
                    } else "Toque para selecionar",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (preenchido) Color(0xFF212121) else Color(0xFF9E9E9E))
            }
            Icon(if (preenchido) Icons.Default.CheckCircle else Icons.Default.ArrowForwardIos, null,
                tint = if (preenchido) Color(0xFF2E7D32) else Color(0xFFBDBDBD))
        }
    }
}
