package com.gerfrota.lite.ui.pneus
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gerfrota.lite.data.normalizarPosicao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PneusServicosScreen(placa: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var servicos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var pneusVeiculo by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var modo by remember { mutableStateOf("Todos") }
    var pneuSel by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var expMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            servicos = db.queryAll("manutencoes").filter {
                db.str(it["placa_veiculo"]).trim() == placa &&
                db.str(it["sistema"]).lowercase().contains("pneu")
            }
            pneusVeiculo = db.queryAll("pneus").filter {
                db.str(it["veiculo_id"]) == placa && !db.str(it["status"]).equals("Descartado", true)
            }
        }
    }

    val filtrados = remember(servicos, modo, pneuSel) {
        if (modo == "Todos" || pneuSel == null) servicos
        else {
            val cod = normalizarPosicao(db.str(pneuSel!!["codigo_fogo"]))
            val pos = normalizarPosicao(db.str(pneuSel!!["posicao_atual"]))
            servicos.filter { s ->
                val sub = normalizarPosicao(db.str(s["subsistema"]))
                val tip = normalizarPosicao(db.str(s["tipo_servico"]))
                val obs = normalizarPosicao(db.str(s["observacao"]))
                (pos.isNotBlank() && sub.contains(pos)) ||
                (cod.isNotBlank() && (sub.contains(cod) || tip.contains(cod) || obs.contains(cod)))
            }
        }
    }
    val total = filtrados.sumOf { db.num(it["valor_servico"]) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("SERVIÇOS DE PNEUS", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                FilterChip(selected = modo == "Todos", onClick = { modo = "Todos" },
                    label = { Text("Todos do Veículo") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = modo == "Por Pneu", onClick = { modo = "Por Pneu" },
                    label = { Text("Filtrar por Pneu") })
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total R$", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Text(DatabaseHelper.fmtBRL(total), color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            if (modo == "Por Pneu") {
                ExposedDropdownMenuBox(expanded = expMenu, onExpandedChange = { expMenu = it },
                    modifier = Modifier.padding(horizontal = 12.dp)) {
                    OutlinedTextField(
                        value = pneuSel?.let { "${db.str(it["codigo_fogo"])} — ${db.str(it["posicao_atual"])}" } ?: "Escolha o Pneu",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expMenu) })
                    ExposedDropdownMenu(expanded = expMenu, onDismissRequest = { expMenu = false }) {
                        pneusVeiculo.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${db.str(p["codigo_fogo"])} — ${db.str(p["posicao_atual"])}") },
                                onClick = { pneuSel = p; expMenu = false })
                        }
                    }
                }
            }
            LazyColumn(Modifier.padding(12.dp)) {
                if (filtrados.isEmpty()) item { Text("Nenhum serviço de pneu encontrado.", color = Color.Gray) }
                items(filtrados) { s ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(db.str(s["subsistema"]).uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0D47A1))
                                Text(db.str(s["data_servico"]), fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("Serviço: ${db.str(s["tipo_servico"]).ifBlank { "Não especificado" }}", fontWeight = FontWeight.W600)
                            Text("KM: ${db.num(s["quilometragem"]).toInt()} | Prestador: ${db.str(s["prestador"]).ifBlank { "-" }}")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NF: ${db.str(s["numero_nota"]).ifBlank { "-" }}")
                                Text(DatabaseHelper.fmtBRL(db.num(s["valor_servico"])),
                                    color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
