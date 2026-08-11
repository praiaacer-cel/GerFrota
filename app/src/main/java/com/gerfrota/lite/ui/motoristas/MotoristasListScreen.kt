package com.gerfrota.lite.ui.motoristas

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
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.Motorista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristasListScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var motoristas by remember { mutableStateOf<List<Motorista>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun carregar() = scope.launch(Dispatchers.IO) {
        motoristas = db.queryAll("motoristas", "nome ASC").map { Motorista.fromMap(it) }
    }
    LaunchedEffect(Unit) { carregar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motoristas") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("motorista_form/-1") }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(10.dp)) {
            items(motoristas, key = { it.id ?: 0L }) { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    ListItem(
                        headlineContent = { Text(m.nome, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text("CPF: ${m.cpf ?: "-"}")
                                Text("CNH: ${m.cnh ?: "-"} (Venc: ${m.vencCnh ?: "-"})")
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2))
                        },
                        trailingContent = {
                            Row {
                                // ✅ BOTÃO ADIANTAMENTOS
                                IconButton(
                                    onClick = { nav.navigate("adiantamentos/${m.id}") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = "Adiantamentos",
                                        tint = Color(0xFF1565C0)
                                    )
                                }
                                
                                // ✅ BOTÃO ACERTO CONTAS
                                IconButton(
                                    onClick = { nav.navigate("acerto_periodo/${m.id}") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = "Acerto Contas",
                                        tint = Color(0xFF2E7D32)
                                    )
                                }
                                
                                IconButton(onClick = { nav.navigate("motorista_detail/${m.id}") }) {
                                    Icon(Icons.Default.ChevronRight, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
