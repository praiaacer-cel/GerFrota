package com.gerfrota.lite.data

fun DatabaseHelper.proximoIdAbastecimento(tabela: String): String {
    val maior = queryAll(tabela).maxOfOrNull { str(it["id_abastecimento"]).toIntOrNull() ?: 0 } ?: 0
    return (maior + 1).toString().padStart(5, '0')
}

fun DatabaseHelper.abastecimentosPorPlaca(tabela: String, placa: String): List<Map<String, Any?>> =
    queryAll(tabela).filter { str(it["placa_principal"]) == placa }
        .sortedByDescending { (it["id"] as? Long) ?: 0L }

fun DatabaseHelper.ultimoAbastecimento(tabela: String, placa: String): Map<String, Any?>? =
    abastecimentosPorPlaca(tabela, placa).firstOrNull()
