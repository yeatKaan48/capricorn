package com.capricorn.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import java.io.File

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
        val names = listOf("Bekleme", "Dinleme", "Düşünme", "Konuşma")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(key)
            addView(sys)
            addView(Button(context).apply { text = "Başlat"; setOnClickListener { begin() } })
            addView(TextView(context).apply { text = "\nSprite kareleri (sırayla seç, tek GIF de olur):" })
            for (i in 0..3) addView(Button(context).apply {
                text = names[i] + " karelerini seç"
                setOnClickListener { pick(i) }
            })
            addView(Button(context).apply {
                text = "Spriteları sıfırla"
                setOnClickListener {
                    File(filesDir, "sprites").deleteRecursively()
                    Toast.makeText(context, "Sıfırlandı", Toast.LENGTH_SHORT).show()
                }
            })
        }
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun pick(s: Int) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(i, 10 + s)
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        if (rc < 10 || rc > 13 || res != RESULT_OK || d == null) return
        val dir = File(filesDir, "sprites/${rc - 10}")
        dir.deleteRecursively()
        dir.mkdirs()
        val uris = ArrayList<Uri>()
        val cd = d.clipData
        if (cd != null) { for (i in 0 until cd.itemCount) uris.add(cd.getItemAt(i).uri) }
        else d.data?.let { uris.add(it) }
        uris.forEachIndexed { i, u ->
            val ext = if (contentResolver.getType(u) == "image/gif") "gif" else "png"
            contentResolver.openInputStream(u)?.use { inp ->
                File(dir, "%03d.%s".format(i, ext)).outputStream().use { out -> inp.copyTo(out) }
            }
        }
        Toast.makeText(this, "${uris.size} kare kaydedildi", Toast.LENGTH_SHORT).show()
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
