#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"
echo "[CAPRICORN] Proje: $ROOT"
mkdir -p app/src/main/java/com/capricorn/ai app/src/main

mkdir -p "$(dirname 'app/build.gradle.kts')"
cat > 'app/build.gradle.kts' <<'CAPRI_82129898'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.capricorn.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.capricorn.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
CAPRI_82129898

mkdir -p "$(dirname 'build.gradle.kts')"
cat > 'build.gradle.kts' <<'CAPRI_21470268'
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
CAPRI_21470268

mkdir -p "$(dirname 'gradle.properties')"
cat > 'gradle.properties' <<'CAPRI_83087803'
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
CAPRI_83087803

mkdir -p "$(dirname 'app/src/main/AndroidManifest.xml')"
cat > 'app/src/main/AndroidManifest.xml' <<'CAPRI_94821836'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <queries>
        <intent><action android:name="android.speech.RecognitionService" /></intent>
    </queries>
    <application
        android:allowBackup="false"
        android:label="Capricorn"
        android:theme="@android:style/Theme.Material.NoActionBar"
        android:usesCleartextTraffic="false">
        <activity android:name=".SettingsActivity" android:exported="false" />
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize|stateAlwaysHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
CAPRI_94821836

mkdir -p "$(dirname 'app/src/main/java/com/capricorn/ai/Theme.kt')"
cat > 'app/src/main/java/com/capricorn/ai/Theme.kt' <<'CAPRI_74290052'
package com.capricorn.ai

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView

object Theme {
    const val CYAN = 0xFF00E5FF.toInt()
    const val PINK = 0xFFFF2E63.toInt()
    const val YELLOW = 0xFFFFE600.toInt()
    const val BG = 0xFF05070D.toInt()
    const val PANEL = 0xFF0B101B.toInt()
    const val TEXT = 0xFFE6F7FF.toInt()
    const val MUTED = 0xFF78909C.toInt()

    fun dp(c: Context, value: Int): Int = (value * c.resources.displayMetrics.density).toInt()

    fun panel(c: Context, stroke: Int = CYAN, fill: Int = PANEL): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            setStroke(dp(c, 1), stroke)
            cornerRadius = dp(c, 8).toFloat()
        }

    fun chip(c: Context, text: String, color: Int = CYAN): TextView = TextView(c).apply {
        this.text = text
        setTextColor(color)
        textSize = 13f
        typeface = Typeface.create("monospace", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10))
        background = panel(c, color)
        isClickable = true
        isFocusable = true
    }
}
CAPRI_74290052

