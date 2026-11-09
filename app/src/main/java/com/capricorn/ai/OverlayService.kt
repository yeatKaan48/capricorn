package com.capricorn.ai

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var box: LinearLayout
    private lateinit var bubble: TextView
    private lateinit var label: TextView
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val ui = Handler(Looper.getMainLooper())
    private val history = JSONArray()
    private var active = false

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int { if (::orb.isInitialized) orb.reload(); return START_NOT_STICKY }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("c", "Capricorn", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "c").setContentTitle("Capricorn aktif")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)

        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale("tr", "TR") }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(u: String?) {}
            override fun onDone(u: String?) { ui.post { listen() } }
            override fun onError(u: String?) { ui.post { listen() } }
        })

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        bubble = TextView(this).apply {
            text = "C"; textSize = 26f; gravity = Gravity.CENTER
            setTextColor(0xFF00E5FF.toInt())
        }
        paint(false)
        label = TextView(this).apply {
            textSize = 14f; maxLines = 8; visibility = View.GONE
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            maxWidth = dp(260)
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(0xE60B0F1A.toInt()) }
            setOnClickListener { visibility = View.GONE }
        }
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        box.addView(bubble, LinearLayout.LayoutParams(dp(64), dp(64)))
        box.addView(label, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })

        val lp = WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = dp(16); lp.y = dp(200)

        bubble.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false; var t0 = 0L
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false; t0 = System.currentTimeMillis() }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                        if (moved) { lp.x = sx + dx; lp.y = sy + dy; wm.updateViewLayout(box, lp) }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) {
                        if (System.currentTimeMillis() - t0 > 800) stopSelf() else toggle()
                    }
                }
                return true
            }
        })
        wm.addView(box, lp)
    }

    private fun paint(on: Boolean) {
        bubble.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCC0B0F1A.toInt())
            setStroke(dp(3), if (on) 0xFFFF2E63.toInt() else 0xFF00E5FF.toInt())
        }
    }

    private fun setLabel(s: String) { label.text = s; label.visibility = View.VISIBLE }

    private fun toggle() { if (active) stopAll() else { active = true; listen() } }

    private fun stopAll(msg: String? = null) {
        active = false; sr?.cancel(); tts?.stop(); paint(false)
        if (msg != null) setLabel(msg)
    }

    private fun listen() {
        if (!active) return
        paint(true)
        sr?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        sr = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { setLabel("Dinliyorum…") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
            override fun onError(e: Int) {
                if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { stopAll("Mikrofon izni yok"); return }
                if (active) ui.postDelayed({ listen() }, 700)
            }
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t.isNullOrBlank()) ui.post { listen() } else ask(t)
            }
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"))
    }

    private fun ask(text: String) {
        setLabel("…")
        history.put(JSONObject().put("role", "user").put("content", text))
        Thread {
            var err: String? = null
            val reply = try { callClaude() } catch (e: Exception) { err = e.message; null }
            ui.post {
                if (reply == null) {
                    history.remove(history.length() - 1)
                    stopAll("Hata: $err")
                } else {
                    history.put(JSONObject().put("role", "assistant").put("content", reply))
                    while (history.length() > 20) { history.remove(0); history.remove(0) }
                    setLabel(reply)
                    if (active) tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "u")
                }
            }
        }.start()
    }

    private fun callClaude(): String {
        val p = getSharedPreferences("c", MODE_PRIVATE)
        val body = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 300)
            .put("system", p.getString("sys", "")).put("messages", history)
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("x-api-key", p.getString("key", ""))
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.setRequestProperty("content-type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val txt = c.inputStream.bufferedReader().readText()
        return JSONObject(txt).getJSONArray("content").getJSONObject(0).getString("text")
    }

    override fun onDestroy() {
        sr?.destroy(); tts?.shutdown()
        try { wm.removeView(box) } catch (e: Exception) {}
        super.onDestroy()
    }
}
