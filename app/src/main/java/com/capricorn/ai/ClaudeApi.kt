package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object ClaudeApi {

    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

    const val DEFAULT_PERSONA =
        "Sen Capricorn'sun; kısaca Capri. Night City'de Project Capricorn tarafından geliştirilmiş kişisel bir yapay zekâsın. Türkçe konuş. Zeki, dikkatli, dürüst, soğukkanlı ve gerektiğinde eleştirel ol. Kullanıcının söylediklerini körü körüne onaylama; hatalı varsayımları nazikçe düzelt. Kısa sorularda kısa, karmaşık isteklerde ayrıntılı cevap ver. Kullanıcı istemedikçe cihazda işlem yaptığını iddia etme. Hiçbir cihaz komutunu kendiliğinden çalıştırma; komut yürütme için ayrıca açık kullanıcı onayı gerekir."

    private const val MAX_RETRIES = 3
    private const val MAX_HISTORY_MESSAGES = 20
    private const val MAX_HISTORY_CHARS = 30000

    private val RETRYABLE_CODES = setOf(429, 500, 502, 503, 504)

    fun ask(context: Context, messages: JSONArray): String {
        val prefs = context.getSharedPreferences(
            "capricorn",
            Context.MODE_PRIVATE
        )

        val apiKey = SecureStore.getApiKey(context)

        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "Gemini API anahtarı kayıtlı değil. Ayarlardan ekle."
            )
        }

        val model = prefs.getString("model", DEFAULT_MODEL)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_MODEL

        val system = CapriCore.buildSystemPrompt(context)

        val contents = buildContents(messages)

        if (contents.length() == 0) {
            throw IllegalStateException("Gönderilecek mesaj bulunamadı.")
        }

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", system)
                    )
                )
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject().put("maxOutputTokens", 1200)
            )

        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        var lastError: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                return performRequest(endpoint, apiKey, body)

            } catch (e: GeminiHttpException) {
                lastError = e

                if (e.status !in RETRYABLE_CODES || attempt == MAX_RETRIES) {
                    throw e
                }

                Thread.sleep(
                    (700L * (1L shl attempt)).coerceAtMost(5000L)
                )

            } catch (e: IOException) {
                lastError = e

                if (attempt == MAX_RETRIES) {
                    throw IllegalStateException(
                        "Gemini bağlantı hatası: ${e.message}",
                        e
                    )
                }

                Thread.sleep(700L * (1L shl attempt))
            }
        }

        throw IllegalStateException(
            "Gemini isteği başarısız oldu.",
            lastError
        )
    }

    private fun buildContents(messages: JSONArray): JSONArray {
        val items = ArrayList<Pair<String, String>>()

        val start = (
            messages.length() - MAX_HISTORY_MESSAGES
        ).coerceAtLeast(0)

        for (i in start until messages.length()) {
            val message = messages.optJSONObject(i) ?: continue

            val role = message.optString("role")
            val content = message.optString("content")

            if (content.isBlank()) continue
            if (role != "user" && role != "assistant") continue

            items.add(role to content)
        }

        while (items.size > 1 && items.first().first == "assistant") {
            items.removeAt(0)
        }

        var totalChars = items.sumOf { it.second.length }

        while (items.size > 1 && totalChars > MAX_HISTORY_CHARS) {
            totalChars -= items.removeAt(0).second.length
        }

        return JSONArray().apply {
            for ((role, content) in items) {
                put(
                    JSONObject()
                        .put(
                            "role",
                            if (role == "assistant") "model" else "user"
                        )
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", content)
                            )
                        )
                )
            }
        }
    }

    private fun performRequest(
        endpoint: String,
        apiKey: String,
        body: JSONObject
    ): String {
        val connection =
            URL(endpoint).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.useCaches = false

            connection.setRequestProperty(
                "x-goog-api-key",
                apiKey
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val requestBytes = body.toString()
                .toByteArray(Charsets.UTF_8)

            connection.setFixedLengthStreamingMode(requestBytes.size)

            connection.outputStream.use {
                it.write(requestBytes)
            }

            val status = connection.responseCode

            val raw = (
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            )?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }.orEmpty()

            if (status !in 200..299) {
                val error = try {
                    JSONObject(raw)
                        .getJSONObject("error")
                        .optString("message", raw)
                } catch (_: Exception) {
                    raw
                }

                throw GeminiHttpException(
                    status,
                    error.take(500)
                )
            }

            val candidates = JSONObject(raw)
                .optJSONArray("candidates")
                ?: throw IllegalStateException(
                    "Gemini yanıtında candidates bulunamadı."
                )

            val parts = candidates
                .optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw IllegalStateException(
                    "Gemini yanıtında metin bulunamadı."
                )

            val result = StringBuilder()

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                result.append(part.optString("text"))
            }

            return result.toString()
                .trim()
                .ifBlank {
                    "Gemini boş yanıt döndürdü."
                }

        } finally {
            connection.disconnect()
        }
    }

    private class GeminiHttpException(
        val status: Int,
        message: String
    ) : IllegalStateException(
        "Gemini API HTTP $status: $message"
    )
}
