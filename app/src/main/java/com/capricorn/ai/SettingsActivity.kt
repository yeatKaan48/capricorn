package com.capricorn.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import java.io.File

class SettingsActivity : Activity() {
    private lateinit var key: EditText
    private lateinit var model: EditText
    private lateinit var sys: EditText

    private fun prefs() = getSharedPreferences("c", MODE_PRIVATE)
    private fun dp(x: Int) = Theme.dp(this, x)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun label(t: String) = TextView(this).apply {
        text = "\n// $t"
        setTextColor(Theme.YL)
        typeface = Typeface.MONOSPACE
        textSize = 13f
    }

    private fun field(v: String, h: String, lines: Int = 1) = EditText(this).apply {
        setText(v)
        hint = h
        typeface = Typeface.MONOSPACE
        textSize = 13f
        setTextColor(Theme.TX)
        setHintTextColor(0xFF4A6A7A.toInt())
        background = Theme.box(this@SettingsActivity, Theme.CY)
        setPadding(dp(10), dp(10), dp(10), dp(10))
        if (lines > 1) { minLines = lines; gravity = Gravity.TOP }
    }

    private fun btn(t: String, c: Int, f: () -> Unit) = Theme.chip(this, t, c, f).apply {
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Theme.BG
        val p = prefs()
        key = field(p.getString("key", "") ?: "", "sk-ant-...").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            typeface = Typeface.MONOSPACE
        }
        model = field(p.getString("model", "claude-sonnet-5-5") ?: "claude-sonnet-5-5", "model")
        sys = field(p.getString("sys", Claude.PERSONA) ?: Claude.PERSONA, "kişilik", 8)
        val names = listOf("Bekleme", "Dinleme", "Düşünme", "Konuşma")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(14), dp(40), dp(14), dp(24))
            addView(TextView(context).apply {
                text = "// AYARLAR"
                setTextColor(Theme.CY)
                typeface = Typeface.MONOSPACE
                textSize = 20f
            })
            addView(label("API ANAHTARI")); addView(key)
            addView(label("MODEL")); addView(model)
            addView(label("KİŞİLİK")); addView(sys)
            addView(btn("Varsayılan Capricorn kişiliği", Theme.CY) { sys.setText(Claude.PERSONA) })
            addView(label("SPRITE KARELERİ (sırayla seç, tek GIF de olur)"))
            for (i in 0..3) addView(btn(names[i] + " karelerini seç", Theme.YL) { pick(i) })
            addView(btn("Spriteları sıfırla", Theme.MG) {
                File(filesDir, "sprites").deleteRecursively()
                toast("Sıfırlandı")
            })
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(Theme.BG); addView(col) })
    }

    override fun onPause() {
        super.onPause()
        prefs().edit()
            .putString("key", key.text.toString().trim())
            .putString("model", model.text.toString().trim().ifBlank { "claude-sonnet-5-5" })
            .putString("sys", sys.text.toString().ifBlank { Claude.PERSONA })
            .apply()
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
        toast("${uris.size} kare kaydedildi")
    }
}
