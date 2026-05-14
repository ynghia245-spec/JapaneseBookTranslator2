package com.nghia.jptranslator

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper around ML Kit's on-device Japanese text recognizer.
 * Returns the full recognized text (lines joined by newline).
 */
object OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeJapanese(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val sb = StringBuilder()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            sb.append(line.text).append('\n')
                        }
                        sb.append('\n')
                    }
                    cont.resume(sb.toString().trim())
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
