  package com.gerfrota.lite.ui.relatorios

  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import com.gerfrota.lite.data.DatabaseHelper
  import com.gerfrota.lite.data.RelatoriosDao
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext

@Composable
fun RelatorioFluxoCaixaScreen(db: DatabaseHelper) {
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var f by remember { mutableStateOf<RelatoriosDao.Fluxo?>(null) }
    LaunchedEffect(mes, ano) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { f = RelatoriosDao.fluxoCaixa(db, mes, ano) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Fluxo de Caixa") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            f?.let { x ->
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    ListItem(headlineContent = { Text("Receitas (Viagens)", fontWeight = FontWeight.Bold) },
                        trailingContent = { Text(DatabaseHelper.fmtBRL(x.receitas), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) })
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                    ListItem(headlineContent = { Text("Despesas (Manut./Combust./Adiant.)", fontWeight = FontWeight.Bold) },
                        trailingContent = { Text(DatabaseHelper.fmtBRL(x.despesas), fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) })
                }
                Spacer(Modifier.height(24.dp))
                CardDestaque("Saldo do Período", DatabaseHelper.fmtBRL(x.saldo),
                    if (x.saldo >= 0) Color(0xFF0D47A1) else Color(0xFFE65100), Icons.Default.AttachMoney)
            }
        }
    }
}
