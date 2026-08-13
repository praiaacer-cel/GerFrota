// ui/FrotaScreens.kt
package com.gerfrota.lite.ui.frota

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
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.Veiculo
import com.gerfrota.lite.ui.AzulCard
import com.gerfrota.lite.ui.AzulPrimario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Composable
fun FrotaListScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var veiculos by remember { mutableStateOf<List<Veiculo>>(emptyList()) }
    val scope = rememberCoroutineScope()
    fun carregar() = scope.launch(Dispatchers.IO) {
        veiculos = db.queryAll("frota", "placa ASC").map { Veiculo.fromMap(it) }
    }
    LaunchedEffect(Unit) { carregar() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frota de Veículos") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // ✅ BOTÃO: Navega para Conjuntos
                    Button(
                        onClick = { nav.navigate("conjuntos") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Icon(Icons.Default.LocalShipping, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Conjunto", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("frota_form/-1") }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(10.dp)) {
            items(veiculos, key = { it.id ?: 0L }) { v ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(v.placa, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${v.marca ?: ""} ${v.modelo ?: ""} • ${v.tipoVeiculo ?: "-"}") },
                        leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = AzulCard) },
                        trailingContent = {
                            IconButton(onClick = { nav.navigate("frota_detail/${v.id}") }) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun FrotaDetailScreen(id: Long, nav: NavController, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var v by remember { mutableStateOf<Veiculo?>(null) }
    LaunchedEffect(id) {
        withContext(Dispatchers.IO) {
            v = db.queryAll("frota").firstOrNull { (it["id"] as? Long) == id }?.let { Veiculo.fromMap(it) }
        }
    }
    val veic = v
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha do Veículo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // ✅ BOTÃO: Navega para Gestão de Pneus
                    veic?.let {
                        Button(
                            onClick = {
                                nav.navigate("pneus_gestao/${veic.placa}/${URLEncoder.encode(veic.tipoVeiculo ?: "", "UTF-8")}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text("PNEUS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (veic == null) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            }
        } else {
            LazyColumn(Modifier.padding(pad).padding(16.dp)) {
                item {
                    Text(veic.placa, style = MaterialTheme.typography.headlineMedium, color = AzulPrimario, fontWeight = FontWeight.Bold)
                    Divider(Modifier.padding(vertical = 8.dp))
                    listOf(
                        "Marca/Modelo" to "${veic.marca ?: "-"} / ${veic.modelo ?: "-"}",
                        "Tipo" to (veic.tipoVeiculo ?: "-"),
                        "Carroceria" to (veic.carroceria ?: "-"),
                        "Ano Fab/Modelo" to "${veic.anoFabricacao ?: "-"} / ${veic.anoModelo ?: "-"}",
                        "Renavam" to (veic.renavam ?: "-"),
                        "Chassi" to (veic.chassi ?: "-"),
                        "Venc. Licenciamento" to (veic.vencLicenciamento ?: "-"),
                        "ANTT" to (veic.antt ?: "-"),
                        "Venc. ANTT" to (veic.vencAntt ?: "-"),
                        "Qtd. Pneus" to (veic.qtdPneus ?: "-"),
                        "Observações" to (veic.observacao ?: "-")
                    ).forEach { (l, t) ->
                        Text(l, fontWeight = FontWeight.Bold)
                        Text(t, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ❌ REMOVIDO: o FrotaFormScreen SIMPLES que existia aqui.
// A única definição agora está em FrotaFormScreen.kt (versão completa com anexos).
