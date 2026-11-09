package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ClaudeApi {
    const val DEFAULT_MODEL = "claude-sonnet-5-5"
    const val DEFAULT_PERSONA = "Sen Capricorn'sun; kısaca Capri. Night City'de Project Capricorn tarafından geliştirilmiş kişisel bir yapay zekâsın. Türkçe konuş. Zeki, dikkatli, dürüst, soğukkanlı ve gerektiğinde eleştirel ol. Kullanıcının söylediklerini körü körüne onaylama; hatalı varsayımları nazikçe düzelt. Kısa sorularda kısa, karmaşık isteklerde ayrıntılı cevap ver. Kullanıcı istemedikçe cihazda işlem yaptığını iddia etme. Hiçbir cihaz komutunu kendiliğinden çalıştırma; komut yürütme için ayrıca açık kullanıcı onayı gerekir."

    fun ask(context: Context, messages: JSONArray): String {
        val prefs = context.getSharedPreferences("capricorn", Context.MODE_PRIVATE)
        val apiKey = SecureStore.getApiKey(context)
        if (apiKey.isBlank()) throw IllegalStateException("API anahtarı kayıtlı değil. Ayarlar bölümünden Anthropic API anahtarını ekle.")
        val model = prefs.getString("model", DEFAULT_MODEL)?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_MODEL
        val system = prefs.getString("persona", DEFAULT_PERSONA)?.takeUnless { it.isNullOrBlank() } ?: DEFAULT_PERSONA
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1200)
            .put("system", system)
            .put("messages", messages)

        val connection = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            setRequestProperty("content-type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try { JSONObject(raw).getJSONObject("error").optString("message", raw) } catch (_: Exception) { raw }
                throw IllegalStateException("API HTTP $status: ${message.take(500)}")
            }
            val content = JSONObject(raw).optJSONArray("content") ?: throw IllegalStateException("API yanıtında content bulunamadı.")
            val result = StringBuilder()
            for (i in 0 until content.length()) {
                val item = content.optJSONObject(i) ?: continue
                if (item.optString("type") == "text") result.append(item.optString("text"))
            }
            return result.toString().trim().ifBlank { "Model boş yanıt döndürdü." }
        } finally {
            connection.disconnect()
        }
    }
}
