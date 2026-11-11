package com.capricorn.ai

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MemoryActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var input: EditText

    private fun dp(v: Int) = Theme.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(26), dp(18), dp(20))
            setBackgroundColor(Theme.BG)
        }

        content.addView(TextView(this).apply {
            text = "◈  CAPRI // MEMORY CORE"
            textSize = 19f
            setTextColor(Theme.CYAN)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.06f
        })

        content.addView(TextView(this).apply {
            text = "LONG-TERM STORAGE  //  PERSONAL DATA"
            textSize = 9f
            setTextColor(Theme.MUTED)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.10f
            setPadding(0, dp(5), 0, dp(18))
        })

        content.addView(section("01  //  NEW MEMORY", Theme.CYAN))

        input = EditText(this).apply {
            hint = "Capri için kaydedilecek bilgiyi yaz..."
            setTextColor(Theme.TEXT)
            setHintTextColor(Theme.MUTED)
            textSize = 14f
            minLines = 3
            maxLines = 6
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = Theme.gradientPanel(
                this@MemoryActivity,
                Theme.BORDER,
                Theme.PANEL,
                Theme.PANEL_DARK
            )
        }

        content.addView(
            input,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
        )

        content.addView(
            Theme.chip(this, "＋  SAVE MEMORY", Theme.CYAN).apply {
                setOnClickListener { saveMemory() }
            }
        )

        content.addView(
            section("02  //  STORED MEMORIES", Theme.PINK).apply {
                setPadding(0, dp(24), 0, dp(10))
            }
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        actions.addView(
            Theme.chip(this, "↻  REFRESH", Theme.CYAN).apply {
                setOnClickListener { refresh() }
            }
        )

        actions.addView(
            Theme.chip(this, "✕  CLEAR ALL", Theme.PINK).apply {
                setOnClickListener { confirmClear() }
            },
            LinearLayout.LayoutParams(-2, -2).apply {
                marginStart = dp(10)
            }
        )

        content.addView(actions)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(list)
        }

        content.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(12)
            }
        )

        setContentView(content)
        refresh()
    }

    private fun section(value: String, accent: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = 10f
            setTextColor(accent)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.08f
        }

    private fun saveMemory() {
        val value = input.text.toString().trim()

        if (value.isEmpty()) {
            Toast.makeText(
                this,
                "Önce bir bilgi yaz.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (MemoryStore.add(this, value) != null) {
            input.text.clear()
            refresh()
            Toast.makeText(
                this,
                "Memory saved.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Kayıt başarısız.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refresh() {
        list.removeAllViews()

        val memories = MemoryStore.getAll(this)

        if (memories.length() == 0) {
            list.addView(TextView(this).apply {
                text = "NO STORED MEMORIES\nYour memory core is empty."
                textSize = 12f
                setTextColor(Theme.MUTED)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(32), dp(12), dp(32))
            })
            return
        }

        for (i in 0 until memories.length()) {
            val memory = memories.optJSONObject(i) ?: continue
            val id = memory.optString("id")
            val value = memory.optString("content")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = Theme.gradientPanel(
                    this@MemoryActivity,
                    Theme.BORDER,
                    Theme.PANEL,
                    Theme.PANEL_DARK
                )
                elevation = dp(2).toFloat()
            }

            card.addView(TextView(this).apply {
                text = "◈  MEMORY ${i + 1}"
                textSize = 10f
                setTextColor(Theme.CYAN)
                typeface = android.graphics.Typeface.MONOSPACE
            })

            card.addView(TextView(this).apply {
                text = value
                textSize = 14f
                setTextColor(Theme.TEXT)
                setLineSpacing(dp(3).toFloat(), 1f)
                setPadding(0, dp(9), 0, dp(14))
            })

            val buttons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            buttons.addView(
                Theme.chip(this, "EDIT", Theme.YELLOW).apply {
                    setOnClickListener {
                        editMemory(id, value)
                    }
                }
            )

            buttons.addView(
                Theme.chip(this, "DELETE", Theme.PINK).apply {
                    setOnClickListener {
                        confirmDelete(id)
                    }
                },
                LinearLayout.LayoutParams(-2, -2).apply {
                    marginStart = dp(8)
                }
            )

            card.addView(buttons)

            list.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )
        }
    }

    private fun confirmClear() {
        if (MemoryStore.count(this) == 0) {
            Toast.makeText(
                this,
                "Hafıza zaten boş.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear all memories?")
            .setMessage(
                "Kaydedilmiş tüm uzun süreli hafıza verileri silinecek."
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("CLEAR") { _, _ ->
                if (MemoryStore.clear(this)) {
                    refresh()
                    Toast.makeText(
                        this,
                        "All memories cleared.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Silme başarısız.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun confirmDelete(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete memory?")
            .setMessage("Bu kayıt kalıcı olarak silinecek.")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DELETE") { _, _ ->
                if (MemoryStore.delete(this, id)) {
                    refresh()
                    Toast.makeText(
                        this,
                        "Memory deleted.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Silme başarısız.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun editMemory(id: String, oldValue: String) {
        val editor = EditText(this).apply {
            setText(oldValue)
            setTextColor(Theme.TEXT)
            setHintTextColor(Theme.MUTED)
            minLines = 3
            maxLines = 7
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        AlertDialog.Builder(this)
            .setTitle("Edit memory")
            .setView(editor)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val updated = editor.text.toString().trim()

                if (updated.isNotBlank() &&
                    MemoryStore.update(this, id, updated)
                ) {
                    refresh()
                    Toast.makeText(
                        this,
                        "Memory updated.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Güncelleme başarısız.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}
