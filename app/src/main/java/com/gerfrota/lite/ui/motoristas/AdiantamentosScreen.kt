package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdiantamentosScreen(motoristaId: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var data by remember { mutableStateOf("") }; var valor by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<Long?>(null) }

    fun carregar() = scope.launch(Dispatchers.IO) {
        lista = db.queryAll("adiantamentos").filter { (it["motorista_id"] as? Long) == motoristaId }
    }
    LaunchedEffect(Unit) { carregar() }

    val total = lista.sumOf { db.num(it["valor"]) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Adiantamentos", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(data, { data = it }, label = { Text("Data") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(valor, { valor = it }, label = { Text("Valor (R$)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val v = DatabaseHelper.parseMoney(valor)
                            if (v > 0 && data.isNotBlank()) {
                                if (editando != null) db.update("adiantamentos", editando!!, mapOf("data" to data, "valor" to v))
                                else db.insert("adiantamentos", mapOf("motorista_id" to motoristaId, "data" to data, "valor" to v))
                                data = ""; valor = ""; editando = null; carregar()
                            }
                        }
                    }) { Text(if (editando == null) "ADICIONAR" else "SALVAR ALTERAÇÕES") }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(lista, key = { it["id"] as Long }) { a ->
                    ListItem(
                        headlineContent = { Text(db.str(a["data"]), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(DatabaseHelper.fmtBRL(db.num(a["valor"]))) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editando = a["id"] as Long; data = db.str(a["data"]); valor = db.num(a["valor"]).toString() }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                                IconButton(onClick = { scope.launch(Dispatchers.IO) { db.delete("adiantamentos", a["id"] as Long); carregar() } }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp)) }
                            }
                        })
                }
            }
            Text("TOTAL ADIANTADO: ${DatabaseHelper.fmtBRL(total)}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
        }
    }
}
