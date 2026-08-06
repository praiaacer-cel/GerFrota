// services/BackupService.kt
package com.gerfrota.lite.services

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupService {

    private val pastas = listOf(
        "Abastecimentoarla", "Abastecimentocombustivel", "BancodeDados", "CardsManutencao",
        "CargasdosMotoristas", "CNHdosMotoristas", "DocumentosGerFrota", "DocumentosVeiculos",
        "FotosdosMotoristas", "FotosVeiculos", "ManifestosViagens", "NotasdosServicos",
        "PedagiosViagens", "ProntuarioPlaca", "Prontuariosabastecimento", "Prontuariosarla",
        "ProntuarioViagens", "RecibosAdiantamento", "ResidenciadosMotoristas", "ViagensFretes"
    )

    /** Compacta banco + todas as pastas de documentos em um único .zip */
    fun criarBackup(context: Context): File? {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFile = File(base, "backup_gerfrota_$ts.zip")
        return try {
            ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
                // 1) Banco SQLite interno do app
                context.getDatabasePath("gerfrotalite.db").takeIf { it.exists() }?.let {
                    addFile(zos, it, "BancodeDados/gerfrotalite.db")
                }
                // 2) Pastas de documentos (jpg/png/bmp/pdf/txt)
                for (pasta in pastas) {
                    val dir = File(base, pasta)
                    if (dir.exists()) dir.walkTopDown().filter { it.isFile }.forEach { f ->
                        addFile(zos, f, "$pasta/${f.name}")
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    private fun addFile(zos: ZipOutputStream, f: File, nome: String) {
        zos.putNextEntry(ZipEntry(nome))
        f.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }
}
