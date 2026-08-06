@Composable
fun RelatorioAcertoMotoristaScreen() {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var sel by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var ac by remember { mutableStateOf<RelatoriosDao.AcertoMes?>(null) }
    var exp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        motoristas = db.queryAll("motoristas"); sel = motoristas.firstOrNull()
    } }
    LaunchedEffect(sel, mes, ano) { sel?.let { m -> scope.launch(Dispatchers.IO) {
        ac = RelatoriosDao.acertoMotorista(db, m["id"] as Long, db.str(m["nome"]),
            DatabaseHelper.parseMoney(db.str(m["comissao"])), mes, ano)
    } } }

    Scaffold(topBar = { TopAppBar(title = { Text("Acerto de Motorista") }) }) { pad ->
        Column(Modifier.padding(pad).padding(14.dp)) {
            ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                OutlinedTextField(sel?.let { db.str(it["nome"]) } ?: "Motorista", {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) })
                ExposedDropdownMenu(exp, { exp = false }) {
                    motoristas.forEach { m -> DropdownMenuItem({ Text(db.str(m["nome"])) }, { sel = m }) }
                }
            }
            Spacer(Modifier.height(8.dp)); SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            ac?.let { a ->
                Spacer(Modifier.height(14.dp))
                CardDestaque("SALDO A RECEBER", DatabaseHelper.fmtBRL(a.saldo),
                    if (a.saldo >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Icons.Default.AccountBalanceWallet)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardSecundario("Comissões\n(Produção)", DatabaseHelper.fmtBRL(a.comissoes), Color(0xFF1976D2))
                    CardSecundario("Vales / Adiant.\n(Descontos)", DatabaseHelper.fmtBRL(a.adiantamentos), Color(0xFFC62828))
                }
            }
        }
    }
}
