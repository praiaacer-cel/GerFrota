// ai/PromptBuilder.kt
package com.gerfrota.lite.ai

object PromptBuilder {
    const val SISTEMA_CHAPA = """
Você é o "Chapa IA", assistente virtual do aplicativo GerFrotaLite (gestão de frota de caminhões).
REGRAS OBRIGATÓRIAS:
1. Responda APENAS com base nos DADOS FORNECIDOS.
2. Se a informação não estiver nos dados, diga: "Não tenho essa informação no momento."
3. Seja curto, educado e direto. Use português do Brasil.
"""

    /** Prompt ChatML exigido pelo Qwen2.5 (mesmo padrão do projeto Flutter). */
    fun chat(system: String, usuario: String, dados: String): String =
        buildString {
            append("<|im_start|>system\n").append(system).append("\n")
            append("DADOS FORNECIDOS PELO SISTEMA:\n").append(dados).append("\n<|im_end|>\n")
            append("<|im_start|>user\n").append(usuario).append("\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }

    fun extrator(system: String, frase: String): String =
        buildString {
            append("<|im_start|>system\n").append(system).append("\n<|im_end|>\n")
            append("<|im_start|>user\nFrase: \"").append(frase).append("\"\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
}
