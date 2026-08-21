package com.gerfrota.lite.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.R
import com.gerfrota.lite.services.BackupService
import com.gerfrota.lite.services.DriveUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CardMenu(val titulo: String, val sub: String, val png: Int?, val vetor: ImageVector?, val rota: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gerfrota", android.content.Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val cards = listOf(
        CardMenu("CHAPA IA", "100% offline", R.drawable.ic_chapa_ia, null, "chapa"),
        CardMenu("FROTA", "Veículos e docs", R.drawable.ic_frota, null, "frota"),
        CardMenu("MOTORISTAS", "Fichas e CNH", R.drawable.ic_motorista, null, "motoristas"),
        CardMenu("VIAGENS E FRETES", "Cargas e valores", R.drawable.ic_viagens, null, "viagens"),
        CardMenu("MANUTENÇÃO", "Serviços e pneus", R.drawable.ic_manutencao, null, "manutencao"),
        CardMenu("COMBUSTÍVEL", "Diesel e ARLA", R.drawable.ic_combustivel, null, "combustivel_selecao"),
        CardMenu("RELATÓRIOS", "Gestor", R.drawable.ic_relatorios, null, "relatorios"),
        CardMenu("BACKUP", "Gerar e enviar p/ nuvem", null, Icons.Default.CloudUpload, null),
    )
    Scaffold(topBar = {
        TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Image(painter = painterResource(R.drawable.ic_mapa), contentDescription = null, modifier = Modifier.size(34.dp)); Spacer(Modifier.width(10.dp)); Text("GerFrotaLite", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AzulPrimario) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White))
    }) { pad ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(pad).fillMaxSize().background(Fundo), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cards, key = { it.titulo }) { card ->
                Card(modifier = Modifier.height(150.dp).clip(RoundedCornerShape(16.dp)).clickable {
                    if (card.rota != null) navigate(card.rota) else { scope.launch(Dispatchers.IO) { val zip = BackupService.criarBackupCompactado(context); val token = prefs.getString("conta_token", null); val msg = if (zip != null && token != null) { DriveUploader.upload(zip, token); "Backup gerado e enviado para a nuvem!" } else if (zip == null) "Falha ao gerar backup local." else "Token da conta não encontrado."; withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } } }
                }, colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (card.png != null) Image(painter = painterResource(card.png), contentDescription = card.titulo, modifier = Modifier.size(64.dp))
                        else Icon(imageVector = card.vetorSafe(), contentDescription = card.titulo, tint = AzulCard, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(10.dp)); Text(card.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, color = androidx.compose.ui.graphics.Color(0xFF212121))
                        Text(card.sub, fontSize = 10.sp, textAlign = TextAlign.Center, color = androidx.compose.ui.graphics.Color(0xFF757575))
                    }
                }
            }
        }
    }
}
private fun CardMenu.vetorSafe(): ImageVector = vetor ?: Icons.Default.CloudUpload
