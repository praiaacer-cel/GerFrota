package com.gerfrota.lite.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileNotFoundException

object GerFrotaStorage {

    private const val DB_DIR = "BancodeDados"
    private const val DB_NAME = "gerfrotalite.db"

    const val DIR_FOTOS_VEICULOS = "FotosVeiculos"
    const val DIR_CNH_MOTORISTAS = "CNHdosMotoristas"
    const val DIR_PRONTUARIO_PLACA = "ProntuarioPlaca"

    /**
     * Retorna:
     * /Android/data/com.gerfrota.lite/files/
     */
    fun baseDir(context: Context): File {
        return context.getExternalFilesDir(null)
            ?: throw IllegalStateException("Não foi possível obter getExternalFilesDir(null)")
    }

    /**
     * Retorna:
     * /Android/data/com.gerfrota.lite/files/BancodeDados/gerfrotalite.db
     */
    fun dbFile(context: Context): File {
        val base = baseDir(context)
        return File(base, "$DB_DIR/$DB_NAME")
    }

    fun fotosVeiculosDir(context: Context): File {
        val dir = File(baseDir(context), DIR_FOTOS_VEICULOS)
        dir.mkdirs()
        return dir
    }

    fun cnhMotoristasDir(context: Context): File {
        val dir = File(baseDir(context), DIR_CNH_MOTORISTAS)
        dir.mkdirs()
        return dir
    }

    fun prontuarioPlacaDir(context: Context): File {
        val dir = File(baseDir(context), DIR_PRONTUARIO_PLACA)
        dir.mkdirs()
        return dir
    }

    fun ensureBaseDirs(context: Context) {
        val base = baseDir(context)

        File(base, DB_DIR).mkdirs()
        File(base, DIR_FOTOS_VEICULOS).mkdirs()
        File(base, DIR_CNH_MOTORISTAS).mkdirs()
        File(base, DIR_PRONTUARIO_PLACA).mkdirs()
    }

    /**
     * Abre o banco externo.
     * Se o banco não existir, lança erro para facilitar diagnóstico.
     */
    fun openDatabase(context: Context): SQLiteDatabase {
        val file = dbFile(context)

        if (!file.exists()) {
            throw FileNotFoundException(
                "Banco externo não encontrado: ${file.absolutePath}"
            )
        }

        return SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    }
}
