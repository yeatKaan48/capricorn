package com.capricorn.ai

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class SettingsActivity : Activity() {
    private lateinit var apiKey: EditText
    private lateinit var model: EditText
    private lateinit var persona: EditText
    private fun dp(v: Int) = Theme.dp(this, v)
    private fun prefs() = getSharedPreferences("capricorn", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG
        val p = prefs()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
            setBackgroundColor(Theme.BG)
        }
        content.addView(TextView(this).apply {
            text = "// SYSTEM CONFIGURATION"
            textSize = 19f
            setTextColor(Theme.CYAN)
            typeface = android.graphics.Typeface.MONOSPACE
        })
        content.addView(label("ANTHROPIC API KEY"))
        apiKey = field(SecureStore.getApiKey(this), "sk-ant-…", 1).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        content.addView(apiKey)
        content.addView(label("MODEL ID"))
        model = field(p.getString("model", ClaudeApi.DEFAULT_MODEL) ?: ClaudeApi.DEFAULT_MODEL, "claude-sonnet-5-5", 1)
        content.addView(model)
        content.addView(label("CAPRI PERSONALITY / SYSTEM PROMPT"))
        persona = field(p.getString("persona", ClaudeApi.DEFAULT_PERSONA) ?: ClaudeApi.DEFAULT_PERSONA, "Capri kişiliği", 9)
        content.addView(persona)
        content.addView(Theme.chip(this, "RESTORE DEFAULT PERSONA", Theme.YELLOW).apply {
            setOnClickListener { persona.setText(ClaudeApi.DEFAULT_PERSONA) }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        content.addView(Theme.chip(this, "SAVE CONFIGURATION", Theme.CYAN).apply {
            setOnClickListener { save(); finish() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        content.addView(Theme.chip(this, "BACK", Theme.PINK).apply { setOnClickListener { finish() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        content.addView(TextView(this).apply {
            text = "API key Android Keystore ile cihaz üzerinde şifrelenerek saklanır. API kullanımı Anthropic hesabında ücret oluşturabilir. Model erişimi hesabına bağlıdır."
            textSize = 11f
            setTextColor(Theme.MUTED)
            setPadding(0, dp(16), 0, 0)
        })
        setContentView(ScrollView(this).apply { setBackgroundColor(Theme.BG); addView(content) })
    }

    private fun label(value: String) = TextView(this).apply {
        text = "// $value"
        textSize = 11f
        setTextColor(Theme.YELLOW)
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private fun field(value: String, hintText: String, lines: Int) = EditText(this).apply {
        setText(value)
        hint = hintText
        setHintTextColor(Theme.MUTED)
        setTextColor(Theme.TEXT)
        textSize = 13f
        background = Theme.panel(this@SettingsActivity, Theme.CYAN)
        setPadding(dp(11), dp(10), dp(11), dp(10))
        minLines = lines
        maxLines = if (lines > 1) 16 else 1
        gravity = if (lines > 1) Gravity.TOP else Gravity.CENTER_VERTICAL
    }

    private fun save() {
        try {
            SecureStore.saveApiKey(this, apiKey.text.toString())
            prefs().edit()
                .putString("model", model.text.toString().trim().ifBlank { ClaudeApi.DEFAULT_MODEL })
                .putString("persona", persona.text.toString().trim().ifBlank { ClaudeApi.DEFAULT_PERSONA })
                .apply()
            Toast.makeText(this, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Ayarlar yalnızca SAVE düğmesine basıldığında kalıcı olarak yazılır.
    }
}
