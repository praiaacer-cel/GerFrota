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
                    // ✅ BOTÃO ADICIONADO: Navega para Conjuntos
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
                    // ✅ BOTÃO ADICIONADO: Navega para Gestão de Pneus
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

@Composable
fun FrotaFormScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var placa by remember { mutableStateOf("") }; var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }; var tipo by remember { mutableStateOf("") }
    var anoMod by remember { mutableStateOf("") }; var antt by remember { mutableStateOf("") }

    LaunchedEffect(id) { 
        withContext(Dispatchers.IO) {
            db.queryAll("frota").firstOrNull { (it["id"] as? Long) == id }?.let {
                placa = db.str(it["placa"]); marca = db.str(it["marca"]); modelo = db.str(it["modelo"])
                tipo = db.str(it["tipo_veiculo"]); anoMod = db.str(it["ano_modelo"]); antt = db.str(it["antt"])
            }
        } 
    }

    val tipos = listOf(
        "Cavalo Mecânico Toco", "Cavalo Mecânico Trucado", "Caminhão Toco",
        "Caminhão Truck", "Caminhão BiTruck", "Semi-Reboque 1 eixo", "Semi-Reboque 2 eixos",
        "Semi-Reboque 3 eixos", "Semi-Reboque 4 eixos"
    )

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(if (id == -1L) "Novo Veículo" else "Editar Veículo") },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Default.ArrowBack, null) 
                    } 
                }
            ) 
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            OutlinedTextField(placa, { placa = it.uppercase().take(7) }, label = { Text("Placa *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(marca, { marca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(modelo, { modelo = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
            var exp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                OutlinedTextField(tipo, {}, label = { Text("Tipo de Veículo") }, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
                ExposedDropdownMenu(exp, { exp = false }) {
                    tipos.forEach { t -> DropdownMenuItem({ Text(t) }, { tipo = t }) }
                }
            }
            OutlinedTextField(anoMod, { anoMod = it }, label = { Text("Ano Modelo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(antt, { antt = it }, label = { Text("ANTT") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val row = mapOf(
                        "placa" to placa, "marca" to marca, "modelo" to modelo,
                        "tipo_veiculo" to tipo, "ano_modelo" to anoMod, "antt" to antt
                    )
                    if (id == -1L) db.insert("frota", row) else db.update("frota", id, row)
                    withContext(Dispatchers.Main) { onBack() }
                }
            }, modifier = Modifier.fillMaxWidth()) { 
                Text("SALVAR VEÍCULO") 
            }
        }
    }
}
