package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import com.gerfrota.lite.ui.relatorios.CardDestaque
import com.gerfrota.lite.ui.relatorios.CardSecundario
import com.gerfrota.lite.ui.relatorios.SeletorMesAno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcertoPeriodoScreen(motoristaId: Long, nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    var nome by remember { mutableStateOf("") }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var ac by remember { mutableStateOf<RelatoriosDao.Acerto?>(null) }
    LaunchedEffect(motoristaId) {
        withContext(Dispatchers.IO) {
            db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == motoristaId }?.let { nome = db.str(it["nome"]) }
        }
    }
    LaunchedEffect(motoristaId, mes, ano) {
        withContext(Dispatchers.IO) { ac = RelatoriosDao.acertoMotorista(db, motoristaId, mes, ano) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Acerto: $nome") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            ac?.let { a ->
                Spacer(Modifier.height(16.dp))
                CardDestaque("SALDO DO ACERTO", DatabaseHelper.fmtBRL(a.saldo),
                    if (a.saldo >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Icons.Default.AccountBalanceWallet)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario("Comissões", DatabaseHelper.fmtBRL(a.comissoes), Color(0xFF1976D2), Modifier.weight(1f))
                    CardSecundario("Adiantamentos", DatabaseHelper.fmtBRL(a.adiantamentos), Color(0xFFC62828), Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { nav.navigate("acerto_contas/$motoristaId/0/0") }, modifier = Modifier.fillMaxWidth()) {
                    Text("FECHAR ACERTO DO PERÍODO")
                }
            }
        }
    }
}