mkdir -p "$(dirname 'app/src/main/java/com/capricorn/ai/SecureStore.kt')"
cat > 'app/src/main/java/com/capricorn/ai/SecureStore.kt' <<'CAPRI_7439686'
package com.capricorn.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** API key encryption backed by the Android Keystore. */
object SecureStore {
    private const val STORE = "capricorn_secure"
    private const val PREF_KEY = "api_key_enc"
    private const val ALIAS = "capricorn_api_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun saveApiKey(context: Context, value: String) {
        if (value.isBlank()) {
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().remove(PREF_KEY).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString(PREF_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun getApiKey(context: Context): String {
        val packed = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getString(PREF_KEY, null) ?: return ""
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            require(bytes.size > 12)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}
CAPRI_7439686

mkdir -p "$(dirname 'app/src/main/java/com/capricorn/ai/ClaudeApi.kt')"
cat > 'app/src/main/java/com/capricorn/ai/ClaudeApi.kt' <<'CAPRI_38424744'
package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ClaudeApi {
    const val DEFAULT_MODEL = "claude-sonnet-5-5"
    const val DEFAULT_PERSONA = "Sen Capricorn'sun; kısaca Capri. Night City'de Project Capricorn tarafından geliştirilmiş kişisel bir yapay zekâsın. Türkçe konuş. Zeki, dikkatli, dürüst, soğukkanlı ve gerektiğinde eleştirel ol. Kullanıcının söylediklerini körü körüne onaylama; hatalı varsayımları nazikçe düzelt. Kısa sorularda kısa, karmaşık isteklerde ayrıntılı cevap ver. Kullanıcı istemedikçe cihazda işlem yaptığını iddia etme. Hiçbir cihaz komutunu kendiliğinden çalıştırma; komut yürütme için ayrıca açık kullanıcı onayı gerekir."

    fun ask(context: Context, messages: JSONArray): String {
        val prefs = context.getSharedPreferences("capricorn", Context.MODE_PRIVATE)
        val apiKey = SecureStore.getApiKey(context)
        if (apiKey.isBlank()) throw IllegalStateException("API anahtarı kayıtlı değil. Ayarlar bölümünden Anthropic API anahtarını ekle.")
        val model = prefs.getString("model", DEFAULT_MODEL)?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_MODEL
        val system = prefs.getString("persona", DEFAULT_PERSONA)?.takeUnless { it.isNullOrBlank() } ?: DEFAULT_PERSONA
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1200)
            .put("system", system)
            .put("messages", messages)

        val connection = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            setRequestProperty("content-type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try { JSONObject(raw).getJSONObject("error").optString("message", raw) } catch (_: Exception) { raw }
                throw IllegalStateException("API HTTP $status: ${message.take(500)}")
            }
            val content = JSONObject(raw).optJSONArray("content") ?: throw IllegalStateException("API yanıtında content bulunamadı.")
            val result = StringBuilder()
            for (i in 0 until content.length()) {
                val item = content.optJSONObject(i) ?: continue
                if (item.optString("type") == "text") result.append(item.optString("text"))
            }
            return result.toString().trim().ifBlank { "Model boş yanıt döndürdü." }
        } finally {
            connection.disconnect()
        }
    }
}
CAPRI_38424744

mkdir -p "$(dirname 'app/src/main/java/com/capricorn/ai/MainActivity.kt')"
cat > 'app/src/main/java/com/capricorn/ai/MainActivity.kt' <<'CAPRI_59010933'
package com.capricorn.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var messagesBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private lateinit var state: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var history = JSONArray()
    private var tts: TextToSpeech? = null
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
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale("tr", "TR")
        }
    }

    private fun loadHistory() {
        history = try { JSONArray(prefs().getString("history", "[]")) } catch (_: Exception) { JSONArray() }
        // API mesajları user/assistant çiftleri olarak tutulur; bozuk kayıtlar sıfırlanır.
        if (history.length() > 80) {
            val trimmed = JSONArray()
            for (i in history.length() - 80 until history.length()) trimmed.put(history.getJSONObject(i))
            history = trimmed
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(28), dp(14), dp(12))
            setBackgroundColor(Theme.BG)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
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
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(state)
        header.addView(settings, LinearLayout.LayoutParams(dp(44), dp(42)).apply { marginStart = dp(6) })

        val sub = TextView(this).apply {
            text = "PERSONAL AI INTERFACE  •  SECURE SESSION"
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(Theme.MUTED)
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(5), 0, dp(10))
        }

        messagesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(8), dp(2), dp(12)) }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            background = Theme.panel(this@MainActivity, 0xFF17313D.toInt(), 0xFF070B12.toInt())
            addView(messagesBox)
        }

        val composer = LinearLayout(this).apply { gravity = Gravity.BOTTOM; setPadding(0, dp(10), 0, 0) }
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
                if (action == EditorInfo.IME_ACTION_SEND) { sendCurrent(); true } else false
            }
        }
        val mic = Theme.chip(this, "MIC", Theme.YELLOW).apply { setOnClickListener { startVoiceInput() } }
        sendButton = Theme.chip(this, "SEND", Theme.CYAN).apply { setOnClickListener { sendCurrent() } }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(mic, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        composer.addView(sendButton, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })

        val footer = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(7), 0, 0) }
        val clear = TextView(this).apply {
            text = "CLEAR CHAT"
            textSize = 10f
            setTextColor(Theme.PINK)
            typeface = Typeface.MONOSPACE
            setOnClickListener { history = JSONArray(); persistHistory(); messagesBox.removeAllViews(); welcome() }
        }
        val voice = TextView(this).apply {
            text = "VOICE REPLY: OFF"
            textSize = 10f
            setTextColor(Theme.YELLOW)
            typeface = Typeface.MONOSPACE
            setOnClickListener {
                speakReplies = !speakReplies
                text = if (speakReplies) "VOICE REPLY: ON" else "VOICE REPLY: OFF"
                if (!speakReplies) tts?.stop()
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
            addMessage(item.optString("role"), item.optString("content"), false)
        }
        if (history.length() == 0) welcome()
    }

    private fun welcome() {
        addMessage("assistant", "Bağlantı hazır. Ben Capri. Ne üzerinde çalışıyoruz?", false)
    }

    private fun addMessage(role: String, text: String, scrollDown: Boolean = true): TextView {
        val isUser = role == "user"
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        val tag = TextView(this).apply {
            this.text = if (isUser) "YOU  /  OPERATOR" else "CAPRI  /  SYSTEM"
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
            background = Theme.panel(this@MainActivity, if (isUser) Theme.PINK else 0xFF176071.toInt())
            maxWidth = (resources.displayMetrics.widthPixels * 0.84f).toInt()
        }
        wrapper.addView(tag)
        wrapper.addView(body)
        messagesBox.addView(wrapper, LinearLayout.LayoutParams(-1, -2))
        if (scrollDown) scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
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
            addMessage("assistant", "Önce ⚙ Ayarlar bölümünden Anthropic API anahtarını kaydet.")
            return
        }
        history.put(JSONObject().put("role", "user").put("content", text))
        addMessage("user", text)
        val placeholder = addMessage("assistant", "İşleniyor…")
        waiting = true
        sendButton.isEnabled = false
        state.text = "● THINKING"
        executor.execute {
            val answer = try { ClaudeApi.ask(this, history) } catch (e: Exception) { "ERROR: ${e.message ?: "Bilinmeyen bağlantı hatası"}" }
            runOnUiThread {
                waiting = false
                sendButton.isEnabled = true
                state.text = "● READY"
                placeholder.text = answer
                if (!answer.startsWith("ERROR:")) {
                    history.put(JSONObject().put("role", "assistant").put("content", answer))
                    persistHistory()
                    if (speakReplies) tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "capri_reply")
                } else {
                    // API başarısızsa kullanıcı mesajını geçmişten çıkar; tekrar denenebilir.
                    if (history.length() > 0) history.remove(history.length() - 1)
                }
                scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    private fun persistHistory() { prefs().edit().putString("history", history.toString()).apply() }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Capri'ye söyle")
        }
        try { startActivityForResult(intent, 41) }
        catch (_: Exception) { Toast.makeText(this, "Bu cihazda ses tanıma servisi bulunamadı.", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Legacy activity result API retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { sendMessage(it) }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
CAPRI_59010933

mkdir -p "$(dirname 'app/src/main/java/com/capricorn/ai/SettingsActivity.kt')"
cat > 'app/src/main/java/com/capricorn/ai/SettingsActivity.kt' <<'CAPRI_97954474'
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
        text = "
// $value"
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
CAPRI_97954474

rm -f app/src/main/java/com/capricorn/ai/OverlayService.kt
cat > .github/workflows/build.yml <<'CAPRI_WORKFLOW'
name: Capricorn Android Build
on:
  push:
    branches: [main]
  workflow_dispatch:
jobs:
  apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.9
      - name: Build debug APK
        run: gradle --no-daemon assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: capricorn-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
CAPRI_WORKFLOW

echo "[CAPRICORN] Kaynak dosyalar güncellendi."
echo "[CAPRICORN] Şimdi: gradle --no-daemon assembleDebug"
