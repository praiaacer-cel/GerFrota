@Composable
fun RelatorioRentabilidadeScreen(db: DatabaseHelper) {
    var placas by remember { mutableStateOf<List<String>>(emptyList()) }
    var placa by remember { mutableStateOf<String?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var rx by remember { mutableStateOf<RelatoriosDao.RaioX?>(null) }

    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        placas = db.queryAll("frota").map { db.str(it["placa"]) }.filter { it.isNotBlank() }
        placa = placas.firstOrNull()
    } }
    LaunchedEffect(placa, mes, ano) { if (placa != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        rx = RelatoriosDao.raioX(db, placa!!, mes, ano)
    } }

    Scaffold(topBar = { TopAppBar(title = { Text("Raio-X do Caminhão") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Placa", placa, placas) { placa = it }
            Spacer(Modifier.height(8.dp)); SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            rx?.let { r ->
                Spacer(Modifier.height(16.dp))
                Text("Resumo da Operação - ${r.qtd} Viagens", fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                CardDestaque("LUCRO LÍQUIDO (No Bolso)", DatabaseHelper.fmtBRL(r.liquido),
                    if (r.liquido >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    if (r.liquido >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario("Total Bruto\n(Fretes)", DatabaseHelper.fmtBRL(r.bruto), Color(0xFF1976D2), Modifier.weight(1f))
                    CardSecundario("Despesas\n(Pedágio, Chapa…)", DatabaseHelper.fmtBRL(r.despesas), Color(0xFFC62828), Modifier.weight(1f))
                }
                Spacer(Modifier.height(32.dp))
                Text("Dica do Gestor:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Surface(color = Color(0xFFE3F2FD), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    Text(if (r.liquido >= 0) "Caminhão operando no azul! A receita está cobrindo bem as despesas operacionais deste mês."
                         else "Atenção! O caminhão deu prejuízo neste mês. Verifique gastos excessivos com manutenção ou rotas mal pagas.",
                        Modifier.padding(16.dp), fontSize = 14.sp)
                }
            }
        }
    }
}
