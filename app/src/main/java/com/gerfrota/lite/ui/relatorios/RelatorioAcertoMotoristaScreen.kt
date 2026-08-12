@Composable
fun RelatorioAcertoMotoristaScreen(db: DatabaseHelper) {
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var sel by remember { mutableStateOf<Long?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var ac by remember { mutableStateOf<RelatoriosDao.Acerto?>(null) }

    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        motoristas = db.queryAll("motoristas", "nome ASC"); sel = (motoristas.firstOrNull()?.get("id") as? Long)
    } }
    LaunchedEffect(sel, mes, ano) { if (sel != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        ac = RelatoriosDao.acertoMotorista(db, sel!!, mes, ano)
    } }

    val nomes = motoristas.map { db.str(it["nome"]) }
    val nomeSel = motoristas.firstOrNull { (it["id"] as? Long) == sel }?.let { db.str(it["nome"]) }

    Scaffold(topBar = { TopAppBar(title = { Text("Acerto de Motorista") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Motorista", nomeSel, nomes) { n -> sel = motoristas.firstOrNull { db.str(it["nome"]) == n }?.get("id") as? Long }
            Spacer(Modifier.height(8.dp)); SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            ac?.let { a ->
                Spacer(Modifier.height(16.dp))
                Text("Extrato de Fechamento do Mês", fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                CardDestaque("SALDO A RECEBER", DatabaseHelper.fmtBRL(a.saldo),
                    if (a.saldo >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Icons.Default.AccountBalanceWallet)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSecundario("Comissões\n(Produção)", DatabaseHelper.fmtBRL(a.comissoes), Color(0xFF1976D2), Modifier.weight(1f))
                    CardSecundario("Vales / Adiant.\n(Descontos)", DatabaseHelper.fmtBRL(a.adiantamentos), Color(0xFFC62828), Modifier.weight(1f))
                }
                Spacer(Modifier.height(32.dp))
                Surface(color = Color(0xFFE3F2FD), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    Text(if (a.saldo >= 0) "A frota deve pagar ${DatabaseHelper.fmtBRL(a.saldo)} ao motorista pelas comissões do mês, já abatendo os vales."
                         else "Atenção! O motorista retirou mais vales do que gerou em comissões. Ele está devendo ${DatabaseHelper.fmtBRL(kotlin.math.abs(a.saldo))} para a frota.",
                        Modifier.padding(16.dp), fontSize = 14.sp)
                }
            }
        }
    }
}
