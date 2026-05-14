package com.nghia.jptranslator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Page(
    val imagePath: String,
    val japaneseText: String,
    val vietnameseText: String,
    val timestamp: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("imagePath", imagePath)
        put("japaneseText", japaneseText)
        put("vietnameseText", vietnameseText)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(o: JSONObject) = Page(
            imagePath = o.getString("imagePath"),
            japaneseText = o.getString("japaneseText"),
            vietnameseText = o.getString("vietnameseText"),
            timestamp = o.getLong("timestamp")
        )
    }
}

object PageStore {
    private const val FILE_NAME = "pages.json"

    fun getAll(context: Context): List<Page> {
        val f = File(context.filesDir, FILE_NAME)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { Page.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, page: Page) {
        val all = getAll(context).toMutableList()
        all.add(page)
        save(context, all)
    }

    fun replaceAt(context: Context, index: Int, page: Page) {
        val all = getAll(context).toMutableList()
        if (index in all.indices) {
            all[index] = page
            save(context, all)
        }
    }

    fun removeAt(context: Context, index: Int) {
        val all = getAll(context).toMutableList()
        if (index in all.indices) {
            val p = all.removeAt(index)
            // best-effort delete image
            runCatching { File(p.imagePath).delete() }
            save(context, all)
        }
    }

    fun clearAll(context: Context) {
        val all = getAll(context)
        for (p in all) runCatching { File(p.imagePath).delete() }
        save(context, emptyList())
    }

    private fun save(context: Context, pages: List<Page>) {
        val arr = JSONArray()
        for (p in pages) arr.put(p.toJson())
        File(context.filesDir, FILE_NAME).writeText(arr.toString())
    }
}
