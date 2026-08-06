package com.gerfrota.lite.services

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthHelper(private val activity: Activity) {
    companion object {
        const val RC_SIGN_IN = 9001
        const val SCOPE_DRIVE = "oauth2:https://www.googleapis.com/auth/drive.file"
    }
    private val client = GoogleSignIn.getClient(activity,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build())

    fun signInIntent(): Intent = client.signInIntent

    /** Token de acesso ao Drive (após login). Chamar em background. */
    suspend fun tokenDrive(): String? = withContext(Dispatchers.IO) {
        val acc = GoogleSignIn.getLastSignedInAccount(activity) ?: return@withContext null
        try { GoogleAuthUtil.getToken(activity, acc.account!!, SCOPE_DRIVE) } catch (e: Exception) { null }
    }
}
