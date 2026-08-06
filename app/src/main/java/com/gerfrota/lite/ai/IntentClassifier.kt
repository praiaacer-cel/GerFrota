// ai/IntentClassifier.kt
package com.gerfrota.lite.ai

enum class IaIntencao { CONSULTA, CADASTRO, SAUDACAO, AJUDA, DESCONHECIDA }

/** Fallback por palavras-chave (idêntico à lógica Dart) + uso do LLM quando pronto. */
object IntentClassifier {
    private val palavras = mapOf(
        IaIntencao.SAUDACAO to listOf("olá", "ola", "oi", "bom dia", "boa tarde", "boa noite"),
        IaIntencao.AJUDA to listOf("ajuda", "como funciona", "o que você faz", "tutorial", "exemplos"),
        IaIntencao.CADASTRO to listOf("cadastre", "registre", "adicione", "novo", "nova", "troquei",
            "abasteci", "paguei", "comprei", "fiz manutenção", "gastei"),
        IaIntencao.CONSULTA to listOf("quanto", "gastei", "faturei", "lucro", "prejuízo", "consumo",
            "km/l", "qual", "quantos", "total", "média", "resumo", "relatório", "liste", "mostre",
            "quem", "quando", "pendente", "vencido", "documento", "cnh", "nota")
    )

    fun classificar(texto: String): IaIntencao {
        val lower = texto.lowercase()
        var melhor = IaIntencao.DESCONHECIDA; var score = 0
        for ((int, keys) in palavras) {
            val s = keys.count { lower.contains(it) }
            if (s > score) { score = s; melhor = int }
        }
        return if (score == 0) IaIntencao.DESCONHECIDA else melhor
    }

    fun tipoConsulta(t: String): String {
        val s = t.lowercase()
        return when {
            "motorista" in s -> "MOTORISTAS"
            "viagem" in s || "frete" in s -> "VIAGENS"
            "arla" in s || "ureia" in s || "adblue" in s -> "ARLA"
            "diesel" in s || "combust" in s || "abastec" in s || "gastei" in s -> "COMBUSTIVEL"
            "manuten" in s || "conserto" in s || "oficina" in s -> "MANUTENCAO"
            "pneu" in s -> "PNEUS"
            "lucro" in s || "preju" in s || "custo" in s || "despesa" in s || "receber" in s -> "FINANCEIRO"
            "caminh" in s || "frota" in s || "veículo" in s || "placa" in s || "documento" in s || "cnh" in s -> "FROTA"
            else -> "GERAL"
        }
    }
}
