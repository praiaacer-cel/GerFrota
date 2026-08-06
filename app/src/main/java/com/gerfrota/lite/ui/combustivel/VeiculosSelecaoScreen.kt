package com.gerfrota.lite.ui.combustivel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper

private val TIPOS_PERMITIDOS = setOf(
    "Cavalo Mecânico Toco", "Cavalo Mecânico Trucado",
    "Caminhão Toco", "Caminhão Truck", "Caminhão BiTruck"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeiculosSelecaoScreen(onSelecionar: (placa: String, veiculoId: Long) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var termo by remember { mutableStateOf("") }
    var veiculos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    LaunchedEffect(Unit) {
        veiculos = db.queryAll("frota", "placa ASC").filter { db.str(it["tipo_veiculo"]) in TIPOS_PERMITIDOS }
    }
    val filtrados = veiculos.filter { v ->
        termo.isBlank() || listOf("placa","marca","modelo","tipo_veiculo")
            .any { db.str(v[it]).contains(termo, true) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Selecionar Veículo") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(termo, { termo = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar por placa, marca ou modelo") },
                modifier = Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(filtrados, key = { (it["id"] as? Long) ?: 0L }) { v ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        onClick = { onSelecionar(db.str(v["placa"]), (v["id"] as? Long) ?: 0L) }) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text(db.str(v["placa"]), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${db.str(v["marca"])} ${db.str(v["modelo"])} | ${db.str(v["tipo_veiculo"])}") }
                        )
                    }
                }
            }
        }
    }
}
