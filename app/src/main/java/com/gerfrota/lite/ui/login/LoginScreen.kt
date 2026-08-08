package com.gerfrota.lite.ui.login

import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        if (r.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                // Obtém o token de forma assíncrona para não travar a UI
                val token = auth.tokenDrive()
                prefs.edit()
                    .putString("conta_email", com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(ctx)?.email)
                    .putString("conta_token", token)
                    .putBoolean("logado", true)
                    .apply()
                onLogin()
            }
        }
    }

    Scaffold { pad ->
        Column(Modifier.padding(pad).padding(24.dp)) {
            Spacer(Modifier.height(60.dp))
            
            Text(
                text = "GerFrotaLite", 
                fontSize = 28.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF0D47A1) // Cor corporativa
            )
            Text(
                text = "Gestão de Frota 100% offline", 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
            )
            
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("E-mail") }, 
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = senha, 
                onValueChange = { senha = it }, 
                label = { Text("Senha") }, 
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    prefs.edit()
                        .putString("conta_email", email)
                        .putBoolean("logado", true)
                        .apply()
                    onLogin() 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { 
                Text("ENTRAR") 
            }
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { runCatching { google.launch(auth.signInIntent()) } },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { 
                Text("ENTRAR COM GOOGLE (Backup Drive)") 
            }
        }
    }
}
