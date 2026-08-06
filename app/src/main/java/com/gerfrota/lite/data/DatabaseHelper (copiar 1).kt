// data/DatabaseHelper.kt
package com.gerfrota.lite.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.NumberFormat
import java.util.Locale

class DatabaseHelper private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx, "gerfrotalite.db", null, 6) {

    companion object {
        @Volatile private var inst: DatabaseHelper? = null
        fun get(ctx: Context) = inst ?: DatabaseHelper(ctx.applicationContext).also { inst = it }

        fun fmtBRL(v: Double) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(v)
        fun parseMoney(s: String?): Double {
            if (s.isNullOrBlank()) return 0.0
            var t = s.replace("R$", "").replace(" ", "").trim()
            t = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
            return t.toDoubleOrNull() ?: 0.0
        }
        fun parseDataBR(s: String?): java.util.Calendar? {
            if (s.isNullOrBlank()) return null
            return try {
                val p = s.take(10).split("/")
                if (p.size != 3) null else java.util.Calendar.getInstance().apply {
                    set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt(), 0, 0, 0)
                }
            } catch (e: Exception) { null }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE frota (id INTEGER PRIMARY KEY AUTOINCREMENT, placa TEXT NOT NULL,
            marca TEXT, modelo TEXT, cor TEXT, tipo_veiculo TEXT, ano_fabricacao TEXT, ano_modelo TEXT,
            renavam TEXT, vencimento_licenciamento TEXT, chassi TEXT, antt TEXT, vencimento_antt TEXT,
            carroceria TEXT, quantidade_pneus TEXT, observacao TEXT, caminho_foto_veiculo TEXT,
            caminho_foto_placa TEXT, caminho_foto_crlv TEXT, caminho_foto_antt TEXT)""")
        db.execSQL("""CREATE TABLE motoristas (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL,
            cpf TEXT, data_nascimento TEXT, rg TEXT, data_emissao_rg TEXT, cnh TEXT, categoria_cnh TEXT,
            data_vencimento_cnh TEXT, data_emissao_cnh TEXT, certificado_cargas TEXT, vencimento_cargas TEXT,
            telefone TEXT, whatsapp TEXT, endereco TEXT, email TEXT, contato_urgencia TEXT,
            telefone_urgencia TEXT, comissao TEXT, banco TEXT, codigo_banco TEXT, agencia TEXT, conta TEXT,
            chave_pix1 TEXT, chave_pix2 TEXT, path_foto TEXT, path_cnh TEXT, path_residencia TEXT, path_cargas TEXT)""")
        db.execSQL("""CREATE TABLE unidades_transporte (id INTEGER PRIMARY KEY AUTOINCREMENT,
            veiculo_id INTEGER, reboque1_id INTEGER, reboque2_id INTEGER, reboque3_id INTEGER,
            motorista_id INTEGER, marca_modelo_ano TEXT, modelo TEXT, placas TEXT, motorista TEXT)""")
        db.execSQL("""CREATE TABLE manutencoes (id INTEGER PRIMARY KEY AUTOINCREMENT, veiculo_id INTEGER,
            placa_veiculo TEXT NOT NULL, data_servico TEXT NOT NULL, quilometragem INTEGER NOT NULL,
            sistema TEXT NOT NULL, subsistema TEXT NOT NULL, tipo_servico TEXT, observacao TEXT,
            valor_servico TEXT, prestador TEXT, numero_nota TEXT, caminho_nota_arquivo TEXT)""")
        db.execSQL("""CREATE TABLE viagens (id INTEGER PRIMARY KEY AUTOINCREMENT, unidade_id INTEGER,
            motorista_id INTEGER, nro_viagem TEXT, marca TEXT, modelo TEXT, ano_modelo TEXT, placas TEXT,
            motorista TEXT, data_carga TEXT, data_descarga TEXT, cidade_partida TEXT, cidade_destino TEXT,
            empresa TEXT, carga TEXT, nota_fiscal TEXT, manifesto_doc TEXT, valor_bruto REAL,
            pago_adiantamento_sn TEXT, pago_adiantamento_val REAL, vale_pedagio_sn TEXT, vale_pedagio_val REAL,
            reembolso_estadia_sn TEXT, reembolso_estadia_val REAL, reembolso_carga_sn TEXT, reembolso_carga_val REAL,
            reembolso_chapa_sn TEXT, reembolso_chapa_val REAL, reembolso_doc_sn TEXT, reembolso_doc_val REAL,
            outros_reembolsos_sn TEXT, outros_reembolsos_val REAL, agenciador_sn TEXT, agenciador_val REAL,
            desp_pedagio_sn TEXT, desp_pedagio_val REAL, desp_estadia_sn TEXT, desp_estadia_val REAL,
            desp_carga_sn TEXT, desp_carga_val REAL, desp_chapa_sn TEXT, desp_chapa_val REAL,
            desp_doc_sn TEXT, desp_doc_val REAL, outras_despesas_sn TEXT, outras_despesas_val REAL,
            saldo_frete_sn TEXT, saldo_pedagio_sn TEXT, saldo_estadia_sn TEXT, saldo_carga_sn TEXT,
            saldo_chapa_sn TEXT, saldo_doc_sn TEXT, saldo_outros_sn TEXT, resumo_reembolsos REAL,
            resumo_despesas REAL, total_saldos REAL, valor_liquido REAL, path_manifesto TEXT,
            path_recibo_adiantamento TEXT, qtd_pedagios INTEGER, paths_pedagios TEXT, status TEXT DEFAULT 'pendente')""")
        db.execSQL("""CREATE TABLE combustivel (id INTEGER PRIMARY KEY AUTOINCREMENT, veiculo_id INTEGER,
            id_abastecimento TEXT, id_unidade TEXT, placa_principal TEXT, data_registro TEXT, km_inicial REAL,
            km_final REAL, km_rodado REAL, combustivel TEXT, litros REAL, valor_total REAL, valor_litro REAL,
            posto TEXT, uf TEXT, rota TEXT, observacoes TEXT, consumo_km_l REAL, custo_km REAL,
            nota_fiscal TEXT, path_nota TEXT)""")
        db.execSQL("""CREATE TABLE arla (id INTEGER PRIMARY KEY AUTOINCREMENT, veiculo_id INTEGER,
            id_abastecimento TEXT, id_unidade TEXT, placa_principal TEXT, data_registro TEXT, km_inicial REAL,
            km_final REAL, km_rodado REAL, litros REAL, valor_total REAL, valor_litro REAL, posto TEXT,
            nota_fiscal TEXT, observacoes TEXT, consumo_km_l REAL, custo_km REAL, path_nota TEXT)""")
        db.execSQL("""CREATE TABLE pneus (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo_fogo TEXT, marca TEXT,
            modelo TEXT, medida TEXT, status TEXT, posicao_atual TEXT, veiculo_id TEXT, data_instalacao TEXT,
            km_instalacao REAL, km_atual REAL, valor_compra REAL, profundidade_atual REAL, pressao_atual REAL,
            pressao_recomendada REAL, observacao TEXT)""")
        db.execSQL("""CREATE TABLE pneus_servicos (id INTEGER PRIMARY KEY AUTOINCREMENT, pneu_id INTEGER,
            tipo_servico TEXT, data_servico TEXT, km_servico REAL, custo REAL, fornecedor TEXT, observacao TEXT)""")
        db.execSQL("""CREATE TABLE descartes (id INTEGER PRIMARY KEY AUTOINCREMENT, pneu_id INTEGER,
            data_descarte TEXT, motivo TEXT, km_descarte REAL, observacao TEXT, cpk_final REAL)""")
        db.execSQL("""CREATE TABLE rodizios (id INTEGER PRIMARY KEY AUTOINCREMENT, veiculo_id TEXT,
            pneu_origem_id INTEGER, pneu_destino_id INTEGER, posicao_origem TEXT, posicao_destino TEXT,
            data_rodizio TEXT, km_rodizio REAL, observacoes TEXT)""")
        db.execSQL("""CREATE TABLE comissoes_motorista (id INTEGER PRIMARY KEY AUTOINCREMENT, viagem_id INTEGER,
            motorista_id INTEGER, percentual_comissao REAL, valor_comissao REAL, status_pagamento TEXT,
            data_pagamento TEXT, observacao TEXT)""")
        db.execSQL("""CREATE TABLE adiantamentos (id INTEGER PRIMARY KEY AUTOINCREMENT, motorista_id INTEGER NOT NULL,
            data TEXT NOT NULL, valor REAL NOT NULL)""")
        db.execSQL("""CREATE TABLE acertos_historico (id INTEGER PRIMARY KEY AUTOINCREMENT, motorista_id INTEGER,
            data_inicio TEXT, data_fim TEXT, bruto REAL, descontos REAL, adiantamentos REAL, liquido REAL,
            itens_json TEXT, criado_em TEXT)""")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { /* migrações futuras */ }

    // ---------- utilidades ----------
    fun cursorToMap(c: Cursor): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        for (i in 0 until c.columnCount) m[c.getColumnName(i)] = when (c.getType(i)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
            else -> c.getString(i)
        }
        return m
    }

    fun queryAll(table: String, orderBy: String? = null): List<Map<String, Any?>> {
        readableDatabase.query(table, null, null, null, null, null, orderBy).use { c ->
            return List(c.count) { c.moveToPosition(it); cursorToMap(c) }
        }
    }

    fun insert(table: String, row: Map<String, Any?>): Long = writableDatabase.insert(table, null,
        android.content.ContentValues().apply { row.forEach { (k, v) ->
            when (v) { null -> putNull(k); is Long -> put(k, v); is Int -> put(k, v);
                is Double -> put(k, v); else -> put(k, v.toString()) } } })

    fun update(table: String, id: Long, row: Map<String, Any?>): Int = writableDatabase.update(table,
        android.content.ContentValues().apply { row.forEach { (k, v) ->
            when (v) { null -> putNull(k); is Long -> put(k, v); is Int -> put(k, v);
                is Double -> put(k, v); else -> put(k, v.toString()) } } }, "id = ?", arrayOf("$id"))

    fun delete(table: String, id: Long) = writableDatabase.delete(table, "id = ?", arrayOf("$id"))

    fun num(v: Any?): Double = when (v) { null -> 0.0; is Double -> v; is Long -> v.toDouble();
        is Int -> v.toDouble(); else -> parseMoney(v.toString()) }

    fun str(v: Any?): String = v?.toString() ?: ""

    // ---------- consultas gerenciais (usadas pela UI e pelo RAG) ----------
    fun resumoFinanceiroMes(mes: Int, ano: Int): Triple<Double, Double, Double> {
        var rec = 0.0; var desp = 0.0
        queryAll("viagens").forEach { v ->
            val d = parseDataBR(str(v["data_carga"]))
            if (d != null && d.get(java.util.Calendar.MONTH) + 1 == mes && d.get(java.util.Calendar.YEAR) == ano)
                rec += num(v["valor_liquido"])
        }
        listOf("manutencoes" to "data_servico", "combustivel" to "data_registro", "adiantamentos" to "data")
            .forEach { (tab, col) ->
                queryAll(tab).forEach { r ->
                    val d = parseDataBR(str(r[col]))
                    if (d != null && d.get(java.util.Calendar.MONTH) + 1 == mes && d.get(java.util.Calendar.YEAR) == ano)
                        desp += num(r["valor_servico"] ?: r["valor_total"] ?: r["valor"])
                }
            }
        return Triple(rec, desp, rec - desp)
    }

    fun contasReceber(): List<Map<String, Any?>> = queryAll("viagens").filter {
        val sn = str(it["saldo_frete_sn"]).uppercase()
        sn == "SIM" || sn == "ESPERA" || sn.isEmpty()
    }

    fun gastoCombustivelMes(mes: Int, ano: Int): Map<String, Pair<Double, Double>> {
        val out = LinkedHashMap<String, Pair<Double, Double>>()
        queryAll("combustivel").forEach { c ->
            val d = parseDataBR(str(c["data_registro"]))
            if (d != null && d.get(java.util.Calendar.MONTH) + 1 == mes && d.get(java.util.Calendar.YEAR) == ano) {
                val placa = str(c["placa_principal"])
                val (v, l) = out[placa] ?: (0.0 to 0.0)
                out[placa] = (v + num(c["valor_total"])) to (l + num(c["litros"]))
            }
        }
        return out
    }

    fun alertasVencimento(dias: Int = 30): List<String> {
        val out = mutableListOf<String>(); val hoje = java.util.Calendar.getInstance()
        val limite = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, dias) }
        fun checa(dataStr: String?, descr: String) {
            val d = parseDataBR(dataStr) ?: return
            if (!d.before(hoje) && d.before(limite)) out += "$descr vence em ${dataStr.take(10)}"
            else if (d.before(hoje)) out += "$descr VENCIDO em ${dataStr.take(10)}"
        }
        queryAll("frota").forEach { v ->
            checa(str(v["vencimento_antt"]).ifBlank { null }, "ANTT ${v["placa"]}")
            checa(str(v["vencimento_licenciamento"]).ifBlank { null }, "Licenciamento ${v["placa"]}")
        }
        queryAll("motoristas").forEach { m ->
            checa(str(m["data_vencimento_cnh"]).ifBlank { null }, "CNH ${m["nome"]}")
            checa(str(m["vencimento_cargas"]).ifBlank { null }, "Curso cargas ${m["nome"]}")
        }
        return out
    }

    fun proximoIdAbastecimento(tabela: String): String {
        val maior = queryAll(tabela).maxOfOrNull { str(it["id_abastecimento"]).toIntOrNull() ?: 0 } ?: 0
        return (maior + 1).toString().padStart(5, '0')
    }
}
