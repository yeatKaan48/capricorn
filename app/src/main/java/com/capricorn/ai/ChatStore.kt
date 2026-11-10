package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int
)

object ChatStore {
    private const val PREFS = "capricorn"
    private const val CHATS_KEY = "chats_v1"
    private const val ACTIVE_KEY = "active_chat_id"
    private const val LEGACY_KEY = "history"
    private val lock = Any()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readChats(context: Context): JSONArray {
        val raw = prefs(context).getString(CHATS_KEY, null)
            ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveChats(context: Context, chats: JSONArray) {
        prefs(context).edit().putString(CHATS_KEY, chats.toString()).apply()
    }

    private fun findChat(chats: JSONArray, id: String): JSONObject? {
        for (i in 0 until chats.length()) {
            val chat = chats.optJSONObject(i) ?: continue
            if (chat.optString("id") == id) return chat
        }
        return null
    }

    private fun newChat(title: String = "Yeni sohbet"): JSONObject {
        val now = System.currentTimeMillis()
        return JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("title", title)
            put("createdAt", now)
            put("updatedAt", now)
            put("messages", JSONArray())
        }
    }

    fun initialize(context: Context): String = synchronized(lock) {
        val p = prefs(context)
        val raw = p.getString(CHATS_KEY, null)

        val chats = if (raw == null) {
            val legacy = try {
                JSONArray(p.getString(LEGACY_KEY, "[]"))
            } catch (_: Exception) {
                JSONArray()
            }
            JSONArray().put(
                newChat(if (legacy.length() > 0) "Eski sohbet" else "Yeni sohbet")
                    .apply { put("messages", legacy) }
            )
        } else {
            readChats(context)
        }

        if (chats.length() == 0) chats.put(newChat())

        val activeId = p.getString(ACTIVE_KEY, null)
        val active = activeId?.let { findChat(chats, it) }
        val selected = active ?: chats.optJSONObject(chats.length() - 1) ?: newChat()

        if (findChat(chats, selected.optString("id")) == null) {
            chats.put(selected)
        }

        saveChats(context, chats)
        val id = selected.getString("id")
        p.edit().putString(ACTIVE_KEY, id).apply()
        id
    }

    fun activeChatId(context: Context): String = initialize(context)

    fun setActiveChat(context: Context, id: String): Boolean =
        synchronized(lock) {
            val chats = readChats(context)
            if (findChat(chats, id) == null) return@synchronized false
            prefs(context).edit().putString(ACTIVE_KEY, id).apply()
            true
        }

    fun createChat(context: Context): String = synchronized(lock) {
        val chats = readChats(context)
        val chat = newChat()
        chats.put(chat)
        saveChats(context, chats)
        val id = chat.getString("id")
        prefs(context).edit().putString(ACTIVE_KEY, id).apply()
        id
    }

    fun listChats(context: Context): List<ChatSummary> =
        synchronized(lock) {
            val chats = readChats(context)
            val result = ArrayList<ChatSummary>()

            for (i in 0 until chats.length()) {
                val chat = chats.optJSONObject(i) ?: continue
                val messages = chat.optJSONArray("messages") ?: JSONArray()

                result.add(
                    ChatSummary(
                        id = chat.optString("id"),
                        title = chat.optString("title", "Yeni sohbet"),
                        updatedAt = chat.optLong("updatedAt"),
                        messageCount = messages.length()
                    )
                )
            }

            result.sortedByDescending { it.updatedAt }
        }

    fun getMessages(context: Context, id: String): JSONArray =
        synchronized(lock) {
            val chat = findChat(readChats(context), id)
                ?: return@synchronized JSONArray()
            JSONArray(
                (chat.optJSONArray("messages") ?: JSONArray()).toString()
            )
        }

    fun addMessage(
        context: Context,
        id: String,
        role: String,
        content: String
    ): Boolean = synchronized(lock) {
        if ((role != "user" && role != "assistant") || content.isBlank()) {
            return@synchronized false
        }

        val chats = readChats(context)
        val chat = findChat(chats, id) ?: return@synchronized false
        val messages = chat.optJSONArray("messages") ?: JSONArray()

        messages.put(
            JSONObject()
                .put("role", role)
                .put("content", content)
                .put("timestamp", System.currentTimeMillis())
        )

        while (messages.length() > 80) {
            messages.remove(0)
        }

        chat.put("messages", messages)
        chat.put("updatedAt", System.currentTimeMillis())

        if (role == "user" && chat.optString("title") == "Yeni sohbet") {
            chat.put("title", content.replace("\n", " ").take(36))
        }

        saveChats(context, chats)
        true
    }

    fun renameChat(
        context: Context,
        id: String,
        title: String
    ): Boolean = synchronized(lock) {
        val clean = title.trim().take(60)
        if (clean.isBlank()) return@synchronized false

        val chats = readChats(context)
        val chat = findChat(chats, id) ?: return@synchronized false

        chat.put("title", clean)
        chat.put("updatedAt", System.currentTimeMillis())
        saveChats(context, chats)
        true
    }

    fun clearChat(context: Context, id: String): Boolean =
        synchronized(lock) {
            val chats = readChats(context)
            val chat = findChat(chats, id)
                ?: return@synchronized false

            chat.put("messages", JSONArray())
            chat.put("title", "Yeni sohbet")
            chat.put("updatedAt", System.currentTimeMillis())

            saveChats(context, chats)
            true
        }

    fun deleteChat(context: Context, id: String): Boolean =
        synchronized(lock) {
            val chats = readChats(context)
            val updated = JSONArray()
            var found = false

            for (i in 0 until chats.length()) {
                val chat = chats.optJSONObject(i) ?: continue
                if (chat.optString("id") == id) {
                    found = true
                } else {
                    updated.put(chat)
                }
            }

            if (!found) return@synchronized false
            if (updated.length() == 0) updated.put(newChat())

            saveChats(context, updated)

            val p = prefs(context)
            if (p.getString(ACTIVE_KEY, null) == id) {
                val nextId = updated
                    .optJSONObject(updated.length() - 1)
                    ?.optString("id")
                p.edit().putString(ACTIVE_KEY, nextId).apply()
            }

            true
        }
}
