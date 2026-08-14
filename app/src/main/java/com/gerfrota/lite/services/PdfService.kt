package com.gerfrota.lite.services

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.gerfrota.lite.data.DatabaseHelper
import com.gerfrota.lite.core.PathHelper
import java.io.File

/** Writer simples multi-página sobre android.graphics.pdf.PdfDocument. */
class PdfWriter(private val w: Int, private val h: Int, private val margin: Float = 40f) {
    private val doc = PdfDocument()
    private var open: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f
    private val pText = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.BLACK }
    private val pBold = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.BLACK; isFakeBoldText = true }

    private fun newPage() {
        open?.let { doc.finishPage(it) }
        open = doc.startPage(PdfDocument.PageInfo.Builder(w, h, doc.pageCount + 1).create())
        canvas = open!!.canvas; y = margin
    }
    private fun ensure() { if (canvas == null) newPage() }
    private fun quebra() { if (y > h - margin) newPage() }

    fun header(titulo: String, sub: String) {
        ensure(); val c = canvas!!
        c.drawRect(RectF(margin, y, w - margin, y + 44), Paint().apply { color = Color.rgb(38, 50, 56); isAntiAlias = true })
        c.drawText(titulo, margin + 8, y + 18, Paint().apply { isAntiAlias = true; textSize = 14f; color = Color.WHITE; isFakeBoldText = true })
        c.drawText(sub, margin + 8, y + 34, Paint().apply { isAntiAlias = true; textSize = 8f; color = Color.WHITE })
        y += 56
    }
    fun secao(t: String) { ensure(); quebra(); y += 6; canvas!!.drawText(t.uppercase(), margin, y,
        Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(25, 118, 210); isFakeBoldText = true }); y += 14 }
    fun linha(label: String, valor: String) {
        ensure(); quebra()
        canvas!!.drawText("$label: ", margin, y, pBold)
        val wl = pBold.measureText("$label: ")
        textoQuebrado(valor.ifBlank { "Não informado" }, margin + wl, w - margin - (margin + wl))
    }
    fun texto(t: String) { ensure(); textoQuebrado(t, margin, w - margin * 2) }
    private fun textoQuebrado(t: String, x: Float, maxW: Float) {
        var line = ""
        for (word in t.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (pText.measureText(test) > maxW && line.isNotEmpty()) {
                quebra(); canvas!!.drawText(line, x, y, pText); y += 13f; line = word
            } else line = test
        }
        if (line.isNotEmpty()) { quebra(); canvas!!.drawText(line, x, y, pText); y += 13f }
    }
    fun salvar(file: File) {
        ensure(); open?.let { doc.finishPage(it) }; open = null
        file.parentFile?.mkdirs()
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}

object PdfService {
    private const val A4W = 595; private const val A4H = 842
    private const val A6W = 297; private const val A6H = 419

    private fun pasta(ctx: Context, vararg sub: String) =
        File(PathHelper.pastaDocumentosGerFrota(ctx), sub.joinToString("/"))

    // ✅ NOVA FUNÇÃO: Formatação de moeda
    private fun fmt(v: Double) = com.gerfrota.lite.data.DatabaseHelper.fmtBRL(v)

    /** Card A6 de manutenção (salva em CardsManutencao). */
    fun gerarCardA6(ctx: Context, m: Map<String, Any?>, placa: String): File {
        val db = DatabaseHelper.get(ctx)
        val wp = PdfWriter(A6W, A6H, 24f)
        wp.header("Manutenção: $placa", "GerFrotaLite")
        wp.linha("Data", db.str(m["data_servico"])); wp.linha("KM", db.num(m["quilometragem"]).toInt().toString())
        wp.linha("Sistema", db.str(m["sistema"])); wp.linha("Subsistema", db.str(m["subsistema"]))
        wp.linha("Serviço", db.str(m["tipo_servico"])); wp.linha("Prestador", db.str(m["prestador"]))
        wp.linha("Valor", db.str(m["valor_servico"])); wp.linha("Nota", db.str(m["numero_nota"]))
        wp.texto("Obs: ${db.str(m["observacao"]).ifBlank { "-" }}")
        val nome = "Manutencao_${placa}_${db.str(m["data_servico"]).replace("/", "")}.pdf"
        val f = File(PathHelper.pastaCardsManutencao(ctx), nome)
        wp.salvar(f); return f
    }

    /** Ficha A4 do veículo (DocumentosGerFrota/Veiculos). */
    fun gerarFichaVeiculo(ctx: Context, v: Map<String, Any?>): File {
        val db = DatabaseHelper.get(ctx)
        val wp = PdfWriter(A4W, A4H)
        wp.header("FICHA DO VEÍCULO", "Placa: ${db.str(v["placa"])}")
        wp.secao("Dados Gerais")
        wp.linha("Marca/Modelo", "${db.str(v["marca"])} ${db.str(v["modelo"])}"); wp.linha("Cor", db.str(v["cor"]))
        wp.linha("Tipo", db.str(v["tipo_veiculo"])); wp.linha("Carroceria", db.str(v["carroceria"]))
        wp.linha("Ano Fab./Modelo", "${db.str(v["ano_fabricacao"])}/${db.str(v["ano_modelo"])}")
        wp.linha("Renavam", db.str(v["renavam"])); wp.linha("Chassi", db.str(v["chassi"]))
        wp.linha("Venc. Licenciamento", db.str(v["vencimento_licenciamento"]))
        wp.linha("ANTT", db.str(v["antt"])); wp.linha("Venc. ANTT", db.str(v["vencimento_antt"]))
        wp.linha("Qtd. Pneus", db.str(v["quantidade_pneus"])); wp.linha("Observações", db.str(v["observacao"]))
        val f = File(PathHelper.pastaFichasVeiculos(ctx), "${db.str(v["placa"]).uppercase()}_FICHA.pdf")
        wp.salvar(f); return f
    }

