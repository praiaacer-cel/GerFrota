// ai/RagService.kt
package com.gerfrota.lite.ai

import com.gerfrota.lite.data.DatabaseHelper
import java.util.Calendar

/** Recupera dados REAIS do gerfrotalite.db e das pastas de arquivos para o LLM. */
class RagService(private val db: DatabaseHelper, private val files: FileSearchService) {

    private val RX_PLACA = Regex("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}|[A-Z]{3}[0-9]{4}")

    fun placaNaConsulta(t: String): String? =
        RX_PLACA.find(t.uppercase())?.value

    suspend fun contexto(consulta: String): String {
        val tipo = IntentClassifier.tipoConsulta(consulta)
        val agora = Calendar.getInstance()
        return when (tipo) {
            "COMBUSTIVEL" -> comb(agora)
            "MANUTENCAO" -> manut()
            "MOTORISTAS" -> motoristas(consulta)
            "VIAGENS" -> viagens()
            "PNEUS" -> pneus()
            "FINANCEIRO" -> financeiro(agora)
            "ARLA" -> arla()
            "FROTA" -> frota(consulta)
            else -> geral(agora)
        }
    }

    private fun comb(agora: Calendar): String {
        val mes = agora.get(Calendar.MONTH) + 1; val ano = agora.get(Calendar.YEAR)
        val porPlaca = db.gastoCombustivelMes(mes, ano)
        if (porPlaca.isEmpty())
            return "Nenhum gasto de combustível registrado em $mes/$ano no banco gerfrotalite.db."
        val total = porPlaca.values.sumOf { it.first }
        val litros = porPlaca.values.sumOf { it.second }
        val sb = StringBuilder("GASTOS COM COMBUSTÍVEL em $mes/$ano:\n")
        sb.append("Total: ${DatabaseHelper.fmtBRL(total)} (${litros.toInt()} litros)\n")
        porPlaca.forEach { (p, v) -> sb.append("• $p: ${DatabaseHelper.fmtBRL(v.first)} (${v.second.toInt()} L)\n") }
        files.lerTxtPasta("Prontuariosabastecimento", "", 600)?.let { sb.append("\nPRONTUÁRIO DE ABASTECIMENTO:\n$it") }
        return sb.toString()
    }

    private fun manut(): String {
        val rows = db.queryAll("manutencoes", "id DESC").take(6)
        if (rows.isEmpty()) return "Nenhuma manutenção registrada no banco de dados."
        val sb = StringBuilder("MANUTENÇÕES RECENTES:\n")
        rows.forEach { m ->
            sb.append("• ${m["placa_veiculo"]} ${m["data_servico"]} – ${m["sistema"]}/${m["subsistema"]}: " +
                "${m["tipo_servico"]} (${DatabaseHelper.fmtBRL(db.num(m["valor_servico"]))})\n")
        }
        val placas = rows.mapNotNull { it["placa_veiculo"]?.toString() }.distinct()
        placas.take(2).forEach { p ->
            val docs = files.arquivos(p).filter { it.name.contains("Manutencao", true) || it.parentFile?.name == "NotasdosServicos" }
            if (docs.isNotEmpty()) sb.append("Comprovantes de $p: " + docs.joinToString(", ") { it.name } + "\n")
        }
        return sb.toString()
    }

    private fun motoristas(consulta: String): String {
        val rows = db.queryAll("motoristas", "nome ASC")
        if (rows.isEmpty()) return "Nenhum motorista cadastrado no sistema."
        val sb = StringBuilder("MOTORISTAS CADASTRADOS:\n")
        rows.forEach { m ->
            sb.append("• ${m["nome"]} | CNH: ${m["cnh"] ?: "-"} (venc. ${m["data_vencimento_cnh"] ?: "-"}) | " +
                "Comissão: ${m["comissao"] ?: "0"}% | Tel: ${m["whatsapp"] ?: m["telefone"] ?: "-"}\n")
        }
        val nome = rows.map { db.str(it["nome"]) }.firstOrNull { n ->
            n.split(" ").any { parte -> parte.length > 3 && consulta.contains(parte, true) }
        }
        if (nome != null) sb.append("\n").append(files.documentosPessoa(nome))
        return sb.toString()
    }

    private fun viagens(): String {
        val rows = db.queryAll("viagens", "id DESC").take(6)
        if (rows.isEmpty()) return "Nenhuma viagem registrada no banco de dados."
        val sb = StringBuilder("VIAGENS RECENTES:\n")
        rows.forEach { v ->
            sb.append("• Nro ${v["nro_viagem"]}: ${v["cidade_partida"]} → ${v["cidade_destino"]} " +
                "(${v["data_carga"]}) | Bruto: ${DatabaseHelper.fmtBRL(db.num(v["valor_bruto"]))} | " +
                "Líquido: ${DatabaseHelper.fmtBRL(db.num(v["valor_liquido"]))} | Status: ${v["status"]}\n")
        }
        val receber = db.contasReceber()
        val totalRec = receber.sumOf { db.num(it["valor_bruto"]) - db.num(it["pago_adiantamento_val"]) }
        sb.append("CONTAS A RECEBER: ${receber.size} viagem(ns), total ${DatabaseHelper.fmtBRL(totalRec)}.\n")
        return sb.toString()
    }

