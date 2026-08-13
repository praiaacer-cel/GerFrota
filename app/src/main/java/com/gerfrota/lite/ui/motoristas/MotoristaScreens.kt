// ui/MotoristaScreens.kt
package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.Motorista
import com.gerfrota.lite.ui.AzulPrimario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ❌ REMOVIDO: MotoristasListScreen duplicada.
// A versão oficial (com botões de Adiantamentos e Acerto de Contas)
// agora vive SOMENTE em MotoristasListScreen.kt.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaDetailScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var m by remember { mutableStateOf<Motorista?>(null) }

    LaunchedEffect(id) {
        withContext(Dispatchers.IO) {
            m = db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == id }
                ?.let { Motorista.fromMap(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha do Motorista") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        val mot = m
        if (mot == null) {
            Box(Modifier.padding(pad)) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(pad).padding(16.dp)) {
                Text(
                    mot.nome,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AzulPrimario,
                    fontWeight = FontWeight.Bold
                )
                Divider(Modifier.padding(vertical = 8.dp))
                listOf(
                    "CPF" to (mot.cpf ?: "-"),
                    "CNH" to "${mot.cnh ?: "-"} (cat. ${mot.categoriaCnh ?: "-"})",
                    "Venc. CNH" to (mot.vencCnh ?: "-"),
                    "Telefone" to (mot.telefone ?: "-"),
                    "WhatsApp" to (mot.whatsapp ?: "-"),
                    "Comissão" to "${mot.comissao ?: "0"}%",
                    "Endereço" to (mot.endereco ?: "-")
                ).forEach { (l, t) ->
                    Text(l, fontWeight = FontWeight.Bold)
                    Text(t)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
