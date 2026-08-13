package com.gerfrota.lite.data

import java.util.Calendar

object RelatoriosDao {

    private fun noMes(db: DatabaseHelper, data: String?, mes: Int, ano: Int): Boolean {
        val c = DatabaseHelper.parseDataBR(data) ?: return false
        return c.get(Calendar.MONTH) + 1 == mes && c.get(Calendar.YEAR) == ano
    }

    data class RaioX(val qtd: Int, val bruto: Double, val despesas: Double, val liquido: Double)
    fun raioX(db: DatabaseHelper, placa: String, mes: Int, ano: Int): RaioX {
        var q = 0; var b = 0.0; var d = 0.0; var l = 0.0
        db.queryAll("viagens").forEach { v ->
            if (db.str(v["placas"]).contains(placa) && noMes(db, db.str(v["data_carga"]), mes, ano)) {
                q++; b += db.num(v["valor_bruto"]); d += db.num(v["resumo_despesas"]); l += db.num(v["valor_liquido"])
            }
        }
        return RaioX(q, b, d, l)
    }

    data class Acerto(val comissoes: Double, val adiantamentos: Double, val saldo: Double)
    fun acertoMotorista(db: DatabaseHelper, motoristaId: Long, mes: Int, ano: Int): Acerto {
        val mot = db.queryAll("motoristas").firstOrNull { (it["id"] as? Long) == motoristaId } ?: return Acerto(0.0, 0.0, 0.0)
        val nome = db.str(mot["nome"]).trim().lowercase()
        val pct = DatabaseHelper.parseMoney(db.str(mot["comissao"]))
        var com = 0.0
        db.queryAll("viagens").forEach { v ->
            if (db.str(v["motorista"]).trim().lowercase() == nome && noMes(db, db.str(v["data_carga"]), mes, ano))
                com += (db.num(v["valor_bruto"]) - db.num(v["agenciador_val"])) * pct / 100.0
        }
        var adi = 0.0
        db.queryAll("adiantamentos").forEach { a ->
            if ((a["motorista_id"] as? Long) == motoristaId && noMes(db, db.str(a["data"]), mes, ano)) adi += db.num(a["valor"])
        }
        return Acerto(com, adi, com - adi)
    }

    data class RankPneu(val marca: String, val qtd: Int, val km: Double, val custo: Double, val cpk: Double)
    fun desempenhoPneus(db: DatabaseHelper): List<RankPneu> =
        db.queryAll("pneus").groupBy { db.str(it["marca"]).ifBlank { "Desconhecida" } }
            .map { (marca, list) ->
                val km = list.sumOf { db.num(it["km_atual"]) }
                val custo = list.sumOf { db.num(it["valor_compra"]) }
                RankPneu(marca, list.size, km, custo, if (km > 0) custo / km else 0.0)
            }.sortedBy { it.cpk }

    data class Consumo(val placa: String, val km: Double, val litros: Double, val media: Double)
    fun consumoCombustivel(db: DatabaseHelper): List<Consumo> =
        db.queryAll("combustivel").groupBy { db.str(it["placa_principal"]) }
            .map { (placa, list) ->
                val km = list.sumOf { db.num(it["km_rodado"]) }
                val li = list.sumOf { db.num(it["litros"]) }
                Consumo(placa, km, li, if (li > 0) km / li else 0.0)
            }.sortedByDescending { it.media }

    fun custoTotalManutencoes(db: DatabaseHelper): Pair<Double, List<Map<String, Any?>>> {
        val list = db.queryAll("manutencoes")
        return list.sumOf { DatabaseHelper.parseMoney(db.str(it["valor_servico"])) } to list
    }

    data class Fluxo(val receitas: Double, val despesas: Double, val saldo: Double)
    fun fluxoCaixa(db: DatabaseHelper, mes: Int, ano: Int): Fluxo {
        var rec = 0.0; var desp = 0.0
        db.queryAll("viagens").forEach { v -> if (noMes(db, db.str(v["data_carga"]), mes, ano)) rec += db.num(v["valor_liquido"]) }
        db.queryAll("manutencoes").forEach { m -> if (noMes(db, db.str(m["data_servico"]), mes, ano)) desp += DatabaseHelper.parseMoney(db.str(m["valor_servico"])) }
        db.queryAll("combustivel").forEach { c -> if (noMes(db, db.str(c["data_registro"]), mes, ano)) desp += db.num(c["valor_total"]) }
        db.queryAll("adiantamentos").forEach { a -> if (noMes(db, db.str(a["data"]), mes, ano)) desp += db.num(a["valor"]) }
        return Fluxo(rec, desp, rec - desp)
    }

    data class ContaReceber(val empresa: String, val nro: String, val dataCarga: String, val valor: Double)
    fun contasReceber(db: DatabaseHelper): List<ContaReceber> =
        db.queryAll("viagens").mapNotNull { v ->
            val sn = db.str(v["saldo_frete_sn"]).uppercase()
            if (sn == "SIM" || sn == "ESPERA" || sn.isEmpty()) {
                var valor = db.num(v["valor_bruto"])
                if (db.str(v["pago_adiantamento_sn"]).uppercase() == "SIM") valor -= db.num(v["pago_adiantamento_val"])
                ContaReceber(
                    empresa = db.str(v["empresa"]), 
                    nro = db.str(v["nro_viagem"]), 
                    dataCarga = db.str(v["data_carga"]), 
                    valor = valor
                )
            } else null
         }
}
