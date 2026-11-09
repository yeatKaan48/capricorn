package com.capricorn.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private lateinit var key: EditText
    private lateinit var sys: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("c", MODE_PRIVATE)
        val pad = (24 * resources.displayMetrics.density).toInt()
        key = EditText(this).apply { hint = "Anthropic API anahtarı"; setText(p.getString("key", "")) }
        sys = EditText(this).apply {
            hint = "Kişilik"
            setText(p.getString("sys", "Sen Capricorn'sun: soğukkanlı, zeki, keskin espri anlayışlı bir yapay zekasın. Sesli konuşuyorsun: en fazla 2-3 kısa cümle, markdown ve emoji yok."))
        }
        val btn = Button(this).apply { text = "Başlat"; setOnClickListener { begin() } }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(key); addView(sys); addView(btn)
        })
    }

    private fun begin() {
        getSharedPreferences("c", MODE_PRIVATE).edit()
            .putString("key", key.text.toString().trim())
            .putString("sys", sys.text.toString()).apply()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 1)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "İzni aç, geri dönüp Başlat'a bas", Toast.LENGTH_LONG).show()
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) begin()
    }
}
