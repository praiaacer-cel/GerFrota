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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(prefs: SharedPreferences, onLogin: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val auth = remember { GoogleAuthHelper(ctx as android.app.Activity) }

    val google = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) scope.launch {
            val token = auth.tokenDrive()
            if (token != null) {
                prefs.edit().putString("conta_email",
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(ctx)?.email)
                    .putString("conta_token", token).putBoolean("logado", true).apply()
                onLogin()
            }
        }
    }

    Scaffold { pad ->
        Column(Modifier.padding(pad).padding(24.dp)) {
            Text("GerFrota", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(senha, { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                prefs.edit().putString("conta_email", email).putBoolean("logado", true).apply(); onLogin()
            }, modifier = Modifier.fillMaxWidth()) { Text("ENTRAR") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { google.launch(auth.signInIntent()) },
                modifier = Modifier.fillMaxWidth()) { Text("ENTRAR COM GOOGLE (Backup Drive)") }
        }
    }
}
