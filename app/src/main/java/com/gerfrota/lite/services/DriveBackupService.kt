package com.gerfrota.lite.services

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object DriveBackupService {
    /** Gera o ZIP e, se houver conta Google, envia ao Drive. Retorna mensagem de status. */
    suspend fun backupCompleto(ctx: Context, prefs: SharedPreferences): String {
        val zip = BackupService.criarBackupCompactado(ctx) ?: return "Falha ao gerar o ZIP de backup."
        val email = prefs.getString("conta_email", null) ?: return "Backup local gerado: ${zip.name}"
        val token = GoogleAuthHelper(ctx as android.app.Activity).tokenDrive()
            ?: return "Backup local gerado. Faça login com Google para enviar ao Drive."
        return if (DriveUploader.upload(zip, token) != null) "Backup enviado ao Drive de $email ✓"
               else "ZIP gerado, mas o envio ao Drive falhou."
    }
}
