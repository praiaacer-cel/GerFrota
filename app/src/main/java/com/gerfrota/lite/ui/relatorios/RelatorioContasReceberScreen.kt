@Composable
fun RelatorioContasReceberScreen(db: DatabaseHelper) {
    var contas by remember { mutableStateOf<List<RelatoriosDao.ContaReceber>>(emptyList()) }
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { contas = RelatoriosDao.contasReceber(db) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Contas a Receber (Pendentes)") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp)) {
            if (contas.isEmpty()) item { Text("Nenhuma conta pendente encontrada no sistema.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            items(contas.size) { i ->
                val c = contas[i]
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    ListItem(
                        headlineContent = { Text("Empresa: ${c.empresa.ifBlank { "Não informada" }}", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Viagem: ${c.nro}\nData Carga: ${c.dataCarga}") },
                        trailingContent = { Text(DatabaseHelper.fmtBRL(c.valor), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp) })
                }
            }
        }
    }
}
