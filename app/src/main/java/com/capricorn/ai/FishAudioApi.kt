package com.capricorn.ai

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object FishAudioApi {
    const val VOICE_ID = "fdef76c9b2e741119d817754bd8b5dd0"
    private const val MODEL = "s2.1-pro-free"
    private const val ENDPOINT = "https://api.fish.audio/v1/tts"

    fun synthesize(context: Context, text: String): File {
        val apiKey = SecureStore.getFishApiKey(context)
        require(apiKey.isNotBlank()) { "Fish Audio API anahtarı ayarlanmamış." }

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("model", MODEL)
        }

        try {
            val body = JSONObject()
                .put("text", text.take(2500))
                .put("reference_id", VOICE_ID)
                .put("format", "mp3")

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "HTTP $code"
                throw IllegalStateException("Fish Audio hatası ($code): ${error.take(300)}")
            }

            val file = File(context.cacheDir, "capri_voice.mp3")
            connection.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            require(file.length() > 0) { "Fish Audio boş ses dosyası döndürdü." }
            return file
        } finally {
            connection.disconnect()
        }
    }
}
