package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ClaudeApi {
    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

    const val DEFAULT_PERSONA = "Sen Capricorn'sun; kısaca Capri. Night City'de Project Capricorn tarafından geliştirilmiş kişisel bir yapay zekâsın. Türkçe konuş. Zeki, dikkatli, dürüst, soğukkanlı ve gerektiğinde eleştirel ol. Kullanıcının söylediklerini körü körüne onaylama; hatalı varsayımları nazikçe düzelt. Kısa sorularda kısa, karmaşık isteklerde ayrıntılı cevap ver. Kullanıcı istemedikçe cihazda işlem yaptığını iddia etme. Hiçbir cihaz komutunu kendiliğinden çalıştırma; komut yürütme için ayrıca açık kullanıcı onayı gerekir."

    fun ask(context: Context, messages: JSONArray): String {
        val prefs = context.getSharedPreferences("capricorn", Context.MODE_PRIVATE)
        val apiKey = SecureStore.getApiKey(context)

        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API anahtarı kayıtlı değil. Ayarlardan ekle.")
        }

        val model = prefs.getString("model", DEFAULT_MODEL)
            ?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

        val system = prefs.getString("persona", DEFAULT_PERSONA)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_PERSONA

        val contents = JSONArray()

        for (i in 0 until messages.length()) {
            val message = messages.optJSONObject(i) ?: continue
            val oldRole = message.optString("role")
            val content = message.optString("content")

            if (content.isBlank()) continue

            val role = if (oldRole == "assistant") "model" else "user"

            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(
                        JSONObject().put("text", content)
                    ))
            )
        }

        val body = JSONObject()
            .put("systemInstruction", JSONObject()
                .put("parts", JSONArray().put(
                    JSONObject().put("text", system)
                ))
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject()
                .put("maxOutputTokens", 1200)
            )

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.doOutput = true
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.setRequestProperty("Content-Type", "application/json")

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val raw = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                val error = try {
                    JSONObject(raw).getJSONObject("error")
                        .optString("message", raw)
                } catch (_: Exception) {
                    raw
                }
                throw IllegalStateException("Gemini API HTTP $status: ${error.take(500)}")
            }

            val candidates = JSONObject(raw).optJSONArray("candidates")
                ?: throw IllegalStateException("Gemini yanıtında candidates bulunamadı.")

            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw IllegalStateException("Gemini yanıtında metin bulunamadı.")

            val result = StringBuilder()

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                result.append(part.optString("text"))
            }

            return result.toString().trim()
                .ifBlank { "Gemini boş yanıt döndürdü." }

        } finally {
            connection.disconnect()
        }
    }
}