    /** Ficha A4 do motorista (DocumentosGerFrota/Motoristas). */
    fun gerarFichaMotorista(ctx: Context, m: Map<String, Any?>): File {
        val db = DatabaseHelper.get(ctx)
        val wp = PdfWriter(A4W, A4H)
        wp.header("FICHA DO MOTORISTA", db.str(m["nome"]))
        wp.secao("Dados Pessoais")
        wp.linha("CPF", db.str(m["cpf"])); wp.linha("RG", db.str(m["rg"]))
        wp.linha("CNH", "${db.str(m["cnh"])} Cat. ${db.str(m["categoria_cnh"])}")
        wp.linha("Venc. CNH", db.str(m["data_vencimento_cnh"]))
        wp.linha("Cert. Cargas", "${db.str(m["certificado_cargas"])} (Venc: ${db.str(m["vencimento_cargas"])})")
        wp.secao("Contato")
        wp.linha("WhatsApp", db.str(m["whatsapp"])); wp.linha("Telefone", db.str(m["telefone"]))
        wp.linha("Endereço", db.str(m["endereco"])); wp.linha("E-mail", db.str(m["email"]))
        wp.secao("Dados Bancários")
        wp.linha("Banco", "${db.str(m["codigo_banco"])} - ${db.str(m["banco"])}")
        wp.linha("Ag/Conta", "${db.str(m["agencia"])}/${db.str(m["conta"])}")
        wp.linha("PIX", db.str(m["chave_pix1"])); wp.linha("Comissão", "${db.str(m["comissao"])}%")
        val nome = db.str(m["nome"]).replace(" ", "_").uppercase()
        val f = File(PathHelper.pastaFichasMotoristas(ctx), "${nome}_FICHA.pdf")
        wp.salvar(f); return f
    }

    /** Acerto de contas A4 (DocumentosGerFrota/AcertosMotoristas). */
    fun gerarAcertoPdf(ctx: Context, nomeMotorista: String, periodo: String,
                       itens: List<Triple<String, String, String>>, totalLiquido: String): File {
        val db = DatabaseHelper.get(ctx)
        val wp = PdfWriter(A4W, A4H)
        wp.header("ACERTO DE CONTAS", "Motorista: $nomeMotorista | Período: $periodo")
        for ((titulo, detalhe, valor) in itens) {
            wp.secao(titulo); wp.texto(detalhe); wp.linha("Comissão líquida", valor)
        }
        wp.secao("TOTAL DAS COMISSÕES LÍQUIDAS"); wp.texto(totalLiquido)
        val nome = nomeMotorista.replace(" ", "_").uppercase()
        val f = File(PathHelper.pastaAcertosMotoristas(ctx), "${nome}_ACERTO.pdf")
        wp.salvar(f); return f
    }

    // ✅ NOVA FUNÇÃO: Relatório de Viagem
    /** Relatório de Viagem A4 (salva em ViagensFretes). */
    fun gerarRelatorioViagem(ctx: android.content.Context, v: Map<String, Any?>): File {
        val db = com.gerfrota.lite.data.DatabaseHelper.get(ctx)
        val wp = PdfWriter(595, 842)
        wp.header("RELATÓRIO DE VIAGEM - ${db.str(v["nro_viagem"])}", "GerFrotaLite")
        wp.linha("Motorista", db.str(v["motorista"]))
        wp.linha("Veículo", "${v["marca"]} ${v["modelo"]} - Placas: ${v["placas"]}")
        wp.linha("Rota", "${v["cidade_partida"]} -> ${v["cidade_destino"]}")
        wp.linha("Empresa / Carga", "${v["empresa"]} / ${v["carga"]}")
        wp.secao("Financeiro")
        wp.linha("Valor Bruto", fmt(db.num(v["valor_bruto"])))
        wp.linha("Total Reembolsos", fmt(db.num(v["resumo_reembolsos"])))
        wp.linha("Total Despesas", fmt(db.num(v["resumo_despesas"])))
        wp.linha("Saldos a Receber", fmt(db.num(v["total_saldos"])))
        wp.linha("VALOR LÍQUIDO", fmt(db.num(v["valor_liquido"])))
        val f = File(PathHelper.pastaViagensFretes(ctx),
            "Viagem_${v["nro_viagem"]}_${db.str(v["data_carga"]).replace("/", "")}.pdf")
        wp.salvar(f); return f
    }

    // ✅ NOVA FUNÇÃO: Mesclar PDFs
    /** Mescla PDFs anexos ao PDF base usando PdfRenderer (100% nativo). */
    fun mesclarPdfs(base: File, anexos: List<File>, out: File) {
        val doc = android.graphics.pdf.PdfDocument()
        var n = 1
        fun add(file: File) {
            val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(fd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width, page.height, n++).create()
                val p = doc.startPage(info); p.canvas.drawBitmap(bmp, 0f, 0f, null); doc.finishPage(p)
                bmp.recycle()
            }
            renderer.close(); fd.close()
        }
        add(base); anexos.forEach { runCatching { add(it) } }
        out.outputStream().use { doc.writeTo(it) }; doc.close()
    }

    /** Compartilha via ACTION_SEND + FileProvider. */
    fun compartilhar(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
    }
}
