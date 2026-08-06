package com.gerfrota.lite.ui.login

import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gerfrota.lite.services.GoogleAuthHelper

@Composable
fun LoginScreen(prefs: SharedPreferences, onLogin: () -> Unit) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val auth = remember { GoogleAuthHelper(ctx as android.app.Activity) }

    val google = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) {
            prefs.edit().putString("conta_email",
                com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(ctx)?.email)
                .putBoolean("logado", true).apply()
            // Token é obtido sob demanda pelo DriveBackupService
            onLogin()
        }
    }

    Scaffold { pad ->
        Column(Modifier.padding(pad).padding(24.dp)) {
            Spacer(Modifier.height(60.dp))
            Text("GerFrotaLite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Gestão de Frota 100% offline", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(senha, { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                prefs.edit().putString("conta_email", email).putBoolean("logado", true).apply(); onLogin()
            }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("ENTRAR") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { runCatching { google.launch(auth.signInIntent()) } },
                modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("ENTRAR COM GOOGLE (Backup Drive)") }
        }
    }
}
