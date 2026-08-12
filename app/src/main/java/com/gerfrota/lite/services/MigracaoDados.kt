// services/MigracaoDados.kt
package com.gerfrota.lite.services

import android.content.Context
import com.gerfrota.lite.core.PathHelper
import java.io.File

/**
 * Se o usuário copiou a estrutura do GerFrotaLite Flutter para
 * /sdcard/Android/data/com.gerfrota.lite/files/BancodeDados/gerfrotalite.db,
 * importa o banco na primeira execução (mantém frota, fretes, pneus etc.).
 */
object MigracaoDados {
    fun importarSeNecessario(context: Context) {
        val base = PathHelper.base(context)   // ✅ era getExternalFilesDir(null) ?: return
        val origem = File(base, "BancodeDados/gerfrotalite.db")
        val destino = context.getDatabasePath("gerfrotalite.db")
        if (origem.exists() && origem.length() > 0 && !destino.exists()) {
            destino.parentFile?.mkdirs()
            origem.copyTo(destino, overwrite = true)
        }
    }
}
