package com.gerfrota.lite.core

import android.content.Context
import java.io.File

fun String.sanitized() = replace(Regex("[\\\\/:*?\"<>|]"), "")

object PathHelper {
    /** Diretório base do app (mesmo padrão dos blocos anteriores). */
    fun base(ctx: Context): File = ctx.getExternalFilesDir(null) ?: ctx.filesDir

    fun pasta(ctx: Context, nome: String): File = File(base(ctx), nome).apply { mkdirs() }

    fun prontuarioPlaca(ctx: Context, placa: String) =
        File(pasta(ctx, "ProntuarioPlaca"), "${placa}_prontuario.txt")

    fun prontuarioAbastecimento(ctx: Context, placa: String) =
        File(pasta(ctx, "Prontuariosabastecimento"), "Abastecimento_${placa.sanitized()}.txt")

    fun prontuarioArla(ctx: Context, placa: String) =
        File(pasta(ctx, "Prontuariosarla"), "ARLA_${placa.sanitized()}.txt")

    fun abastecimentoCombustivel(ctx: Context) = pasta(ctx, "Abastecimentocombustivel")
    fun abastecimentoArla(ctx: Context) = pasta(ctx, "Abastecimentoarla")
}
