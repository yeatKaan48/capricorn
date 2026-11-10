package com.capricorn.ai

import android.app.Activity
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
    private lateinit var orb: OrbView

    private val executor = Executors.newSingleThreadExecutor()
    private var history = JSONArray()
    private var activeChatId = ""
    private var mediaPlayer: MediaPlayer? = null
    private var speakReplies = false
    private var waiting = false

    private fun prefs() =
        getSharedPreferences("capricorn", MODE_PRIVATE)

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
        activeChatId = ChatStore.initialize(this)
        history = ChatStore.getMessages(this, activeChatId)

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
            setPadding(dp(16), dp(24), dp(16), dp(12))
            setBackgroundColor(Theme.BG)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brandBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val brand = TextView(this).apply {
            text = "CAPRI"
            setTextColor(Theme.CYAN)
            textSize = 19f
            typeface = Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD
            )
            letterSpacing = 0.12f
        }

        val subtitle = TextView(this).apply {
            text = "NEURAL INTERFACE  //  PERSONAL AI"
            setTextColor(Theme.MUTED)
            textSize = 8f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
        }

        brandBlock.addView(brand)
        brandBlock.addView(subtitle)
        header.addView(
            brandBlock,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        state = TextView(this).apply {
            text = "● SYSTEM READY"
            setTextColor(Theme.YELLOW)
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = Theme.panel(
                this@MainActivity,
                Theme.YELLOW
            )
        }

        header.addView(
            state,
            LinearLayout.LayoutParams(-2, dp(38)).apply {
                marginEnd = dp(6)
            }
        )

        val settings = Theme.chip(this, "⚙", Theme.CYAN).apply {
            setOnClickListener {
                tapFeedback(it)
                startActivity(
                    Intent(
                        this@MainActivity,
                        SettingsActivity::class.java
                    )
                )
            }
        }

        header.addView(
            settings,
            LinearLayout.LayoutParams(dp(42), dp(38))
        )
        root.addView(header)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }

        nav.addView(
            Theme.chip(this, "CHATS", Theme.YELLOW).apply {
                setOnClickListener {
                    tapFeedback(it)
                    showChatManager()
                }
            },
            LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                marginEnd = dp(5)
            }
        )

        nav.addView(
            Theme.chip(this, "MEMORY", Theme.CYAN).apply {
                setOnClickListener {
                    tapFeedback(it)
                    startActivity(
                        Intent(
                            this@MainActivity,
                            MemoryActivity::class.java
                        )
                    )
                }
            },
            LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                marginEnd = dp(5)
            }
        )

        nav.addView(
            Theme.chip(
                this,
                if (speakReplies) "VOICE: ON" else "VOICE: OFF",
                Theme.PINK
            ).apply {
                setOnClickListener {
                    speakReplies = !speakReplies
                    text = if (speakReplies) {
                        "VOICE: ON"
                    } else {
                        "VOICE: OFF"
                    }

                    val accent = if (speakReplies) {
                        Theme.CYAN
                    } else {
                        Theme.PINK
                    }

                    setTextColor(accent)
                    background = Theme.panel(
                        this@MainActivity,
                        accent
                    )

                    tapFeedback(this)
                    if (!speakReplies) stopVoice()
                }
            },
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        val navLabel = TextView(this).apply {
            text = "⌘  SYSTEM MODULES"
            setTextColor(Theme.MUTED)
            textSize = 9f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.14f
            setPadding(dp(3), dp(2), 0, dp(2))
        }

        root.addView(navLabel)
        root.addView(nav)

        val orbPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Theme.panel(
                this@MainActivity,
                0xFF17404B.toInt(),
                0xFF080E17.toInt()
            )
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        val orbTitle = TextView(this).apply {
            text = "◈  NEURAL CORE     //     LIVE SYSTEM"
            setTextColor(Theme.CYAN)
            textSize = 9f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.13f
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(2))
        }

        orbPanel.addView(
            orbTitle,
            LinearLayout.LayoutParams(-1, dp(22))
        )

        orb = OrbView(this).apply {
            state = 0
            contentDescription = "Capri durumu"
        }

        orbPanel.addView(
            orb,
            LinearLayout.LayoutParams(-1, dp(154))
        )

        root.addView(
            orbPanel,
            LinearLayout.LayoutParams(-1, dp(188)).apply {
                bottomMargin = dp(10)
            }
        )

        ObjectAnimator.ofFloat(
            orbPanel,
            "alpha",
            0.88f,
            1f
        ).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        messagesBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            background = Theme.panel(
                this@MainActivity,
                0xFF18313D.toInt(),
                0xFF070B12.toInt()
            )
            addView(messagesBox)
        }

        val chatHeader = TextView(this).apply {
            text = "◈  COMMUNICATION FEED     //     SECURE LINK"
            setTextColor(Theme.CYAN)
            textSize = 9f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            setPadding(dp(5), dp(7), dp(5), dp(7))
        }

        root.addView(chatHeader)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val inputLabel = TextView(this).apply {
            text = "⌘  INPUT TERMINAL     //     AWAITING TRANSMISSION"
            setTextColor(Theme.MUTED)
            textSize = 9f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.1f
            setPadding(dp(4), dp(5), 0, dp(3))
        }

        root.addView(inputLabel)

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(6))
        }

        input = EditText(this).apply {
            hint = "Transmit a message to Capri..."
            setTextColor(Theme.TEXT)
            setHintTextColor(Theme.MUTED)
            textSize = 14f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = Theme.panel(
                this@MainActivity,
                0xFF245B6A.toInt(),
                0xFF0B1420.toInt()
            )
            setSingleLine(false)
            minLines = 1
            maxLines = 4
            imeOptions = EditorInfo.IME_ACTION_SEND

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCurrent()
                    true
                } else {
                    false
                }
            }
        }

        composer.background = Theme.panel(
            this,
            0xFF17313D.toInt(),
            0xFF080E17.toInt()
        )
        composer.setPadding(
            dp(7),
            dp(8),
            dp(7),
            dp(8)
        )

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, dp(54), 1f)
        )

        composer.addView(
            Theme.chip(this, "MIC", Theme.YELLOW).apply {
                setOnClickListener {
                    tapFeedback(it)
                    startVoiceInput()
                }
            },
            LinearLayout.LayoutParams(dp(58), dp(46)).apply {
                marginStart = dp(6)
            }
        )

        sendButton = Theme.chip(
            this,
            "SEND",
            Theme.CYAN
        ).apply {
            setOnClickListener {
                tapFeedback(it)
                sendCurrent()
            }
        }

        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(dp(66), dp(46)).apply {
                marginStart = dp(6)
            }
        )

        root.addView(composer)

        val footer = TextView(this).apply {
            text = "⌁  SESSION CONTROL     //     NEURAL INTERFACE"
            setTextColor(Theme.PINK)
            textSize = 10f
            letterSpacing = 0.08f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(2))
            setOnClickListener {
                showChatManager()
            }
        }

        root.addView(footer)

        root.alpha = 0f
        root.translationY = dp(10).toFloat()
        setContentView(root)

        root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .start()

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

    private fun tapFeedback(view: android.view.View) {
        view.animate().cancel()
        view.scaleX = 1f
        view.scaleY = 1f

        view.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(110)
                    .start()
            }
            .start()
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
        val accent = if (isUser) Theme.PINK else Theme.CYAN

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(dp(5), dp(11), dp(5), dp(11))
        }

        val identity = TextView(this).apply {
            this.text = if (isUser) {
                "◆  YOU  //  OPERATOR"
            } else {
                "◈  CAPRI  //  NEURAL CORE"
            }
            setTextColor(accent)
            textSize = 9f
            letterSpacing = 0.12f
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
            )
            setPadding(dp(5), dp(2), dp(5), dp(5))
        }

        wrapper.addView(identity)

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            background = Theme.panel(
                this@MainActivity,
                if (isUser) {
                    0xFF71304B.toInt()
                } else {
                    0xFF176071.toInt()
                },
                if (isUser) {
                    0xFF170D18.toInt()
                } else {
                    0xFF091720.toInt()
                }
            )
            setPadding(dp(3), dp(3), dp(12), dp(3))
        }

        val marker = android.view.View(this).apply {
            setBackgroundColor(accent)
        }

        bubble.addView(
            marker,
            LinearLayout.LayoutParams(dp(3), -1).apply {
                marginEnd = dp(9)
            }
        )

        val body = TextView(this).apply {
            this.text = text
            setTextColor(Theme.TEXT)
            textSize = 14f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
            setLineSpacing(dp(3).toFloat(), 1.04f)
            setPadding(dp(2), dp(9), dp(2), dp(9))
            setTextIsSelectable(true)
        }

        val maxBubbleWidth =
            (resources.displayMetrics.widthPixels * 0.88f).toInt()

        bubble.addView(
            body,
            LinearLayout.LayoutParams(
                maxBubbleWidth - dp(32),
                -2
            )
        )

        wrapper.addView(
            bubble,
            LinearLayout.LayoutParams(-2, -2).apply {
                if (isUser) {
                    marginStart = dp(20)
                } else {
                    marginEnd = dp(20)
                }
            }
        )

        messagesBox.addView(
            wrapper,
            LinearLayout.LayoutParams(-1, -2)
        )

        if (scrollDown) {
            wrapper.alpha = 0f
            wrapper.translationX = if (isUser) {
                dp(18).toFloat()
            } else {
                -dp(18).toFloat()
            }

            wrapper.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(220)
                .start()

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

        ChatStore.addMessage(
            this,
            activeChatId,
            "user",
            text
        )

        addMessage("user", text)

        val placeholder = addMessage(
            "assistant",
            "İşleniyor…"
        )

        waiting = true
        sendButton.isEnabled = false
        state.text = "◉ PROCESSING"
        orb.state = 1

        executor.execute {
            val answer = try {
                ClaudeApi.ask(this, history)
            } catch (e: Exception) {
                "ERROR: ${e.message ?: "Bilinmeyen bağlantı hatası"}"
            }

            runOnUiThread {
                waiting = false
                sendButton.isEnabled = true
                state.text = "● SYSTEM READY"
                orb.state = 0
                placeholder.text = answer

                if (!answer.startsWith("ERROR:")) {
                    history.put(
                        JSONObject()
                            .put("role", "assistant")
                            .put("content", answer)
                    )

                    ChatStore.addMessage(
                        this,
                        activeChatId,
                        "assistant",
                        answer
                    )

                    if (speakReplies) {
                        synthesizeVoice(answer)
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
                val audioFile = FishAudioApi.synthesize(
                    this,
                    text
                )

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
                    orb.state = 3
                    state.text = "◉ VOICE ACTIVE"
                } else {
                    it.release()
                    if (mediaPlayer === it) {
                        mediaPlayer = null
                    }
                }
            }

            player.setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                }

                if (!waiting) {
                    orb.state = 0
                    state.text = "● SYSTEM READY"
                }
            }

            player.setOnErrorListener { mp, _, _ ->
                mp.release()

                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }

                if (!waiting) {
                    orb.state = 0
                    state.text = "● SYSTEM READY"
                }

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

    private fun startVoiceInput() {
        orb.state = 2
        state.text = "◉ LISTENING"

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "tr-TR"
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Capri'ye söyle"
            )
        }

        try {
            startActivityForResult(intent, 41)
        } catch (_: Exception) {
            orb.state = 0
            state.text = "● SYSTEM READY"

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
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 41) {
            orb.state = 0
            state.text = "● SYSTEM READY"

            if (resultCode == RESULT_OK) {
                data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )?.firstOrNull()?.let {
                    sendMessage(it)
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        stopVoice()
        super.onDestroy()
    }

    private fun renderChat() {
        history = ChatStore.getMessages(
            this,
            activeChatId
        )

        messagesBox.removeAllViews()

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue

            addMessage(
                item.optString("role"),
                item.optString("content"),
                false
            )
        }

        if (history.length() == 0) {
            welcome()
        }

        scroll.post {
            scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun showChatManager() {
        if (waiting) {
            Toast.makeText(
                this,
                "Yanıt tamamlandıktan sonra sohbet değiştirebilirsin.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val chats = ChatStore.listChats(this)

        val labels = chats.map {
            "${if (it.id == activeChatId) "● " else ""}${it.title} (${it.messageCount})"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("SOHBETLER")
            .setItems(labels) { _, index ->
                showChatActions(chats[index])
            }
            .setPositiveButton("YENİ SOHBET") { _, _ ->
                activeChatId = ChatStore.createChat(this)
                renderChat()
            }
            .setNegativeButton("KAPAT", null)
            .show()
    }

    private fun showChatActions(chat: ChatSummary) {
        val actions = arrayOf(
            "Sohbeti aç",
            "Yeniden adlandır",
            "Sohbeti sil"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle(chat.title)
            .setItems(actions) { _, index ->
                when (index) {
                    0 -> {
                        ChatStore.setActiveChat(
                            this,
                            chat.id
                        )
                        activeChatId = chat.id
                        renderChat()
                    }

                    1 -> renameChatDialog(chat)
                    2 -> confirmDeleteChat(chat)
                }
            }
            .show()
    }

    private fun renameChatDialog(chat: ChatSummary) {
        val field = EditText(this).apply {
            setText(chat.title)
            setSingleLine(true)
            setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
            )
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("SOHBETİ YENİDEN ADLANDIR")
            .setView(field)
            .setPositiveButton("KAYDET") { _, _ ->
                if (
                    ChatStore.renameChat(
                        this,
                        chat.id,
                        field.text.toString()
                    )
                ) {
                    Toast.makeText(
                        this,
                        "Sohbet adı güncellendi.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Geçerli bir isim gir.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("İPTAL", null)
            .show()
    }

    private fun confirmDeleteChat(chat: ChatSummary) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SOHBET SİLİNSİN Mİ?")
            .setMessage(
                "'${chat.title}' kalıcı olarak silinecek."
            )
            .setPositiveButton("SİL") { _, _ ->
                ChatStore.deleteChat(
                    this,
                    chat.id
                )

                if (chat.id == activeChatId) {
                    activeChatId = ChatStore.initialize(this)
                    renderChat()
                }

                Toast.makeText(
                    this,
                    "Sohbet silindi.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("VAZGEÇ", null)
            .show()
    }
}