    private fun pneus(): String {
        val rows = db.queryAll("pneus")
        if (rows.isEmpty()) return "Nenhum pneu cadastrado."
        val porMarca = rows.groupBy { db.str(it["marca"]).ifBlank { "Sem marca" } }
        val sb = StringBuilder("DESEMPENHO DE PNEUS (CPK aproximado):\n")
        porMarca.map { (marca, ps) ->
            val km = ps.sumOf { db.num(it["km_atual"]) }
            val custo = ps.sumOf { db.num(it["valor_compra"]) }
            Triple(marca, ps.size, if (km > 0) custo / km else 0.0)
        }.sortedBy { it.third }.forEach { (marca, qtd, cpk) ->
            sb.append("• $marca: $qtd pneu(s), CPK ${DatabaseHelper.fmtBRL(cpk)}\n")
        }
        return sb.toString()
    }

    private fun financeiro(agora: Calendar): String {
        val mes = agora.get(Calendar.MONTH) + 1; val ano = agora.get(Calendar.YEAR)
        val (rec, desp, saldo) = db.resumoFinanceiroMes(mes, ano)
        val receber = db.contasReceber().sumOf { db.num(it["valor_bruto"]) - db.num(it["pago_adiantamento_val"]) }
        return "FLUXO DE CAIXA $mes/$ano:\n• Receitas (líquido viagens): ${DatabaseHelper.fmtBRL(rec)}\n" +
            "• Despesas (manutenção+combustível+adiantamentos): ${DatabaseHelper.fmtBRL(desp)}\n" +
            "• Saldo do mês: ${DatabaseHelper.fmtBRL(saldo)}\n" +
            "• Total a receber de fretes: ${DatabaseHelper.fmtBRL(receber)}"
    }

    private fun arla(): String {
        val rows = db.queryAll("arla", "id DESC").take(5)
        if (rows.isEmpty()) return "Nenhum abastecimento de ARLA 32 registrado."
        val sb = StringBuilder("ARLA 32 RECENTE:\n")
        rows.forEach { a ->
            sb.append("• ${a["placa_principal"]} ${a["data_registro"]}: ${db.num(a["litros"]).toInt()} L, " +
                "${DatabaseHelper.fmtBRL(db.num(a["valor_total"]))}\n")
        }
        return sb.toString()
    }

    private fun frota(consulta: String): String {
        val rows = db.queryAll("frota", "placa ASC")
        if (rows.isEmpty()) return "Nenhum veículo cadastrado na frota."
        val sb = StringBuilder("VEÍCULOS DA FROTA (${rows.size}):\n")
        rows.forEach { v ->
            sb.append("• ${v["placa"]} – ${v["marca"] ?: ""} ${v["modelo"] ?: ""} " +
                "(${v["tipo_veiculo"] ?: "-"}) ANTT venc. ${v["vencimento_antt"] ?: "-"}\n")
        }
        placaNaConsulta(consulta)?.let { p ->
            sb.append("\n").append(files.documentosPlaca(p))
            files.lerProntuario(p)?.let { sb.append("\nPRONTUÁRIO DA PLACA $p:\n$it") }
        }
        return sb.toString()
    }

    private fun geral(agora: Calendar): String {
        val nv = db.queryAll("frota").size
        val nm = db.queryAll("motoristas").size
        val nvi = db.queryAll("viagens").size
        val alertas = db.alertasVencimento()
        val sb = StringBuilder("RESUMO DO GERFROTALITE (${agora.get(Calendar.MONTH) + 1}/${agora.get(Calendar.YEAR)}):\n")
        sb.append("• Veículos: $nv | Motoristas: $nm | Viagens: $nvi\n")
        if (alertas.isNotEmpty()) sb.append("ALERTAS:\n" + alertas.joinToString("\n") { "• $it" } + "\n")
        else sb.append("• Nenhum documento vencendo nos próximos 30 dias.\n")
        return sb.toString()
    }

    fun resumoMatinal(): String {
        val alertas = db.alertasVencimento()
        val (rec, desp, saldo) = db.resumoFinanceiroMes(
            Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))
        return "ALERTAS DO SISTEMA:\n" +
            (if (alertas.isEmpty()) "• Nenhum alerta urgente." else alertas.joinToString("\n") { "• $it" }) +
            "\nSALDO DO MÊS: ${DatabaseHelper.fmtBRL(saldo)} (receitas ${DatabaseHelper.fmtBRL(rec)}, " +
            "despesas ${DatabaseHelper.fmtBRL(desp)})"
    }
}
