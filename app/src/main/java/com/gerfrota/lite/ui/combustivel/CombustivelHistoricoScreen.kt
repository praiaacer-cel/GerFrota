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
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.abastecimentosPorPlaca
import com.gerfrota.lite.services.ProntuarioService
import com.gerfrota.lite.ui.widgets.VisualizadorMidia
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombustivelHistoricoScreen(
    placa: String, veiculoId: Long, marcaModelo: String,
    onNovo: () -> Unit, onEditar: (Long) -> Unit, onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var registros by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        registros = db.abastecimentosPorPlaca("combustivel", placa)
    }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(
        topBar = { TopAppBar(
            title = { Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Histórico de Combustível", fontSize = 14.sp)
                Text(placa, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        floatingActionButton = { FloatingActionButton(onClick = onNovo, containerColor = Color(0xFFE65100)) {
            Icon(Icons.Default.Add, null) } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (registros.isEmpty()) item { Text("Nenhum abastecimento registrado.") }
            items(registros, key = { (it["id"] as? Long) ?: 0L }) { r ->
                val id = (r["id"] as? Long) ?: 0L
                val picker = rememberAnexoPicker("Abastecimentocombustivel",
                    "${r["id_abastecimento"]}_${placa.sanitized()}") { novo ->
                    scope.launch(Dispatchers.IO) {
                        db.update("combustivel", id, mapOf("path_nota" to novo)); carregar()
                    }
                }
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Data: ${r["data_registro"]}", fontWeight = FontWeight.Bold)
                            Text("ID: ${r["id_abastecimento"]}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Posto: ${db.str(r["posto"]).ifBlank { "-" }}")
                            Text("UF: ${db.str(r["uf"]).ifBlank { "-" }}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Combustível: ${db.str(r["combustivel"]).ifBlank { "-" }}")
                            Text("Litros: ${"%.2f".format(db.num(r["litros"]))} L")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("R$/L: ${DatabaseHelper.fmtBRL(db.num(r["valor_litro"]))}")
                            Text("Total: ${DatabaseHelper.fmtBRL(db.num(r["valor_total"]))}",
                                fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("KM: ${"%.1f".format(db.num(r["km_rodado"]))}", fontSize = 12.sp)
                            Text("Consumo: ${r["consumo_km_l"]?.let { "%.2f".format(db.num(it)) + " Km/L" } ?: "-"}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("Custo: ${r["custo_km"]?.let { DatabaseHelper.fmtBRL(db.num(it)) + " /Km" } ?: "-"}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        if (db.str(r["path_nota"]).isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            VisualizadorMidia(db.str(r["path_nota"]))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = picker) {
                                Icon(Icons.Default.AttachFile, null,
                                    tint = if (db.str(r["path_nota"]).isBlank()) Color(0xFF546E7A) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onEditar(id) }) {
                                Icon(Icons.Default.Edit, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.str(r["path_nota"]).takeIf { it.isNotBlank() }?.let { File(it).delete() }
                                    db.delete("combustivel", id)
                                    ProntuarioService.exportarCombustivel(ctx, db, placa, marcaModelo)
                                    carregar()
                                }
                            }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}
