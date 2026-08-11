package com.gerfrota.lite.ui.manutencao

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.services.PdfService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@Composable
fun ManutencaoListScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var veiculos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            veiculos = db.queryAll("frota", "placa ASC")
        }
    }
    
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("MANUTENÇÃO DOS VEÍCULOS") },
                navigationIcon = { 
                    IconButton(onClick = { nav.popBackStack() }) { 
                        Icon(Icons.Default.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            items(veiculos) { v ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable {
                            val placa = db.str(v["placa"])
                            val tipo = URLEncoder.encode(db.str(v["tipo_veiculo"]) ?: "", "UTF-8")
                            nav.navigate("manutencao_detail/$placa/$tipo")
                        }
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(db.str(v["placa"]), fontWeight = FontWeight.Bold, fontSize = 16.sp) 
                        },
                        supportingContent = { 
                            Text("${db.str(v["marca"])} ${db.str(v["modelo"])}") 
                        },
                        leadingContent = { 
                            Icon(Icons.Default.Build, null, tint = Color(0xFF1976D2)) 
                        },
                        trailingContent = { 
                            Icon(Icons.Default.ChevronRight, null) 
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun ManutencaoDetailScreen(placa: String, tipo: String, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var historico by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        historico = db.queryAll("manutencoes", "id DESC")
            .filter { db.str(it["placa_veiculo"]).trim().equals(placa.trim(), true) }
    }
    LaunchedEffect(Unit) { carregar() }

    fun excluir(m: Map<String, Any?>) = scope.launch(Dispatchers.IO) {
        val id = (m["id"] as? Long) ?: return@launch
        db.str(m["caminho_nota_arquivo"]).let { if (it.isNotEmpty()) File(it).delete() }
        db.delete("manutencoes", id)
        // remove o bloco da OS no prontuário
        val f = File(File(db.baseDir(), "ProntuarioPlaca"), "${placa}_prontuario.txt")
        if (f.exists()) {
            val os = id.toString().padStart(5, '0')
            val blocos = f.readText().split("-".repeat(60) + "\n")
                .map { it.trim() }.filter { it.isNotEmpty() && !it.contains("OS: $os") }
            if (blocos.isEmpty()) f.delete()
            else f.writeText(blocos.joinToString("-".repeat(60) + "\n") { it + "\n" } + "-".repeat(60) + "\n")
        }
        carregar()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("HISTÓRICO DE SERVIÇOS") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            actions = {
                Button(onClick = { nav.navigate("pneus_gestao/$placa/${URLEncoder.encode(tipo, "UTF-8")}") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("PNEUS") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = { nav.navigate("manutencao_form/$placa/${URLEncoder.encode(tipo, "UTF-8")}/-1/-1") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("MANUTENÇÃO") }
                Spacer(Modifier.width(8.dp))
            })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(10.dp)) {
            items(historico) { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${db.str(m["data_servico"])} - ${db.str(m["sistema"])}",
                                color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("KM: ${db.num(m["quilometragem"]).toInt()}", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text("Subsistema: ${db.str(m["subsistema"])}", fontSize = 13.sp)
                        Text("Serviço: ${db.str(m["tipo_servico"]).ifBlank { "Não informado" }}", fontSize = 13.sp)
                        Text("Observação: ${db.str(m["observacao"]).ifBlank { "Nenhuma" }}", fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Custo: ${db.str(m["valor_servico"])}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { excluir(m) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val f = PdfService.gerarCardA6(ctx, m, placa)
                                        PdfService.compartilhar(ctx, f)
                                    }
                                }) { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp)) }
                                IconButton(onClick = {
                                    scope.launch(Dispatchers.IO) { PdfService.gerarCardA6(ctx, m, placa) }
                                }) { Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp)) }
                                TextButton(onClick = {
                                    nav.navigate("manutencao_form/$placa/${URLEncoder.encode(tipo, "UTF-8")}/${m["id"]}/-1")
                                }) { Text("Alterar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
