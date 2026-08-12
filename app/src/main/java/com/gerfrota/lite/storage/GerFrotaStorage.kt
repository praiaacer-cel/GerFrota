package com.gerfrota.lite.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.gerfrota.lite.core.PathHelper
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
    fun dbFile(context: Context): File = File(PathHelper.base(context), "$DB_DIR/$DB_NAME")
    fun baseDir(context: Context): File = PathHelper.base(context)          // ✅ não lança mais exceção
    fun fotosVeiculosDir(context: Context): File = PathHelper.pastaFotosVeiculos(context)
    fun cnhMotoristasDir(context: Context): File = PathHelper.pastaCNH(context)
    fun prontuarioPlacaDir(context: Context): File = PathHelper.pasta(context, "ProntuarioPlaca")
    fun ensureBaseDirs(context: Context) {
        PathHelper.base(context); PathHelper.pastaFotosVeiculos(context)
        PathHelper.pastaCNH(context); PathHelper.pasta(context, "ProntuarioPlaca")
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
