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

object SecureStore {
    private const val STORE = "capricorn_secure"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(alias: String): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun save(context: Context, pref: String, alias: String, value: String) {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        if (value.isBlank()) {
            prefs.edit().remove(pref).apply()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(alias))
        val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        prefs.edit().putString(pref, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun get(context: Context, pref: String, alias: String): String {
        val packed = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getString(pref, null) ?: return ""

        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            require(bytes.size > 12)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(alias), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun saveApiKey(context: Context, value: String) =
        save(context, "api_key_enc", "capricorn_api_key_v1", value)

    fun getApiKey(context: Context): String =
        get(context, "api_key_enc", "capricorn_api_key_v1")

    fun saveFishApiKey(context: Context, value: String) =
        save(context, "fish_api_key_enc", "capricorn_fish_key_v1", value)

    fun getFishApiKey(context: Context): String =
        get(context, "fish_api_key_enc", "capricorn_fish_key_v1")
}
