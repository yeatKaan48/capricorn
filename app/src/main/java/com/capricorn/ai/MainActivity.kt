package com.capricorn.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var history = JSONArray()
    private val ui = Handler(Looper.getMainLooper())

    private fun prefs() = getSharedPreferences("c", MODE_PRIVATE)
    private fun on() = prefs().getBoolean("on", true)
    private fun dp(x: Int) = Theme.dp(this, x)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Theme.BG
        try { history = JSONArray(prefs().getString("h", "[]")) } catch (e: Exception) {}

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(12), dp(40), dp(12), dp(12))
        }
        val title = TextView(this).apply {
            text = "CAPRICORN //"
            setTextColor(Theme.CY)
            typeface = Typeface.MONOSPACE
            textSize = 18f
        }
        status = Theme.chip(this, "", Theme.CY) {
            prefs().edit().putBoolean("on", !on()).apply()
            refresh()
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(status)
        head.addView(Theme.chip(this, "⬡", Theme.YL) { startBubble() },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        head.addView(Theme.chip(this, "⚙", Theme.CY) { startActivity(Intent(this, SettingsActivity::class.java)) },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll = ScrollView(this).apply { addView(list) }

        input = EditText(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setTextColor(Theme.TX)
            setHintTextColor(0xFF4A6A7A.toInt())
            background = Theme.box(this@MainActivity, Theme.CY)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            maxLines = 4
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Theme.chip(this, "🎤", Theme.YL) { listen() },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        row.addView(Theme.chip(this, "▶", Theme.CY) {
            val t = input.text.toString().trim()
            input.setText("")
            send(t)
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })

        root.addView(head)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })
        root.addView(row)
        setContentView(root)

        for (i in 0 until history.length()) {
            val m = history.getJSONObject(i)
            addMsg(m.getString("role"), m.getString("content"))
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val o = on()
        val c = if (o) Theme.CY else Theme.MG
        status.text = if (o) "● ONLINE" else "○ KAPALI"
        status.setTextColor(c)
        status.background = Theme.box(this, c)
        input.isEnabled = o
        input.hint = if (o) "> mesajını yaz..." else "> izin kapalı"
    }

    private fun addMsg(role: String, msg: String): TextView {
        val me = role == "user"
        val tv = TextView(this).apply {
            text = (if (me) "SEN> " else "CAPRI> ") + msg
            setTextColor(Theme.TX)
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = Theme.box(this@MainActivity, if (me) Theme.MG else Theme.CY)
        }
        list.addView(tv, LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = dp(8)
            gravity = if (me) Gravity.END else Gravity.START
        })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return tv
    }

    private fun save() {
        while (history.length() > 40) { history.remove(0); history.remove(0) }
        prefs().edit().putString("h", history.toString()).apply()
    }

    private fun send(text: String) {
        if (!on() || text.isBlank()) return
        if (prefs().getString("key", "").isNullOrBlank()) {
            addMsg("assistant", "API anahtarı yok. Sağ üstteki ⚙ ayarlardan gir.")
            return
        }
        if (text.lowercase(Locale("tr", "TR")).contains("hafızayı sil")) {
            history = JSONArray(); save(); list.removeAllViews()
            addMsg("assistant", "Hafıza temizlendi.")
            return
        }
        addMsg("user", text)
        history.put(JSONObject().put("role", "user").put("content", text))
        val w = addMsg("assistant", "...")
        Thread {
            var err: String? = null
            val r = try { Claude.ask(this, history) } catch (e: Exception) { err = e.message; null }
            ui.post {
                if (r == null) {
                    history.remove(history.length() - 1)
                    w.text = "ERR> $err"
                } else {
                    history.put(JSONObject().put("role", "assistant").put("content", r))
                    save()
                    w.text = "CAPRI> $r"
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }.start()
    }

    private fun listen() {
        if (!on()) return
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"), 1)
        } catch (e: Exception) {
            Toast.makeText(this, "Ses tanıma bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        if (rc == 1 && res == RESULT_OK) {
            d?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { send(it) }
        }
    }

    private fun startBubble() {
        if (!on()) { Toast.makeText(this, "İzin kapalı", Toast.LENGTH_SHORT).show(); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 20)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "İzni aç, geri dönüp ⬡'a bas", Toast.LENGTH_LONG).show()
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        moveTaskToBack(true)
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        if (c == 20 && r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) startBubble()
    }
}
