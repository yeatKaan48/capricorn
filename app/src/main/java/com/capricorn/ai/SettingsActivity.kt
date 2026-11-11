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
            textSize = 20f
            setTextColor(Theme.PINK)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.07f
            setPadding(0, dp(4), 0, dp(6))
        })

        content.addView(TextView(this).apply {
            text = "SYSTEM PREFERENCES  //  CONFIGURATION"
            textSize = 9f
            setTextColor(Theme.MUTED)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, 0, 0, dp(22))
        })

        content.addView(sectionPanel("01  //  AI CONNECTION", Theme.PINK) {
            addView(label("GOOGLE GEMINI API KEY"), fieldParams())

            apiKey = field(
                SecureStore.getApiKey(this@SettingsActivity),
                "Google AI Studio API key",
                1
            ).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            addView(apiKey, inputParams())

            addView(label("MODEL ID"), fieldParams())

            model = field(
                p.getString("model", ClaudeApi.DEFAULT_MODEL)
                    ?: ClaudeApi.DEFAULT_MODEL,
                "Gemini model ID",
                1
            )
            addView(model, inputParams())
        }, panelParams())

        content.addView(sectionPanel("02  //  VOICE SYSTEM", Theme.PINK) {
            addView(label("FISH AUDIO API KEY"), fieldParams())

            fishApiKey = field(
                SecureStore.getFishApiKey(this@SettingsActivity),
                "Fish Audio API key",
                1
            ).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            addView(fishApiKey, inputParams())
        }, panelParams())

        content.addView(sectionPanel("03  //  PERSONALITY CORE", Theme.YELLOW) {
            addView(label("CAPRI PERSONALITY / SYSTEM PROMPT"), fieldParams())

            persona = field(
                p.getString("persona", CapriCore.DEFAULT_PERSONA)
                    ?: CapriCore.DEFAULT_PERSONA,
                "Capri kişiliği",
                9
            )
            addView(persona, inputParams())

            addView(
                Theme.chip(
                    this@SettingsActivity,
                    "RESTORE DEFAULT PERSONA",
                    Theme.YELLOW
                ).apply {
                    setOnClickListener {
                        persona.setText(CapriCore.DEFAULT_PERSONA)
                    }
                },
                LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(12)
                }
            )
        }, panelParams())

        content.addView(
            Theme.chip(this, "SAVE CONFIGURATION", Theme.PINK).apply {
                setOnClickListener {
                    if (save()) finish()
                }
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(4)
            }
        )

        content.addView(
            Theme.chip(this, "BACK", Theme.YELLOW).apply {
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
            }
        )

        content.addView(TextView(this).apply {
            text = "API anahtarları Android Keystore ile cihaz üzerinde şifrelenerek saklanır. Gemini ve Fish Audio kullanımı ilgili hesaplarının kullanım koşullarına ve limitlerine bağlıdır."
            textSize = 11f
            setTextColor(Theme.MUTED)
            setPadding(0, dp(18), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Theme.BG)
            isFillViewport = true
            addView(content)
        })
    }

    private fun sectionPanel(
        title: String,
        accent: Int,
        contentBlock: LinearLayout.() -> Unit
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(16))
        background = Theme.gradientPanel(
            this@SettingsActivity,
            Theme.BORDER,
            Theme.PANEL,
            Theme.PANEL_DARK
        )

        addView(TextView(this@SettingsActivity).apply {
            text = title
            textSize = 11f
            setTextColor(accent)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
        })

        contentBlock()
    }

    private fun label(value: String) =
        TextView(this).apply {
            text = "// $value"
            textSize = 10f
            setTextColor(Theme.YELLOW)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.04f
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
            Theme.BORDER,
            Theme.BG
        )
        setPadding(dp(14), dp(12), dp(14), dp(12))
        letterSpacing = 0.02f
        minLines = lines
        maxLines = if (lines > 1) 16 else 1
        gravity = if (lines > 1) {
            Gravity.TOP
        } else {
            Gravity.CENTER_VERTICAL
        }
    }

    private fun fieldParams() =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(13)
        }

    private fun inputParams() =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(7)
        }

    private fun panelParams() =
        LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(14)
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
