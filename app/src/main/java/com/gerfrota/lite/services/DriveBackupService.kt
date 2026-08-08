package com.gerfrota.lite.services

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/** Gera o ZIP local e, se houver token Google, envia ao Drive. */
object DriveBackupService {
    
    suspend fun backupCompleto(ctx: Context, prefs: SharedPreferences): String {
        // 1. Gera o ZIP local
        val zip = BackupService.criarBackup(ctx) ?: return "Falha ao gerar o ZIP de backup."
        
        // 2. Busca o token salvo nas preferências (evita depender da Activity/GoogleAuthHelper)
        val token = prefs.getString("conta_token", null)
            ?: return "Backup local gerado: ${zip.name}"
            
        // 3. Tenta enviar para o Drive e retorna o status
        return if (DriveUploader.upload(zip, token) != null) {
            val email = prefs.getString("conta_email", "")
            "Backup enviado ao Drive de $email ✓"
        } else {
            "ZIP gerado, mas o envio ao Drive falhou."
        }
    }
}
