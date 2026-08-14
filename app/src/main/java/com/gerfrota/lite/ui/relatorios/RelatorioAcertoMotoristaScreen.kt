package com.gerfrota.lite.ui.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.RelatoriosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioAcertoMotoristaScreen() {
    // Obtendo db internamente (casa com a chamada do MainActivity sem parâmetros)
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    
    var motoristas by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var sel by remember { mutableStateOf<Long?>(null) }
    var mes by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
    var ano by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var ac by remember { mutableStateOf<RelatoriosDao.Acerto?>(null) }
    
    LaunchedEffect(Unit) { 
        withContext(Dispatchers.IO) {
            motoristas = db.queryAll("motoristas", "nome ASC")
            sel = (motoristas.firstOrNull()?.get("id") as? Long)
        } 
    }
    
    LaunchedEffect(sel, mes, ano) { 
        if (sel != null) {
            withContext(Dispatchers.IO) {
                ac = RelatoriosDao.acertoMotorista(db, sel!!, mes, ano)
            } 
        }
    }
    
    val nomes = motoristas.map { db.str(it["nome"]) }
    val nomeSel = motoristas.firstOrNull { (it["id"] as? Long) == sel }?.let { db.str(it["nome"]) }
    
    Scaffold(topBar = { TopAppBar(title = { Text("Acerto de Motorista") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            DropdownSimples("Motorista", nomeSel, nomes) { n: String -> 
                sel = motoristas.firstOrNull { db.str(it["nome"]) == n }?.get("id") as? Long 
            }
            Spacer(Modifier.height(8.dp))
            SeletorMesAno(mes, ano, { mes = it }, { ano = it })
            
            ac?.let { a ->
                Spacer(Modifier.height(16.dp))
                Text("Extrato de Fechamento do Mês", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                    Text(
                        if (a.saldo >= 0) "A frota deve pagar ${DatabaseHelper.fmtBRL(a.saldo)} ao motorista pelas comissões do mês, já abatendo os vales."
                        else "Atenção! O motorista retirou mais vales do que gerou em comissões. Ele está devendo ${DatabaseHelper.fmtBRL(abs(a.saldo))} para a frota.",
                        Modifier.padding(16.dp), fontSize = 14.sp
                    )
                }
            }
        }
    }
}
