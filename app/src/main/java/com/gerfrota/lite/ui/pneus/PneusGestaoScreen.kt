package com.gerfrota.lite.ui.pneus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PneusGestaoScreen(
    placa: String, tipo: String, 
    onMap: () -> Unit, onServicos: () -> Unit, onRodizio: () -> Unit, onEstoque: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    
    // Busca a marca e modelo dinamicamente através da placa
    val veiculo = remember { db.queryAll("frota").firstOrNull { db.str(it["placa"]) == placa } }
    val marca = db.str(veiculo?.get("marca"))
    val modelo = db.str(veiculo?.get("modelo"))

    Scaffold(topBar = {
        TopAppBar(title = { Text("GESTÃO DE PNEUS", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("$placa - $marca - $modelo", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(tipo, color = Color(0xFFB0BEC5), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
            BotaoGestao("Cadastrar / Editar Pneus", Icons.Default.AddCircle, onMap)
            Spacer(Modifier.height(20.dp))
            BotaoGestao("Serviços dos Pneus", Icons.Default.BuildCircle, onServicos)
            Spacer(Modifier.height(20.dp))
            BotaoGestao("Rodízio de Pneus", Icons.Default.SwapHoriz, onRodizio)
            Spacer(Modifier.height(20.dp))
            BotaoGestao("Estoque de Pneus", Icons.Default.Inventory2, onEstoque)
        }
    }
}

@Composable
private fun BotaoGestao(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
        shape = RoundedCornerShape(12.dp)) {
        Icon(icon, null, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
