package com.gerfrota.lite.ui.motoristas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.ui.widgets.CampoData
import com.gerfrota.lite.ui.widgets.rememberAnexoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaFormScreen(id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { DatabaseHelper.get(ctx) }
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }; var cpf by remember { mutableStateOf("") }
    var nasc by remember { mutableStateOf("") }; var rg by remember { mutableStateOf("") }
    var emisRg by remember { mutableStateOf("") }; var cnh by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }; var vencCnh by remember { mutableStateOf("") }
    var certCargas by remember { mutableStateOf("") }; var vencCargas by remember { mutableStateOf("") }
    var whats by remember { mutableStateOf("") }; var tel by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var urgNome by remember { mutableStateOf("") }; var urgTel by remember { mutableStateOf("") }
    var comissao by remember { mutableStateOf("") }; var banco by remember { mutableStateOf("") }
    var codBanco by remember { mutableStateOf("") }; var ag by remember { mutableStateOf("") }
    var conta by remember { mutableStateOf("") }; var pix1 by remember { mutableStateOf("") }
    var pix2 by remember { mutableStateOf("") }
    var pathFoto by remember { mutableStateOf<String?>(null) }
    var pathCnh by remember { mutableStateOf<String?>(null) }
    var pathRes by remember { mutableStateOf<String?>(null) }
    var pathCargas by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id >= 0) withContext(Dispatchers.IO) {
            db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == id }?.let { m ->
                nome = db.str(m["nome"]); cpf = db.str(m["cpf"]); nasc = db.str(m["data_nascimento"])
                rg = db.str(m["rg"]); emisRg = db.str(m["data_emissao_rg"]); cnh = db.str(m["cnh"])
                cat = db.str(m["categoria_cnh"]); vencCnh = db.str(m["data_vencimento_cnh"])
                certCargas = db.str(m["certificado_cargas"]); vencCargas = db.str(m["vencimento_cargas"])
                whats = db.str(m["whatsapp"]); tel = db.str(m["telefone"]); end = db.str(m["endereco"])
                email = db.str(m["email"]); urgNome = db.str(m["contato_urgencia"]); urgTel = db.str(m["telefone_urgencia"])
                comissao = db.str(m["comissao"]); banco = db.str(m["banco"]); codBanco = db.str(m["codigo_banco"])
                ag = db.str(m["agencia"]); conta = db.str(m["conta"]); pix1 = db.str(m["chave_pix1"]); pix2 = db.str(m["chave_pix2"])
                pathFoto = db.str(m["path_foto"]).ifBlank { null }; pathCnh = db.str(m["path_cnh"]).ifBlank { null }
                pathRes = db.str(m["path_residencia"]).ifBlank { null }; pathCargas = db.str(m["path_cargas"]).ifBlank { null }
            }
        }
    }

    val base = nome.ifBlank { "SEM_NOME" }.trim().replace(" ", "_").uppercase()
    val pickFoto = rememberAnexoPicker("FotosdosMotoristas", "${base}_FOTO") { pathFoto = it }
    val pickCnh = rememberAnexoPicker("CNHdosMotoristas", "${base}_CNH") { pathCnh = it }
    val pickRes = rememberAnexoPicker("ResidenciadosMotoristas", "${base}_RES") { pathRes = it }
    val pickCargas = rememberAnexoPicker("CargasdosMotoristas", "${base}_CARGAS") { pathCargas = it }

    fun salvar() = scope.launch(Dispatchers.IO) {
        val row = mapOf(
            "nome" to nome.trim().uppercase(), "cpf" to cpf, "data_nascimento" to nasc,
            "rg" to rg, "data_emissao_rg" to emisRg, "cnh" to cnh, "categoria_cnh" to cat.uppercase(),
            "data_vencimento_cnh" to vencCnh, "certificado_cargas" to certCargas, "vencimento_cargas" to vencCargas,
            "telefone" to tel, "whatsapp" to whats, "endereco" to end, "email" to email,
            "contato_urgencia" to urgNome, "telefone_urgencia" to urgTel, "comissao" to comissao,
            "banco" to banco, "codigo_banco" to codBanco, "agencia" to ag, "conta" to conta,
            "chave_pix1" to pix1, "chave_pix2" to pix2,
            "path_foto" to pathFoto, "path_cnh" to pathCnh, "path_residencia" to pathRes, "path_cargas" to pathCargas)
        if (id >= 0) db.update("motoristas", id, row) else db.insert("motoristas", row)
        withContext(Dispatchers.Main) { onBack() }
    }

    @Composable
    fun AnexoBotao(label: String, path: String?, pick: () -> Unit) {
        Button(onClick = pick, colors = ButtonDefaults.buttonColors(
            containerColor = if (path != null) Color(0xFF2E7D32) else Color(0xFF546E7A)),
            modifier = Modifier.weight(1f).height(56.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (id >= 0) "Editar Motorista" else "Novo Motorista") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome Completo *") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(cpf, { cpf = it }, label = { Text("CPF") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoData("Data Nasc.", nasc, { nasc = it }, Modifier.weight(1f)) }
            Row { OutlinedTextField(rg, { rg = it }, label = { Text("RG") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoData("Emissão RG", emisRg, { emisRg = it }, Modifier.weight(1f)) }
            Row { OutlinedTextField(cnh, { cnh = it }, label = { Text("Nº CNH") }, modifier = Modifier.weight(2f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(cat, { cat = it }, label = { Text("Cat.") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CampoData("Venc. CNH", vencCnh, { vencCnh = it }, Modifier.weight(2f)) }
            Row { OutlinedTextField(certCargas, { certCargas = it }, label = { Text("Cert. Cargas Perigosas") }, modifier = Modifier.weight(2f))
                Spacer(Modifier.width(8.dp))
                CampoData("Validade", vencCargas, { vencCargas = it }, Modifier.weight(1f)) }
            Row { OutlinedTextField(whats, { whats = it }, label = { Text("WhatsApp") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(tel, { tel = it }, label = { Text("Telefone") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(end, { end = it }, label = { Text("Endereço Residencial") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedTextField(urgNome, { urgNome = it }, label = { Text("Contato Urgência") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(urgTel, { urgTel = it }, label = { Text("Tel. Urgência") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(comissao, { comissao = it }, label = { Text("Comissão (%)") }, modifier = Modifier.fillMaxWidth())
            Text("Dados Bancários", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Row { OutlinedTextField(banco, { banco = it }, label = { Text("Banco") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(codBanco, { codBanco = it }, label = { Text("Código") }, modifier = Modifier.weight(1f)) }
            Row { OutlinedTextField(ag, { ag = it }, label = { Text("Agência") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(conta, { conta = it }, label = { Text("Conta") }, modifier = Modifier.weight(1f)) }
            Row { OutlinedTextField(pix1, { pix1 = it }, label = { Text("PIX 1") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(pix2, { pix2 = it }, label = { Text("PIX 2") }, modifier = Modifier.weight(1f)) }

            Text("Anexos de Documentos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnexoBotao(if (pathFoto != null) "Foto ✓" else "Foto Perfil", pathFoto, pickFoto)
                AnexoBotao(if (pathCnh != null) "CNH ✓" else "CNH", pathCnh, pickCnh)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnexoBotao(if (pathRes != null) "Residência ✓" else "Residência", pathRes, pickRes)
                AnexoBotao(if (pathCargas != null) "Cargas ✓" else "Cargas", pathCargas, pickCargas)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { salvar() }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))) {
                Text("SALVAR MOTORISTA", fontWeight = FontWeight.Bold)
            }
        }
    }
}
