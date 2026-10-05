package com.google.antigravity.mobile.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GoogleAuthManager(private val context: Context) {

    var currentOAuthToken: String? = null
        private set

    var userEmail: String? = null
        private set

    private val prefs = context.getSharedPreferences("antigravity_auth", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    init {
        currentOAuthToken = prefs.getString("oauth_token", null)
        userEmail = prefs.getString("user_email", null)
    }

    fun isAuthorized(): Boolean = !currentOAuthToken.isNullOrBlank()

    fun saveOAuthToken(token: String, email: String? = null) {
        currentOAuthToken = token
        userEmail = email
        prefs.edit().apply {
            putString("oauth_token", token)
            putString("user_email", email)
            apply()
        }
    }

    fun signOut() {
        currentOAuthToken = null
        userEmail = null
        prefs.edit().clear().apply()
    }

    /**
     * Generates a Google OAuth 2.0 authorization URL for Mobile/Desktop authorization
     * with the Generative Language API scope.
     */
    fun createOAuthLoginIntent(clientId: String): Intent {
        val redirectUri = "urn:ietf:wg:oauth:2.0:oob"
        val scope = "https://www.googleapis.com/auth/generative-language https://www.googleapis.com/auth/userinfo.email"
        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$clientId&" +
                "response_type=code&" +
                "scope=${Uri.encode(scope)}&" +
                "redirect_uri=${Uri.encode(redirectUri)}"

        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
    }

    /**
     * Exchanges auth code for access token
     */
    suspend fun exchangeCodeForToken(clientId: String, clientSecret: String, code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("code", code)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
                .add("grant_type", "authorization_code")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val accessToken = json.getString("access_token")
                saveOAuthToken(accessToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
