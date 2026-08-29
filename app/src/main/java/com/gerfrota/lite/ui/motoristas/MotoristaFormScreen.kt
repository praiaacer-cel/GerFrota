package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.ui.widgets.CampoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaFormScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var rg by remember { mutableStateOf("") }
    var cnh by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var vencCnh by remember { mutableStateOf("") }
    var certCargas by remember { mutableStateOf("") }
    var vencCargas by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var codBanco by remember { mutableStateOf("") }
    var banco by remember { mutableStateOf("") }
    var agencia by remember { mutableStateOf("") }
    var conta by remember { mutableStateOf("") }
    var pix by remember { mutableStateOf("") }
    var comissao by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id >= 0) withContext(Dispatchers.IO) {
            db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == id }?.let { m ->
                nome = db.str(m["nome"]); cpf = db.str(m["cpf"]); rg = db.str(m["rg"])
                cnh = db.str(m["cnh"]); categoria = db.str(m["categoria_cnh"])
                vencCnh = db.str(m["data_vencimento_cnh"]); certCargas = db.str(m["certificado_cargas"])
                vencCargas = db.str(m["vencimento_cargas"]); whatsapp = db.str(m["whatsapp"])
                telefone = db.str(m["telefone"]); endereco = db.str(m["endereco"])
                email = db.str(m["email"]); codBanco = db.str(m["codigo_banco"])
                banco = db.str(m["banco"]); agencia = db.str(m["agencia"])
                conta = db.str(m["conta"]); pix = db.str(m["chave_pix1"]); comissao = db.str(m["comissao"])
            }
        }
    }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mapOf(
            "nome" to nome, "cpf" to cpf, "rg" to rg, "cnh" to cnh,
            "categoria_cnh" to categoria, "data_vencimento_cnh" to vencCnh,
            "certificado_cargas" to certCargas, "vencimento_cargas" to vencCargas,
            "whatsapp" to whatsapp, "telefone" to telefone, "endereco" to endereco,
            "email" to email, "codigo_banco" to codBanco, "banco" to banco,
            "agencia" to agencia, "conta" to conta, "chave_pix1" to pix, "comissao" to comissao)
        if (id >= 0) db.update("motoristas", id, row) else db.insert("motoristas", row)
        withContext(Dispatchers.Main) { onBack() }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (id >= 0) "Editar Motorista" else "Novo Motorista") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            CampoForm("Nome *", nome, Modifier.fillMaxWidth()) { nome = it }
            Row {
                CampoForm("CPF", cpf, Modifier.weight(1f)) { cpf = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("RG", rg, Modifier.weight(1f)) { rg = it }
            }
            Row {
                CampoForm("CNH", cnh, Modifier.weight(1f)) { cnh = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("Categoria", categoria, Modifier.weight(1f)) { categoria = it }
            }
            Row {
                CampoData("Venc. CNH", vencCnh, { vencCnh = it }, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoForm("Comissão %", comissao, Modifier.weight(1f)) { comissao = it }
            }
            Row {
                CampoForm("Cert. Cargas", certCargas, Modifier.weight(1f)) { certCargas = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("Venc. Cargas", vencCargas, Modifier.weight(1f)) { vencCargas = it }
            }
            Row {
                CampoForm("WhatsApp", whatsapp, Modifier.weight(1f)) { whatsapp = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("Telefone", telefone, Modifier.weight(1f)) { telefone = it }
            }
            CampoForm("Endereço", endereco, Modifier.fillMaxWidth()) { endereco = it }
            CampoForm("E-mail", email, Modifier.fillMaxWidth()) { email = it }
            Text("Dados Bancários", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Row {
                CampoForm("Cód. Banco", codBanco, Modifier.weight(1f)) { codBanco = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("Banco", banco, Modifier.weight(2f)) { banco = it }
            }
            Row {
                CampoForm("Agência", agencia, Modifier.weight(1f)) { agencia = it }
                Spacer(Modifier.width(8.dp))
                CampoForm("Conta", conta, Modifier.weight(1f)) { conta = it }
            }
            CampoForm("Chave PIX", pix, Modifier.fillMaxWidth()) { pix = it }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))) {
                Text("SALVAR MOTORISTA", fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun CampoForm(label: String, value: String, modifier: Modifier = Modifier, on: (String) -> Unit) {
    OutlinedTextField(value, on, label = { Text(label) }, modifier = modifier)
}
