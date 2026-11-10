package com.capricorn.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object MemoryStore {
    private const val PREFS_NAME = "capri_memory"
    private const val KEY_MEMORIES = "memories"
    private val lock = Any()

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun read(context: Context): JSONArray {
        val raw = preferences(context).getString(KEY_MEMORIES, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun save(context: Context, memories: JSONArray): Boolean =
        preferences(context).edit()
            .putString(KEY_MEMORIES, memories.toString())
            .commit()

    fun getAll(context: Context): JSONArray = synchronized(lock) {
        JSONArray(read(context).toString())
    }

    fun add(context: Context, content: String): String? = synchronized(lock) {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return@synchronized null

        val memories = read(context)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val memory = JSONObject().apply {
            put("id", id)
            put("content", cleanContent)
            put("createdAt", now)
            put("updatedAt", now)
        }
        memories.put(memory)
        if (save(context, memories)) id else null
    }

    fun update(context: Context, id: String, content: String): Boolean =
        synchronized(lock) {
            val cleanContent = content.trim()
            if (cleanContent.isEmpty()) return@synchronized false
            val memories = read(context)
            for (i in 0 until memories.length()) {
                val memory = memories.optJSONObject(i) ?: continue
                if (memory.optString("id") == id) {
                    memory.put("content", cleanContent)
                    memory.put("updatedAt", System.currentTimeMillis())
                    return@synchronized save(context, memories)
                }
            }
            false
        }

    fun delete(context: Context, id: String): Boolean = synchronized(lock) {
        val memories = read(context)
        val updated = JSONArray()
        var found = false
        for (i in 0 until memories.length()) {
            val memory = memories.optJSONObject(i) ?: continue
            if (memory.optString("id") == id) {
                found = true
            } else {
                updated.put(memory)
            }
        }
        found && save(context, updated)
    }

    fun clear(context: Context): Boolean = synchronized(lock) {
        save(context, JSONArray())
    }

    fun count(context: Context): Int = synchronized(lock) {
        read(context).length()
    }
}\n