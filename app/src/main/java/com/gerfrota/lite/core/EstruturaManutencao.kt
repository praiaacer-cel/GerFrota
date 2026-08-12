package com.gerfrota.lite.core

import com.gerfrota.lite.core.VeiculoConstants

/** Cascata SISTEMA → SUBSISTEMA por tipo de veículo (igual ao Flutter). */
object EstruturaManutencao {

    private val motor = listOf("Bloco", "Cabeçote", "Válvulas", "Injeção", "Turbo",
        "Filtro Ar Motor", "Filtro Óleo Lubrificante", "Filtro Combustível",
        "Filtro Separador de Água", "Filtro ARLA", "Óleo Lubrificante",
        "Líquido Arrefecimento", "Graxa")
    private val transmissao = listOf("Câmbio", "Embreagem", "Eixo Cardan",
        "Diferencial", "Lubrificante Câmbio", "Lubrificante Diferencial")
    private val parachoquesFrente = listOf("Dianteiro", "Lat. Esquerdo", "Lat. Direito", "Traseiro")
    private val parachoquesTras = listOf("Lat. Esquerdo", "Lat. Direito", "Traseiro")
    private val direcao = listOf("Caixa de Direção", "Coluna", "Terminais", "Bomba Hidráulica", "Fluído Hidráulico")
    private val seguranca = listOf("Tacógrafo Analógico", "Tacógrafo Digital")
    private val iluminacao = listOf("Farol", "Lanterna", "Luz de Freio", "Luz de Ré", "Pisca", "Fiação")
    private val arCaminhao = listOf("Compressor", "Válv. Reguladora", "Secador de Ar",
        "Válv. Prot. 4 Circuitos", "Válvulas de Pedal", "Válv. Moduladoras")
    private val arSemi = listOf("Engates (Mão de Amigo)", "Filtro de Linha",
        "Válv. Simuladora de Carga", "Válv. Emergência", "Válv. Carga", "Sist. Susp. Pneumática")
    private val eletrico = listOf("Módulo Eletrônico", "Bateria", "Alternador",
        "Motor de Partida", "Fiação", "Fusíveis")
    private val estruturaSemi = listOf("Chassis", "Carroceria", "Engate", "Piso", "Graxa")

    private fun freios(lonas: List<String>) = lonas +
        listOf("Cilindro Mestre", "Pastilhas", "Disco", "Tambor", "Câmara de Freio")
    private fun suspensao(molas: List<String>) = molas + listOf("Amortecedores", "Buchas", "Estabilizador")
    private fun paralamas(pos: List<String>) = pos.map { "Paralamas $it" }

    private fun eixosTraseiros(prefixos: List<String>): List<String> =
        prefixos.flatMap { p -> listOf("$p Esq.", "$p Dir.") }

    private fun mapBase(traseiros: List<String>, semi: Boolean = false): Map<String, List<String>> {
        val m = LinkedHashMap<String, List<String>>()
        m["Freios"] = freios(eixosTraseiros(listOf("Lona Dianteira") .map { it.replace("Dianteira", "Dianteira") } ) .let { l ->
            // monta lonas dianteiras + traseiras
            val dianteiras = listOf("Lona Dianteira Esq.", "Lona Dianteira Dir.")
            dianteiras + traseiros + l.takeLast(5)
        })
        m["Suspensão"] = suspensao(listOf("Molas Dianteira Esq.", "Molas Dianteira Dir.") + traseiros.map { it.replace("Lona", "Molas") })
        m["Paralamas"] = paralamas(listOf("Dianteira Esq.", "Dianteira Dir.") + traseiros.map { it.replace("Lona", "") }.map { it.trim() })
        m["Parachoques"] = if (semi) parachoquesTras else parachoquesFrente
        m["Iluminação"] = iluminacao
        m["Sistema de Ar"] = if (semi) arSemi else arCaminhao +
            (if (traseiros.isNotEmpty()) listOf("Sist. Susp. Pneumática") else emptyList())
        if (!semi) { m["Motor"] = motor; m["Transmissão"] = transmissao; m["Direção"] = direcao;
            m["Equip. Segurança"] = seguranca; m["Elétrico/Eletrônico"] = eletrico }
        else m["Estrutura Semi-Reboque"] = estruturaSemi
        m["Outros"] = listOf("OUTROS")
        return m
    }

    fun estruturaPara(tipo: String): Map<String, List<String>> {
        val pneus = VeiculoConstants.posicoesPneus(tipo)
        val t = tipo.lowercase()
        val base: Map<String, List<String>> = when {
            t.contains("bitruck") -> mapBase(listOf("Lona Bi-truck Esq.", "Lona Bi-truck Dir.",
                "Lona Tração Esq.", "Lona Tração Dir.", "Lona Truck Esq.", "Lona Truck Dir."))
            t.contains("truck") || t.contains("trucado") -> mapBase(listOf("Lona Tração Esq.",
                "Lona Tração Dir.", "Lona Truck Esq.", "Lona Truck Dir."))
            t.contains("reboque") -> {
                val eixos = when { 
                    t.contains("4") -> 4
                    t.contains("3") -> 3
                    t.contains("2") -> 2
                    else -> 1 
                }
                val lonas = (1..eixos).flatMap { i -> 
                    listOf("Lona ${i}º Eixo Esq.", "Lona ${i}º Eixo Dir.") 
                }
                mapBase(lonas, semi = true)
            }
            else -> mapBase(listOf("Lona Tração Esq.", "Lona Tração Dir."))
        }
        val completo = LinkedHashMap<String, List<String>>()
        completo["Pneus"] = pneus
        completo.putAll(base)
        return completo
    }
}
