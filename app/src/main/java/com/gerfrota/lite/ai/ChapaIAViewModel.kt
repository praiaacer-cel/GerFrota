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
        .replace("<|im_start|>", "").replace("<|im_end|>", "")
        .substringBefore("assistant:").trim()

    private fun respondeIA(t: String) = add(Role.IA, t)

    // ---------------- AÇÕES RÁPIDAS / CADASTRO GUIADO ----------------
    private fun acaoRapida(t: String): Boolean {
        val lower = t.lowercase()
        val placa = RX_PLACA.find(t.uppercase())?.value ?: return false
        val valor = RX_NUM.findAll(t).lastOrNull()?.value?.let { DatabaseHelper.parseMoney(it) } ?: return false
        val hoje = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())

        if (lower.contains("abastec") || lower.contains("diesel")) {
            val litros = RX_NUM.findAll(t).firstOrNull()?.value?.toDoubleOrNull() ?: 0.0
            db.insert("combustivel", mapOf(
                "id_abastecimento" to db.proximoIdAbastecimento("combustivel"),
                "placa_principal" to placa, "data_registro" to hoje,
                "litros" to litros, "valor_total" to valor,
                "valor_litro" to if (litros > 0) valor / litros else 0.0,
                "combustivel" to "Diesel S10"))
            respondeIA("✅ Abastecimento registrado: $placa, ${DatabaseHelper.fmtBRL(valor)}" +
                (if (litros > 0) " (${"%.0f".format(litros)} L)" else "") + " em $hoje.")
            return true
        }
        if (lower.contains("manuten") || lower.contains("conserto") || lower.contains("pneu") || lower.contains("gastei")) {
            db.insert("manutencoes", mapOf(
                "placa_veiculo" to placa, "data_servico" to hoje, "quilometragem" to 0,
                "sistema" to "Outros", "subsistema" to "OUTROS",
                "tipo_servico" to "Serviço registrado pelo Chapa IA",
                "valor_servico" to DatabaseHelper.fmtBRL(valor), "prestador" to "Não informado"))
            respondeIA("🔧 Manutenção registrada para $placa no valor de ${DatabaseHelper.fmtBRL(valor)}.")
            return true
        }
        return false
    }

    private fun iniciarCadastro(t: String) {
        val lower = t.lowercase()
        when {
            lower.contains("abastec") -> { fluxo = Fluxo.ABASTECIMENTO; etapa = 0; dados.clear()
                respondeIA("Vou registrar um abastecimento. Qual a PLACA do veículo?") }
            lower.contains("manuten") || lower.contains("conserto") -> { fluxo = Fluxo.MANUTENCAO; etapa = 0; dados.clear()
                respondeIA("Vou registrar uma manutenção. Qual a PLACA do veículo?") }
            else -> respondeIA("Posso guiar o cadastro de ABASTECIMENTO ou MANUTENÇÃO. " +
                "Para o restante, use as telas do app. Qual deseja?")
        }
    }

    private fun continuarCadastro(t: String) {
        when (fluxo) {
            Fluxo.ABASTECIMENTO -> when (etapa) {
                0 -> { dados["placa"] = RX_PLACA.find(t.uppercase())?.value ?: t.uppercase().take(7)
                    etapa = 1; respondeIA("Placa ${dados["placa"]}. Quantos LITROS?") }
                1 -> { dados["litros"] = (RX_NUM.find(t)?.value ?: "0")
                    etapa = 2; respondeIA("Agora o VALOR TOTAL em R\$.") }
                2 -> {
                    val valor = DatabaseHelper.parseMoney(RX_NUM.find(t)?.value ?: "0")
                    val litros = dados["litros"]?.toDoubleOrNull() ?: 0.0
                    val hoje = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                    db.insert("combustivel", mapOf(
                        "id_abastecimento" to db.proximoIdAbastecimento("combustivel"),
                        "placa_principal" to dados["placa"], "data_registro" to hoje,
                        "litros" to litros, "valor_total" to valor,
                        "valor_litro" to if (litros > 0) valor / litros else 0.0,
                        "combustivel" to "Diesel S10"))
                    respondeIA("✅ Abastecimento salvo: ${dados["placa"]} – ${"%.0f".format(litros)} L – " +
                        "${DatabaseHelper.fmtBRL(valor)}.")
                    fluxo = null
                }
            }
            Fluxo.MANUTENCAO -> when (etapa) {
                0 -> { dados["placa"] = RX_PLACA.find(t.uppercase())?.value ?: t.uppercase().take(7)
                    etapa = 1; respondeIA("Placa ${dados["placa"]}. Descreva o SERVIÇO realizado.") }
                1 -> { dados["servico"] = t; etapa = 2; respondeIA("Qual o VALOR do serviço em R\$?") }
                2 -> {
                    val valor = DatabaseHelper.parseMoney(RX_NUM.find(t)?.value ?: "0")
                    val hoje = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                    db.insert("manutencoes", mapOf(
                        "placa_veiculo" to dados["placa"], "data_servico" to hoje, "quilometragem" to 0,
                        "sistema" to "Outros", "subsistema" to "OUTROS",
                        "tipo_servico" to dados["servico"], "valor_servico" to DatabaseHelper.fmtBRL(valor)))
                    respondeIA("🔧 Manutenção salva: ${dados["placa"]} – ${dados["servico"]} – " +
                        "${DatabaseHelper.fmtBRL(valor)}.")
                    fluxo = null
                }
            }
            null -> {}
        }
    }

    /** Chip "Resumo do dia": alertas + financeiro formatados pelo LLM (ou direto). */
    fun resumoDoDia() {
        add(Role.USER, "Resumo do dia")
        viewModelScope.launch(Dispatchers.IO) {
            val fatos = rag.resumoMatinal()
            if (_status.value != ModelStatus.PRONTO) { respondeIA(fatos); return@launch }
            _gerando.value = true; add(Role.IA, "", loading = true)
            val prompt = PromptBuilder.chat(
                "Você é o Chapa IA. Dê bom dia ao gestor e resuma os fatos abaixo de forma curta e amigável.",
                "Gere o resumo matinal.", fatos)
            engine.generate(prompt, 256, object : LlamaCppEngine.GenerateCallback {
                override fun onToken(t: String) = _messages.update { l ->
                    if (l.isEmpty()) l else l.dropLast(1) + ChatMsg(Role.IA, l.last().text + t) }
                override fun onComplete() {
                    _messages.update { l ->
                        if (l.isEmpty()) l else l.dropLast(1) +
                            ChatMsg(Role.IA, limpar(l.last().text).ifBlank { fatos }) }
                    _gerando.value = false
                }
            })
        }
    }

    override fun onCleared() { engine.release(); super.onCleared() }
}
