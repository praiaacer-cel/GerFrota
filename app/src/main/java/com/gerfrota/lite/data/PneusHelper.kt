package com.gerfrota.lite.data

/** Próximo código sequencial PN-00001 */
fun DatabaseHelper.proximoCodigoPneu(): String {
    var seq = 1
    queryAll("pneus").forEach { p ->
        val c = str(p["codigo_fogo"])
        if (c.startsWith("PN-")) {
            val n = c.substringAfter("-").toIntOrNull() ?: 0
            if (n >= seq) seq = n + 1
        }
    }
    return "PN-%05d".format(seq)
}

/** Valor acumulado do pneu (compra + serviços de pneus) */
fun DatabaseHelper.valorAcumuladoPneu(codigo: String): Double {
    var total = queryAll("pneus")
        .firstOrNull { str(it["codigo_fogo"]) == codigo }
        ?.let { num(it["valor_compra"]) } ?: 0.0
    total += queryAll("manutencoes").filter { m ->
        str(m["sistema"]).equals("Pneus", true) &&
            (str(m["subsistema"]).contains(codigo, true) ||
             str(m["tipo_servico"]).contains(codigo, true) ||
             str(m["observacao"]).contains(codigo, true))
    }.sumOf { num(it["valor_servico"]) }
    return total
}

/** Normalização para casar posições/códigos em serviços de pneus */
fun normalizarPosicao(s: String): String = s.lowercase()
    .replace("ã", "a").replace("ç", "c").replace("õ", "o")
    .replace("á", "a").replace("é", "e").replace("í", "i")
    .replace("ó", "o").replace("ú", "u")
    .replace("esquerdo", "esq").replace("direito", "dir")
    .replace(".", "").replace("-", "").replace(" ", "").trim()
