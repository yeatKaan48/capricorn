package com.capricorn.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var messagesBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var state: TextView

    private val executor = Executors.newSingleThreadExecutor()
    private var history = JSONArray()
    private var mediaPlayer: MediaPlayer? = null
    private var speakReplies = false
    private var waiting = false

    private fun prefs() = getSharedPreferences("capricorn", MODE_PRIVATE)
    private fun dp(v: Int) = Theme.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG
        window.decorView.setBackgroundColor(Theme.BG)
        loadHistory()
        buildUi()
    }

    private fun loadHistory() {
        history = try {
            JSONArray(prefs().getString("history", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        if (history.length() > 80) {
            val trimmed = JSONArray()
            for (i in history.length() - 80 until history.length()) {
                trimmed.put(history.getJSONObject(i))
            }
            history = trimmed
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(28), dp(14), dp(12))
            setBackgroundColor(Theme.BG)
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "CAPRICORN  //  NIGHT CITY"
            textSize = 17f
            setTextColor(Theme.CYAN)
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }

        state = TextView(this).apply {
            text = "● READY"
            textSize = 10f
            setTextColor(Theme.YELLOW)
            typeface = Typeface.MONOSPACE
            setPadding(dp(6), 0, dp(6), 0)
        }

        val settings = Theme.chip(this, "⚙", Theme.CYAN).apply {
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(state)
        header.addView(
            settings,
            LinearLayout.LayoutParams(dp(44), dp(42)).apply {
                marginStart = dp(6)
            }
        )

        val sub = TextView(this).apply {
            text = "PERSONAL AI INTERFACE  •  SECURE SESSION"
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(Theme.MUTED)
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(5), 0, dp(10))
        }

        messagesBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(12))
        }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            background = Theme.panel(
                this@MainActivity,
                0xFF17313D.toInt(),
                0xFF070B12.toInt()
            )
            addView(messagesBox)
        }

        val composer = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(10), 0, 0)
        }

        input = EditText(this).apply {
            hint = "> Capri'ye mesaj yaz..."
            setHintTextColor(Theme.MUTED)
            setTextColor(Theme.TEXT)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            background = Theme.panel(this@MainActivity, Theme.CYAN)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minLines = 1
            maxLines = 4
            imeOptions = EditorInfo.IME_ACTION_SEND

            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) {
                    sendCurrent()
                    true
                } else {
                    false
                }
            }
        }

        val mic = Theme.chip(this, "MIC", Theme.YELLOW).apply {
            setOnClickListener { startVoiceInput() }
        }

        sendButton = Theme.chip(this, "SEND", Theme.CYAN).apply {
            setOnClickListener { sendCurrent() }
        }

        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(
            mic,
            LinearLayout.LayoutParams(-2, -2).apply {
                marginStart = dp(6)
            }
        )
        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(-2, -2).apply {
                marginStart = dp(6)
            }
        )

        val footer = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, 0)
        }

        val clear = TextView(this).apply {
            text = "CLEAR CHAT"
            textSize = 10f
            setTextColor(Theme.PINK)
            typeface = Typeface.MONOSPACE

            setOnClickListener {
                history = JSONArray()
                persistHistory()
                messagesBox.removeAllViews()
                welcome()
            }
        }

        val voice = TextView(this).apply {
            text = "VOICE REPLY: OFF"
            textSize = 10f
            setTextColor(Theme.YELLOW)
            typeface = Typeface.MONOSPACE

            setOnClickListener {
                speakReplies = !speakReplies
                text = if (speakReplies) {
                    "VOICE REPLY: ON"
                } else {
                    "VOICE REPLY: OFF"
                }

                if (!speakReplies) stopVoice()
            }
        }

        footer.addView(clear, LinearLayout.LayoutParams(0, -2, 1f))
        footer.addView(voice)

        root.addView(header)
        root.addView(sub)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(composer)
        root.addView(footer)

        setContentView(root)

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue
            addMessage(
                item.optString("role"),
                item.optString("content"),
                false
            )
        }

        if (history.length() == 0) welcome()
    }

    private fun welcome() {
        addMessage(
            "assistant",
            "Bağlantı hazır. Ben Capri. Ne üzerinde çalışıyoruz?",
            false
        )
    }

    private fun addMessage(
        role: String,
        text: String,
        scrollDown: Boolean = true
    ): TextView {
        val isUser = role == "user"

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }

        val tag = TextView(this).apply {
            this.text = if (isUser) {
                "YOU  /  OPERATOR"
            } else {
                "CAPRI  /  SYSTEM"
            }

            textSize = 9f
            typeface = Typeface.MONOSPACE
            setTextColor(if (isUser) Theme.PINK else Theme.CYAN)
        }

        val body = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Theme.TEXT)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(dp(11), dp(9), dp(11), dp(9))

            background = Theme.panel(
                this@MainActivity,
                if (isUser) Theme.PINK else 0xFF176071.toInt()
            )

            maxWidth = (resources.displayMetrics.widthPixels * 0.84f).toInt()
        }

        wrapper.addView(tag)
        wrapper.addView(body)

        messagesBox.addView(
            wrapper,
            LinearLayout.LayoutParams(-1, -2)
        )

        if (scrollDown) {
            scroll.post {
                scroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        return body
    }

    private fun sendCurrent() {
        val text = input.text.toString().trim()

        if (text.isEmpty() || waiting) return

        input.setText("")
        sendMessage(text)
    }

    private fun sendMessage(text: String) {
        if (SecureStore.getApiKey(this).isBlank()) {
            addMessage(
                "assistant",
                "Önce ⚙ Ayarlar bölümünden Gemini API anahtarını kaydet."
            )
            return
        }

        history.put(
            JSONObject()
                .put("role", "user")
                .put("content", text)
        )

        addMessage("user", text)

        val placeholder = addMessage("assistant", "İşleniyor…")

        waiting = true
        sendButton.isEnabled = false
        state.text = "● THINKING"

        executor.execute {
            val answer = try {
                ClaudeApi.ask(this, history)
            } catch (e: Exception) {
                "ERROR: ${e.message ?: "Bilinmeyen bağlantı hatası"}"
            }

            runOnUiThread {
                waiting = false
                sendButton.isEnabled = true
                state.text = "● READY"
                placeholder.text = answer

                if (!answer.startsWith("ERROR:")) {
                    history.put(
                        JSONObject()
                            .put("role", "assistant")
                            .put("content", answer)
                    )

                    persistHistory()

                    if (speakReplies) synthesizeVoice(answer)
                } else {
                    if (history.length() > 0) {
                        history.remove(history.length() - 1)
                    }
                }

                scroll.post {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun synthesizeVoice(text: String) {
        executor.execute {
            try {
                val audioFile = FishAudioApi.synthesize(this, text)

                runOnUiThread {
                    if (!isFinishing && speakReplies) {
                        playVoice(audioFile)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (speakReplies && !isFinishing) {
                        Toast.makeText(
                            this,
                            "Fish Audio hatası: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun playVoice(file: File) {
        if (!speakReplies || isFinishing) return

        stopVoice()

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setDataSource(file.absolutePath)

            player.setOnPreparedListener {
                if (speakReplies && mediaPlayer === it) {
                    it.start()
                } else {
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                }
            }

            player.setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }

            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null

                Toast.makeText(
                    this,
                    "Ses oynatılamadı.",
                    Toast.LENGTH_SHORT
                ).show()

                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            stopVoice()

            Toast.makeText(
                this,
                "Ses oynatma hatası: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopVoice() {
        val player = mediaPlayer
        mediaPlayer = null

        try {
            player?.stop()
        } catch (_: Exception) {
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }
    }

    private fun persistHistory() {
        prefs().edit()
            .putString("history", history.toString())
            .apply()
    }

    private fun startVoiceInput() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Capri'ye söyle")
        }

        try {
            startActivityForResult(intent, 41)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Bu cihazda ses tanıma servisi bulunamadı.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Legacy activity result API retained for broad Android compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 41 && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()?.let {
                sendMessage(it)
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        stopVoice()
        super.onDestroy()
    }
}
