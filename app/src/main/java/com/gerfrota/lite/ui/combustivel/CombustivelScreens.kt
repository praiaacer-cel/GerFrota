// ui/CombustivelScreens.kt
package com.gerfrota.lite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombustivelScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var placaSel by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) {
        rows = db.queryAll("combustivel", "id DESC")
        if (placaSel == null) placaSel = rows.firstOrNull()?.get("placa_principal")?.toString()
    } }

    val placas = rows.map { db.str(it["placa_principal"]) }.distinct()
    val filtrados = rows.filter { db.str(it["placa_principal"]) == placaSel }
    val totalL = filtrados.sumOf { db.num(it["litros"]) }
    val totalV = filtrados.sumOf { db.num(it["valor_total"]) }
    val totalKm = filtrados.sumOf { db.num(it["km_rodado"]) }

    Scaffold(topBar = { TopAppBar(title = { Text("Combustível / ARLA 32") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            var exp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                OutlinedTextField(placaSel ?: "Placa", {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
                ExposedDropdownMenu(exp, { exp = false }) {
                    placas.forEach { p -> DropdownMenuItem({ Text(p) }, { placaSel = p }) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("KM: ${"%.0f".format(totalKm)}", fontWeight = FontWeight.Bold)
                Text("Litros: ${"%.0f".format(totalL)}", fontWeight = FontWeight.Bold)
                Text(DatabaseHelper.fmtBRL(totalV), fontWeight = FontWeight.Bold, color = VermelhoAlerta)
                Text(if (totalL > 0) "${"%.2f".format(totalKm / totalL)} km/L" else "-",
                    fontWeight = FontWeight.Bold, color = VerdeSucesso)
            }
            LazyColumn {
                items(filtrados) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row { Text("${c["data_registro"]} • ${c["posto"] ?: "-"}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(DatabaseHelper.fmtBRL(db.num(c["valor_total"])), color = VermelhoAlerta, fontWeight = FontWeight.Bold) }
                            Text("${"%.0f".format(db.num(c["litros"]))} L • ${"%.0f".format(db.num(c["km_rodado"]))} km • " +
                                "Consumo: ${c["consumo_km_l"]?.let { "%.2f".format(db.num(it)) } ?: "-"} km/L", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
