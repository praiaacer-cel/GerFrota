// ai/ChapaIAViewModel.kt
package com.gerfrota.lite.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gerfrota.lite.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ModelStatus { CARREGANDO, PRONTO, SEM_MODELO, ERRO }
enum class Role { USER, IA, SISTEMA }
data class ChatMsg(val role: Role, var text: String, val loading: Boolean = false)

class ChapaIAViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = LlamaCppEngine()
    private val db = DatabaseHelper.get(app)
    private val files = FileSearchService(app)
    private val rag = RagService(db, files)
    private val ocr = OcrService(app) // <-- INTEGRAÇÃO DO OCR

    private val _messages = MutableStateFlow<List<ChatMsg>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _status = MutableStateFlow(ModelStatus.CARREGANDO)
    val status = _status.asStateFlow()
    private val _gerando = MutableStateFlow(false)
    val gerando = _gerando.asStateFlow()

    // fluxo de cadastro guiado
    private enum class Fluxo { ABASTECIMENTO, MANUTENCAO }
    private var fluxo: Fluxo? = null
    private var etapa = 0
    private val dados = mutableMapOf<String, String>()

    private val RX_PLACA = Regex("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}|[A-Z]{3}[0-9]{4}")
    private val RX_NUM = Regex("[0-9]+([.,][0-9]{1,2})?")

    init {
        add(Role.IA, "Olá! Sou o Chapa IA do GerFrotaLite. 🚛\nPosso consultar o banco de dados e os " +
            "documentos da frota, ou registrar abastecimentos e manutenções por frase.\n" +
            "Ex.: \"Quanto gastei de diesel este mês?\"")
        viewModelScope.launch(Dispatchers.IO) {
            val model = ModelManager.resolve(app)
            if (model == null) _status.value = ModelStatus.SEM_MODELO
            else {
                val ok = engine.init(model.absolutePath, nCtx = 2048,
                    nThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4))
                _status.value = if (ok) ModelStatus.PRONTO else ModelStatus.ERRO
            }
        }
    }

    private fun add(role: Role, text: String, loading: Boolean = false) =
        _messages.update { it + ChatMsg(role, text, loading) }

    fun send(raw: String) {
        val texto = raw.trim(); if (texto.isEmpty() || _gerando.value) return
        add(Role.USER, texto)

        viewModelScope.launch(Dispatchers.IO) {
            // 1) cadastro guiado em andamento?
            if (fluxo != null) { continuarCadastro(texto); return@launch }

            // 2) ação rápida por frase (ex.: "abasteci 300 reais na CBA1234")
            if (acaoRapida(texto)) return@launch

            when (IntentClassifier.classificar(texto)) {
                IaIntencao.SAUDACAO -> respondeIA("Olá, chefe! 👋 Em que posso ajudar? Posso consultar " +
                    "combustível, manutenções, viagens, documentos ou registrar um abastecimento.")
                IaIntencao.AJUDA -> respondeIA("Você pode perguntar:\n• Quanto gastei de diesel este mês?\n" +
                    "• Qual caminhão deu prejuízo?\n• Documentos da placa CBA1234\n• Resumo do dia\n" +
                    "Ou comandar: \"Cadastre um abastecimento de 500 reais na placa CBA1234\".")
                IaIntencao.CADASTRO -> iniciarCadastro(texto)
                else -> consultaRag(texto)   // CONSULTA / DESCONHECIDA → RAG + LLM
            }
        }
    }

    // ---------------- OCR / FOTO DE NOTA ----------------
    fun processarFoto(path: String) {
        if (_gerando.value) return // Evita múltiplas requisições simultâneas
        
        viewModelScope.launch(Dispatchers.IO) {
            add(Role.USER, "📷 Foto de nota/documento enviada.")
            val texto = ocr.lerImagem(path)
            
            if (texto.isBlank()) {
                add(Role.IA, "Não consegui ler o texto da imagem.")
            } else {
                // Fallback para modo simulado caso o modelo LLM não esteja carregado
                if (_status.value != ModelStatus.PRONTO) {
                    add(Role.IA, "Texto extraído (modo simulado):\n$texto")
                    return@launch
                }
                
                _gerando.value = true
                add(Role.IA, "", loading = true)
                
                // Passa o texto sujo do OCR para o LLM extrair DATA/LOCAL/TOTAL e responder
                val systemPrompt = "Você é um leitor de notas fiscais. Extraia: DATA: [data] | LOCAL: [nome] | TOTAL: [valor]. Se não achar, use N/A."
                val prompt = PromptBuilder.extrator(systemPrompt, texto)
                
                // Reutiliza o fluxo de geração nativa do engine (substituindo gerarRespostaLlm)
                engine.generate(prompt, 256, object : LlamaCppEngine.GenerateCallback {
                    override fun onToken(t: String) = _messages.update { list ->
                        if (list.isEmpty()) list else list.dropLast(1) +
                            ChatMsg(Role.IA, list.last().text + t)
                    }
                    override fun onComplete() {
                        _messages.update { list ->
                            if (list.isEmpty()) list else list.dropLast(1) +
                                ChatMsg(Role.IA, limpar(list.last().text).ifBlank { texto })
                        }
                        _gerando.value = false
                    }
                })
            }
        }
    }

    // ---------------- CONSULTA (RAG + LLM) ----------------
    private suspend fun consultaRag(texto: String) {
        val dadosCtx = rag.contexto(texto)
        if (_status.value != ModelStatus.PRONTO) {   // modo simulado: entrega o contexto puro
            respondeIA(dadosCtx); return
        }
        _gerando.value = true
        add(Role.IA, "", loading = true)
        val prompt = PromptBuilder.chat(PromptBuilder.SISTEMA_CHAPA, texto, dadosCtx)
        engine.generate(prompt, 384, object : LlamaCppEngine.GenerateCallback {
            override fun onToken(t: String) = _messages.update { list ->
                if (list.isEmpty()) list else list.dropLast(1) +
                    ChatMsg(Role.IA, list.last().text + t)
            }
            override fun onComplete() {
                _messages.update { list ->
                    if (list.isEmpty()) list else list.dropLast(1) +
                        ChatMsg(Role.IA, limpar(list.last().text).ifBlank { dadosCtx })
                }
                _gerando.value = false
            }
        })
    }

    private fun limpar(t: String): String = t
        .replace("
