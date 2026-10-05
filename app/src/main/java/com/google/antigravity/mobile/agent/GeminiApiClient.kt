package com.google.antigravity.mobile.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GeminiFunctionCall(
    val name: String,
    val args: JSONObject
)

data class GeminiResponse(
    val text: String?,
    val functionCalls: List<GeminiFunctionCall>,
    val rawJson: JSONObject
)

class GeminiApiClient(
    private val apiKey: String? = null,
    private val oauthToken: String? = null,
    private val model: String = "gemini-2.5-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        history: JSONArray,
        systemInstruction: String,
        toolsDeclarations: JSONArray? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val url = if (!apiKey.isNullOrBlank()) {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        } else {
            // Authorized via Google OAuth 2.0 Bearer token
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        }

        val bodyJson = JSONObject().apply {
            put("contents", history)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            if (toolsDeclarations != null && toolsDeclarations.length() > 0) {
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", toolsDeclarations)
                }))
            }
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(mediaType))

        // If authenticated via Google Account, attach Bearer OAuth token
        if (!oauthToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $oauthToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("Gemini API Error (${response.code}): $responseBody")
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResponse(
                    text = "No candidate response returned from model.",
                    functionCalls = emptyList(),
                    rawJson = json
                )
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val functionCalls = mutableListOf<GeminiFunctionCall>()
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        functionCalls.add(
                            GeminiFunctionCall(
                                name = fc.getString("name"),
                                args = fc.optJSONObject("args") ?: JSONObject()
                            )
                        )
                    }
                }
            }

            GeminiResponse(
                text = textBuilder.toString().ifBlank { null },
                functionCalls = functionCalls,
                rawJson = json
            )
        }
    }
}
