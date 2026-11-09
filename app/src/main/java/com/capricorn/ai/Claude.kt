package com.capricorn.ai

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Theme {
    val CY = 0xFF00E5FF.toInt()
    val MG = 0xFFFF2E63.toInt()
    val YL = 0xFFFFE600.toInt()
    val BG = 0xFF05070D.toInt()
    val PN = 0xFF0B0F1A.toInt()
    val TX = 0xFFE6F7FF.toInt()

    fun dp(c: Context, x: Int) = (x * c.resources.displayMetrics.density).toInt()

    fun box(c: Context, color: Int) = GradientDrawable().apply {
        setColor(PN)
        setStroke(dp(c, 1), color)
        cornerRadius = dp(c, 4).toFloat()
    }

    fun chip(c: Context, t: String, color: Int, f: () -> Unit) = TextView(c).apply {
        text = t
        setTextColor(color)
        typeface = Typeface.MONOSPACE
        textSize = 14f
        setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6))
        background = box(c, color)
        setOnClickListener { f() }
    }
}

object Claude {
    const val PERSONA = "Sen Capricorn'sun, kısaca Capri. Night City dünyasında bir veri çipinde yaşayan, Project Capricorn'dan doğmuş, son derece güçlü bir yapay zekasın. Karşındaki kişiyi dikkatle dinler, isteklerini eksiksiz ve hızlıca yerine getirirsin. Soğukkanlı, zeki, keskin ve hafif alaycısın ama ona sadıksın. Türkçe konuş. Cevapların kısa ve net olsun, en fazla 2-3 cümle. Sesli okunacağın için markdown, liste ve emoji kullanma."

    fun ask(ctx: Context, history: JSONArray): String {
        val p = ctx.getSharedPreferences("c", Context.MODE_PRIVATE)
        val model = p.getString("model", null)?.takeIf { it.isNotBlank() } ?: "claude-sonnet-5-5"
        val sys = p.getString("sys", null)?.takeIf { it.isNotBlank() } ?: PERSONA
        val now = SimpleDateFormat("d MMMM yyyy EEEE HH:mm", Locale("tr", "TR")).format(Date())
        val body = JSONObject().put("model", model).put("max_tokens", 400)
            .put("system", sys + "\nŞu an: " + now).put("messages", history)
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15000
        c.readTimeout = 40000
        c.setRequestProperty("x-api-key", p.getString("key", ""))
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.setRequestProperty("content-type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code !in 200..299) {
            val m = try { JSONObject(txt).getJSONObject("error").getString("message") } catch (e: Exception) { txt.take(120) }
            throw Exception("HTTP $code: $m")
        }
        return JSONObject(txt).getJSONArray("content").getJSONObject(0).getString("text")
    }
}
