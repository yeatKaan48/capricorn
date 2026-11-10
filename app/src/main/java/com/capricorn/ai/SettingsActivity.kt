package com.capricorn.ai

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var apiKey: EditText
    private lateinit var fishApiKey: EditText
    private lateinit var model: EditText
    private lateinit var persona: EditText

    private fun dp(v: Int) = Theme.dp(this, v)

    private fun prefs() =
        getSharedPreferences("capricorn", MODE_PRIVATE)

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
            text = "◈  CAPRI // CONTROL CENTER"
            textSize = 19f
            setTextColor(Theme.CYAN)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.07f
            setPadding(0, dp(4), 0, dp(5))
        })

        content.addView(TextView(this).apply {
            text = "SYSTEM PREFERENCES  //  CONFIGURATION"
            textSize = 9f
            setTextColor(Theme.MUTED)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, 0, 0, dp(22))
        })

        content.addView(section("01  //  AI CONNECTION", Theme.CYAN))
        content.addView(label("GOOGLE GEMINI API KEY"))

        apiKey = field(
            SecureStore.getApiKey(this),
            "Google AI Studio API key",
            1
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        content.addView(apiKey)

        content.addView(section("02  //  VOICE SYSTEM", Theme.PINK))
        content.addView(label("FISH AUDIO API KEY"))

        fishApiKey = field(
            SecureStore.getFishApiKey(this),
            "Fish Audio API key",
            1
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        content.addView(fishApiKey)

        content.addView(label("MODEL ID"))
        model = field(
            p.getString("model", ClaudeApi.DEFAULT_MODEL)
                ?: ClaudeApi.DEFAULT_MODEL,
            "Gemini model ID",
            1
        )
        content.addView(model)

        content.addView(section("03  //  PERSONALITY CORE", Theme.YELLOW))
        content.addView(label("CAPRI PERSONALITY / SYSTEM PROMPT"))

        persona = field(
            p.getString("persona", CapriCore.DEFAULT_PERSONA)
                ?: CapriCore.DEFAULT_PERSONA,
            "Capri kişiliği",
            9
        )
        content.addView(persona)

        content.addView(
            Theme.chip(this, "RESTORE DEFAULT PERSONA", Theme.YELLOW).apply {
                setOnClickListener {
                    persona.setText(CapriCore.DEFAULT_PERSONA)
                }
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
            }
        )

        content.addView(
            Theme.chip(this, "SAVE CONFIGURATION", Theme.CYAN).apply {
                setOnClickListener {
                    if (save()) finish()
                }
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
            }
        )

        content.addView(
            Theme.chip(this, "BACK", Theme.PINK).apply {
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            }
        )

        content.addView(TextView(this).apply {
            text = "API anahtarları Android Keystore ile cihaz üzerinde şifrelenerek saklanır. Gemini ve Fish Audio kullanımı ilgili hesaplarının kullanım koşullarına ve limitlerine bağlıdır."
            textSize = 11f
            setTextColor(Theme.MUTED)
            setPadding(0, dp(16), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Theme.BG)
            addView(content)
        })
    }

    private fun section(value: String, accent: Int) =
        TextView(this).apply {
            text = value
            textSize = 10f
            setTextColor(accent)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, dp(22), 0, dp(9))
        }

    private fun label(value: String) =
        TextView(this).apply {
            text = "// $value"
            textSize = 11f
            setTextColor(Theme.YELLOW)
            typeface = android.graphics.Typeface.MONOSPACE
        }

    private fun field(
        value: String,
        hintText: String,
        lines: Int
    ) = EditText(this).apply {
        setText(value)
        hint = hintText
        setHintTextColor(Theme.MUTED)
        setTextColor(Theme.TEXT)
        textSize = 13f
        background = Theme.panel(
            this@SettingsActivity,
            0xFF245B6A.toInt(),
            0xFF0B1420.toInt()
        )
        setPadding(dp(14), dp(12), dp(14), dp(12))
        letterSpacing = 0.02f
        minLines = lines
        maxLines = if (lines > 1) 16 else 1
        gravity = if (lines > 1) Gravity.TOP else Gravity.CENTER_VERTICAL
    }

    private fun save(): Boolean {
        return try {
            SecureStore.saveApiKey(
                this,
                apiKey.text.toString().trim()
            )

            val fishKey = fishApiKey.text.toString().trim()
            if (fishKey.isNotBlank()) {
                SecureStore.saveFishApiKey(this, fishKey)
            }

            prefs().edit()
                .putString(
                    "model",
                    model.text.toString().trim()
                        .ifBlank { ClaudeApi.DEFAULT_MODEL }
                )
                .putString(
                    "persona",
                    persona.text.toString().trim()
                        .ifBlank { CapriCore.DEFAULT_PERSONA }
                )
                .apply()

            Toast.makeText(
                this,
                "Ayarlar kaydedildi.",
                Toast.LENGTH_SHORT
            ).show()
            true
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Kaydetme hatası: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
