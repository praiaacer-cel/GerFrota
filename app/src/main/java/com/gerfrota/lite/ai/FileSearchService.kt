// ai/FileSearchService.kt
package com.gerfrota.lite.ai

import android.content.Context
import java.io.File

/** Busca nos arquivos jpg/png/bmp/pdf/txt das pastas do GerFrotaLite. */
class FileSearchService(private val context: Context) {

    val baseDir: File get() = context.getExternalFilesDir(null) ?: context.filesDir

    val pastas = listOf(
        "FotosVeiculos", "DocumentosVeiculos", "CNHdosMotoristas", "FotosdosMotoristas",
        "ResidenciadosMotoristas", "CargasdosMotoristas", "NotasdosServicos", "CardsManutencao",
        "ManifestosViagens", "PedagiosViagens", "RecibosAdiantamento", "ViagensFretes",
        "ProntuarioPlaca", "Prontuariosabastecimento", "Prontuariosarla", "ProntuarioViagens",
        "Abastecimentocombustivel", "Abastecimentoarla", "DocumentosGerFrota"
    )

    private val extMidia = listOf("jpg", "jpeg", "png", "bmp", "pdf")

    fun arquivos(contendo: String): List<File> {
        val chave = contendo.uppercase().replace(" ", "_")
        val out = mutableListOf<File>()
        for (p in pastas) {
            val dir = File(baseDir, p)
            dir.listFiles()?.forEach { f ->
                if (f.name.uppercase().contains(chave)) out += f
            }
        }
        return out
    }

    fun documentosPlaca(placa: String): String {
        val fs = arquivos(placa).filter { it.extension.lowercase() in extMidia || it.extension == "txt" }
        if (fs.isEmpty()) return "Nenhum arquivo físico encontrado para a placa $placa."
        return "ARQUIVOS DA PLACA $placa:\n" + fs.joinToString("\n") { "• ${it.parentFile?.name}/${it.name}" }
    }

    fun documentosPessoa(nome: String): String {
        val chave = nome.uppercase().replace(" ", "_")
        val fs = mutableListOf<File>()
        for (p in listOf("CNHdosMotoristas", "FotosdosMotoristas", "ResidenciadosMotoristas",
            "CargasdosMotoristas", "DocumentosGerFrota")) {
            File(baseDir, p).listFiles()?.forEach { f ->
                if (f.name.uppercase().contains(chave) || f.name.uppercase().contains(nome.uppercase())) fs += f
            }
        }
        if (fs.isEmpty()) return "Nenhum arquivo físico encontrado para $nome."
        return "ARQUIVOS DE ${nome.uppercase()}:\n" + fs.joinToString("\n") { "• ${it.parentFile?.name}/${it.name}" }
    }

    /** Lê prontuários .txt (históricos) para alimentar o RAG. */
    fun lerProntuario(placa: String, maxChars: Int = 1200): String? {
        val f = File(File(baseDir, "ProntuarioPlaca"), "${placa}_prontuario.txt")
        if (!f.exists()) return null
        return try { f.readText().take(maxChars) } catch (e: Exception) { null }
    }

    fun lerTxtPasta(pasta: String, nomeContem: String, maxChars: Int = 900): String? {
        val dir = File(baseDir, pasta)
        val f = dir.listFiles()?.firstOrNull { it.name.contains(nomeContem, true) && it.extension == "txt" }
            ?: return null
        return try { f.readText().take(maxChars) } catch (e: Exception) { null }
    }
}
