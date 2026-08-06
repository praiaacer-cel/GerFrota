// ui/MotoristaScreens.kt
package com.gerfrota.lite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.Motorista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MotoristasListScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var lista by remember { mutableStateOf<List<Motorista>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        lista = db.queryAll("motoristas", "nome ASC").map { Motorista.fromMap(it) }
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Motoristas") },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(10.dp)) {
            items(lista, key = { it.id ?: 0L }) { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(m.nome, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("CNH: ${m.cnh ?: "-"} • Tel: ${m.whatsapp ?: m.telefone ?: "-"}") },
                        leadingContent = { Icon(Icons.Default.Person, null, tint = LaranjaAccent) },
                        trailingContent = { IconButton(onClick = { nav.navigate("motorista_detail/${m.id}") }) {
                            Icon(Icons.Default.ChevronRight, null) } })
                }
            }
        }
    }
}

@Composable
fun MotoristaDetailScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var m by remember { mutableStateOf<Motorista?>(null) }
    LaunchedEffect(id) { withContext(Dispatchers.IO) {
        m = db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == id }?.let { Motorista.fromMap(it) }
    } }
    Scaffold(topBar = { TopAppBar(title = { Text("Ficha do Motorista") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        val mot = m
        if (mot == null) Box(Modifier.padding(pad)) { CircularProgressIndicator() }
        else Column(Modifier.padding(pad).padding(16.dp)) {
            Text(mot.nome, style = MaterialTheme.typography.headlineSmall, color = AzulPrimario, fontWeight = FontWeight.Bold)
            Divider(Modifier.padding(vertical = 8.dp))
            listOf("CPF" to (mot.cpf ?: "-"), "CNH" to "${mot.cnh ?: "-"} (cat. ${mot.categoriaCnh ?: "-"})",
                "Venc. CNH" to (mot.vencCnh ?: "-"), "Telefone" to (mot.telefone ?: "-"),
                "WhatsApp" to (mot.whatsapp ?: "-"), "Comissão" to "${mot.comissao ?: "0"}%",
                "Endereço" to (mot.endereco ?: "-")).forEach { (l, t) ->
                Text(l, fontWeight = FontWeight.Bold); Text(t); Spacer(Modifier.height(6.dp))
            }
        }
    }
}
