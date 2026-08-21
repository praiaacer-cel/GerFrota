package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioFluxoCaixaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var f by remember { mutableStateOf<RelatoriosDao.Fluxo?>(null) }
    LaunchedEffect(mes, ano) { scope.launch(Dispatchers.IO) { f = RelatoriosDao.fluxoCaixa(db, mes, ano) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Fluxo de Caixa") }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            f?.let { x ->
                Spacer(Modifier.height(14.dp))
                CardSecundario("Receitas (Viagens)", DatabaseHelper.fmtBRL(x.receitas), Color(0xFF2E7D32))
                Spacer(Modifier.height(10.dp))
                CardSecundario("Despesas (Manut./Comb./Adiant.)", DatabaseHelper.fmtBRL(x.despesas), Color(0xFFC62828))
                Spacer(Modifier.height(14.dp))
                CardDestaque("SALDO DO PERÍODO", DatabaseHelper.fmtBRL(x.saldo), if (x.saldo >= 0) Color(0xFF1976D2) else Color(0xFFB26A00), Icons.Default.AttachMoney)
            }
        }
    }
}
