package com.gerfrota.lite.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gerfrota.lite.ai.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapaIaScreen(vm: ChapaIAViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val msgs by vm.messages.collectAsState()
    val status by vm.status.collectAsState()
    val gerando by vm.gerando.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Configuração da Câmera e Arquivo temporário
    val cameraFile = remember { java.io.File(ctx.cacheDir, "nota_${System.currentTimeMillis()}.jpg") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) vm.processarFoto(cameraFile.absolutePath)
    }

    LaunchedEffect(msgs.size, msgs.lastOrNull()?.text?.length) {
        if (msgs.isNotEmpty()) listState.scrollToItem(msgs.size - 1)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_chapa_ia), null, Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chapa IA", fontWeight = FontWeight.Bold)
                    } },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
                Box(Modifier.fillMaxWidth().background(Color(0xFF2E7D32))
                    .padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("100% Offline e Privado. Seus dados não saem do celular.",
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column {
                    Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Quanto gastei de diesel este mês?" to null,
                               "Resumo do dia" to { vm.resumoDoDia() },
                               "Cadastre um abastecimento" to null,
                               "Documentos da frota" to null).forEach { (label, acao) ->
                            AssistChip(onClick = {
                                acao?.invoke() ?: run { input = label }
                            }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Botão da Câmera
                        FilledIconButton(onClick = { camera.launch(FileProvider
                            .getUriForFile(ctx, "${ctx.packageName}.fileprovider", cameraFile)) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1976D2))) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = input, onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Pergunte ou comande…") },
                            shape = RoundedCornerShape(24.dp),
                            enabled = !gerando, maxLines = 3)
                            
                        Spacer(Modifier.width(8.dp))
                        
                        FilledIconButton(
                            onClick = { vm.send(input); input = "" },
                            enabled = input.isNotBlank() && !gerando,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = AzulCard)) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            when (status) {
                ModelStatus.CARREGANDO -> Banner("Carregando modelo Qwen2.5-1.5B…", Color(0xFFE3F2FD))
                ModelStatus.SEM_MODELO -> Banner("Modelo não encontrado — modo simulado ativo (RAG direto do banco).", Color(0xFFFFF3E0))
                ModelStatus.ERRO -> Banner("Erro ao inicializar o modelo — modo simulado ativo.", Color(0xFFFFEBEE))
                ModelStatus.PRONTO -> {}
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp)) {
                itemsIndexed(msgs, key = { i, _ -> i }) { _, m ->
                    val ehUser = m.role == Role.USER
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = if (ehUser) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(14.dp, 14.dp,
                                if (ehUser) 4.dp else 14.dp, if (ehUser) 14.dp else 4.dp),
                            color = when (m.role) {
                                Role.USER -> Color(0xFFBBDEFB)
                                Role.SISTEMA -> Color(0xFFFFE0B2)
                                Role.IA -> Color.White
                            },
                            shadowElevation = if (ehUser) 0.dp else 1.dp,
                            modifier = Modifier.widthIn(max = 320.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                if (m.loading) Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pensando…", fontSize = 12.sp, color = Color.Gray)
                                } else Text(m.text, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(texto: String, cor: Color) {
    Box(Modifier.fillMaxWidth().background(cor).padding(8.dp)) {
        Text(texto, fontSize = 12.sp)
    }
}
