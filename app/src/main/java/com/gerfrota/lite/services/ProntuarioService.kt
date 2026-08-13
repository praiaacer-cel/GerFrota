package com.gerfrota.lite.services

import android.content.Context
import com.gerfrota.lite.core.PathHelper
import com.gerfrota.lite.core.sanitized
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.data.abastecimentosPorPlaca

object ProntuarioService {

    fun exportarCombustivel(ctx: Context, db: DatabaseHelper, placa: String, marcaModelo: String) {
        val todos = db.abastecimentosPorPlaca("combustivel", placa)
            .sortedBy { db.str(it["id_abastecimento"]) }
        val f = PathHelper.prontuarioAbastecimento(ctx, placa)
        if (todos.isEmpty()) { f.delete(); return }
        val sb = StringBuilder()
        sb.appendLine("=== HISTÓRICO DE ABASTECIMENTOS - PLACA: $placa ===")
        sb.appendLine("VEÍCULO: $marcaModelo")
        sb.appendLine("===================================================================\n")
        for (r in todos) {
            sb.appendLine("ID ABASTECIMENTO: ${r["id_abastecimento"]} | DATA: ${r["data_registro"]}")
            sb.appendLine("POSTO: ${r["posto"] ?: "-"} (${r["uf"] ?: "-"}) | COMBUSTÍVEL: ${r["combustivel"] ?: "-"}")
            sb.appendLine("LITROS: ${"%.2f".format(db.num(r["litros"]))} | R\$/L: ${"%.2f".format(db.num(r["valor_litro"]))} | TOTAL: ${DatabaseHelper.fmtBRL(db.num(r["valor_total"]))}")
            sb.appendLine("KM INICIAL: ${"%.1f".format(db.num(r["km_inicial"]))} | KM ATUAL: ${"%.1f".format(db.num(r["km_final"]))} | KM RODADO: ${"%.1f".format(db.num(r["km_rodado"]))}")
            sb.appendLine("CONSUMO: ${r["consumo_km_l"]?.let { "%.2f".format(db.num(it)) + " Km/L" } ?: "-"} | CUSTO: ${r["custo_km"]?.let { DatabaseHelper.fmtBRL(db.num(it)) + " /Km" } ?: "-"}")
            if (!db.str(r["nota_fiscal"]).isBlank()) sb.appendLine("NOTA FISCAL: ${r["nota_fiscal"]}")
            if (!db.str(r["path_nota"]).isBlank()) sb.appendLine("ANEXO NF: ${r["path_nota"]}")
            if (!db.str(r["rota"]).isBlank()) sb.appendLine("ROTA: ${r["rota"]}")
            if (!db.str(r["observacoes"]).isBlank()) sb.appendLine("OBS: ${r["observacoes"]}")
            sb.appendLine("-------------------------------------------------------------------\n")
        }
        f.writeText(sb.toString())
    }

    fun exportarArla(ctx: Context, db: DatabaseHelper, placa: String, marcaModelo: String) {
        val todos = db.abastecimentosPorPlaca("arla", placa)
            .sortedBy { db.str(it["id_abastecimento"]) }
        val f = PathHelper.prontuarioArla(ctx, placa)
        if (todos.isEmpty()) { f.delete(); return }
        val sb = StringBuilder()
        sb.appendLine("=== HISTÓRICO DE ARLA - PLACA: $placa ===")
        sb.appendLine("VEÍCULO: $marcaModelo")
        sb.appendLine("===================================================================\n")
        for (r in todos) {
            sb.appendLine("ID ARLA: ${r["id_abastecimento"]} | DATA: ${r["data_registro"]}")
            sb.appendLine("POSTO: ${r["posto"] ?: "-"} | NF: ${r["nota_fiscal"] ?: "-"}")
            sb.appendLine("LITROS: ${"%.2f".format(db.num(r["litros"]))} | R\$/L: ${"%.2f".format(db.num(r["valor_litro"]))} | TOTAL: ${DatabaseHelper.fmtBRL(db.num(r["valor_total"]))}")
            sb.appendLine("KM INICIAL: ${"%.1f".format(db.num(r["km_inicial"]))} | KM ATUAL: ${"%.1f".format(db.num(r["km_final"]))} | KM RODADO: ${"%.1f".format(db.num(r["km_rodado"]))}")
            sb.appendLine("CONSUMO: ${r["consumo_km_l"]?.let { "%.2f".format(db.num(it)) + " Km/L" } ?: "-"} | CUSTO: ${r["custo_km"]?.let { DatabaseHelper.fmtBRL(db.num(it)) + " /Km" } ?: "-"}")
            if (!db.str(r["observacoes"]).isBlank()) sb.appendLine("OBS: ${r["observacoes"]}")
            sb.appendLine("-------------------------------------------------------------------\n")
        }
        f.writeText(sb.toString())
    }
}
