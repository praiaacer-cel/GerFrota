package com.gerfrota.lite.ui.viagens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViagensUnidadesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var conjuntos by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var frota by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            conjuntos = db.queryAll("unidades_transporte", "placas ASC")
            frota = db.queryAll("frota")
        }
    }

    fun veiculoPorId(id: Any?): Map<String, Any?>? =
        frota.firstOrNull { (it["id"] as? Long) == (id as? Long)?.toLong() }

    fun ehCaminhao(v: Map<String, Any?>?): Boolean {
        val t = db.str(v?.get("tipo_veiculo")).lowercase()
        return t.contains("caminhão") || t.contains("cavalo") || t.contains("truck") || t.contains("toco")
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Viagens e Fretes", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            if (conjuntos.isEmpty()) item {
                Text("Nenhum conjunto cadastrado.\nCrie um conjunto em Frota > Conjunto.", color = Color.Gray)
            }
            items(conjuntos, key = { it["id"] as Long }) { c ->
                val tracao = veiculoPorId(c["veiculo_id"])
                val titulo = if (ehCaminhao(tracao))
                    "${db.str(tracao?.get("marca"))} ${db.str(tracao?.get("modelo"))} (${db.str(tracao?.get("ano_modelo"))})".trim()
                else db.str(c["marca_modelo_ano"])
                val placas = if (ehCaminhao(tracao)) db.str(tracao?.get("placa")) else db.str(c["placas"])

                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF1976D2), modifier = Modifier.size(36.dp)) },
                        headlineContent = { Text(titulo.ifBlank { "Sem identificação" }, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        supportingContent = {
                            Column {
                                Text("Placas: $placas")
                                Text("Motorista: ${db.str(c["motorista"]).ifBlank { "Não definido" }}", fontSize = 13.sp, color = Color(0xFF607D8B))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("viagem_list/${c["id"]}") }
                    )
                }
            }
        }
    }
}
