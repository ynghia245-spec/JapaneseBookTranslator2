package com.nghia.jptranslator

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ML Kit translator: Japanese -> Vietnamese.
 * Models run fully on-device after a one-time download.
 */
object TranslationManager {

    @Volatile private var translator: Translator? = null
    @Volatile private var modelReady: Boolean = false

    private fun getTranslator(): Translator {
        return translator ?: synchronized(this) {
            translator ?: Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.JAPANESE)
                    .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                    .build()
            ).also { translator = it }
        }
    }

    suspend fun ensureModelDownloaded(context: Context): Boolean {
        if (modelReady) return true
        val client = getTranslator()
        val cond = DownloadConditions.Builder().build() // allow over cellular too
        return withContext(Dispatchers.IO) {
            try {
                client.downloadModelIfNeeded(cond).await()
                modelReady = true
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun translate(context: Context, japanese: String): String {
        if (japanese.isBlank()) return ""
        if (!modelReady) ensureModelDownloaded(context)
        val client = getTranslator()
        // Split by blank lines to preserve paragraph boundaries
        val paragraphs = japanese.split("\n\n")
        val out = StringBuilder()
        for (p in paragraphs) {
            if (p.isBlank()) { out.append('\n'); continue }
            try {
                val translated = client.translate(p).await()
                out.append(translated).append("\n\n")
            } catch (e: Exception) {
                out.append("[không dịch được đoạn này]\n\n")
            }
        }
        return out.toString().trim()
    }
}
