package com.gerfrota.lite.core

object VeiculoConstants {

    val tiposVeiculo = listOf(
        "Cavalo Mecânico Toco", "Cavalo Mecânico Trucado",
        "Semi-Reboque 1 eixo", "Semi-Reboque 2 eixos",
        "Semi-Reboque 3 eixos", "Semi-Reboque 4 eixos",
        "Caminhão Toco", "Caminhão Truck", "Caminhão BiTruck"
    )

    private val carroceriasComuns = listOf(
        "Carroceria Carga Seca", "Carroceria Graneleira", "Carroceria Sider",
        "Basculante", "Baú", "Baú Frigorífico", "Tanque", "Plataforma",
        "Cegonha", "Porta-Contêiner"
    )
    private val carroceriasToco = carroceriasComuns.dropLast(1)
    private val nenhuma = listOf("Nenhuma Carroceria")

    fun carroceriasPorTipo(tipo: String?): List<String> = when (tipo) {
        "Cavalo Mecânico Toco", "Cavalo Mecânico Trucado" -> nenhuma
        "Caminhão Toco" -> carroceriasToco
        null -> nenhuma
        else -> carroceriasComuns
    }

    fun quantidadePneus(tipo: String?): Int = when (tipo) {
        "Cavalo Mecânico Toco", "Caminhão Toco" -> 6
        "Cavalo Mecânico Trucado", "Caminhão Truck" -> 10
        "Caminhão BiTruck" -> 12
        "Semi-Reboque 1 eixo" -> 4
        "Semi-Reboque 2 eixos" -> 8
        "Semi-Reboque 3 eixos" -> 12
        "Semi-Reboque 4 eixos" -> 16
        else -> 4
    }

    fun posicoesPneus(tipo: String?): List<String> {
        val t = (tipo ?: "").lowercase()
        val diant = listOf("Dianteiro Esquerdo", "Dianteiro Direito")
        val tracao1 = listOf(
            "Tração 1° Eixo Esq. Fora", "Tração 1° Eixo Esq. Dentro",
            "Tração 1° Eixo Dir. Fora", "Tração 1° Eixo Dir. Dentro"
        )
        val tracao2 = listOf(
            "Tração 2° Eixo Esq. Fora", "Tração 2° Eixo Esq. Dentro",
            "Tração 2° Eixo Dir. Fora", "Tração 2° Eixo Dir. Dentro"
        )
        return when {
            t.contains("bitruck") -> diant +
                listOf("Bi-truck Dianteiro Esquerdo", "Bi-truck Dianteiro Direito") + tracao1 + tracao2
            t.contains("truck") || t.contains("trucado") -> diant + tracao1 + tracao2
            t.contains("toco") -> diant + tracao1
            t.contains("reboque") -> {
                val n = when {
                    t.contains("4") -> 4; t.contains("3") -> 3; t.contains("2") -> 2; else -> 1
                }
                (1..n).flatMap { i ->
                    listOf("$i° Eixo Esq. Fora", "$i° Eixo Esq. Dentro",
                           "$i° Eixo Dir. Fora", "$i° Eixo Dir. Dentro")
                }
            }
            else -> diant + tracao1
        }
    }

    fun ehTracao(tipo: String?): Boolean {
        val t = (tipo ?: "").lowercase()
        return t.contains("cavalo") || t.contains("caminhão") || t.contains("toco") ||
               t.contains("truck") || t.contains("bitruck")
    }
}
