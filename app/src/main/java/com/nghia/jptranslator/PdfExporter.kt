package com.nghia.jptranslator

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a multi-page PDF where each book page produces a small spread:
 *  - Page A: scanned image (kept original)
 *  - Page B: Vietnamese translation (plus collapsed Japanese OCR at bottom)
 *
 * Page size: A4 at 72dpi -> 595 x 842 px (PdfDocument uses points = 1/72 inch).
 */
object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36

    suspend fun exportPagesToPdf(context: Context, pages: List<Page>, outFile: File) =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 18f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 13f
                isAntiAlias = true
            }
            val smallPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(120, 120, 120)
                textSize = 10f
            }

            // Cover page
            run {
                val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
                val page = doc.startPage(info)
                val canvas = page.canvas
                val title = "Bản dịch tiếng Việt"
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi")).format(Date())
                canvas.drawText(title, MARGIN.toFloat(), 120f, TextPaint(titlePaint).apply { textSize = 28f })
                canvas.drawText("Tạo bởi Japanese Book Translator", MARGIN.toFloat(), 150f, bodyPaint)
                canvas.drawText("Ngày: $date", MARGIN.toFloat(), 170f, bodyPaint)
                canvas.drawText("Số trang: ${pages.size}", MARGIN.toFloat(), 190f, bodyPaint)
                doc.finishPage(page)
            }

            var pageNumber = 2
            pages.forEachIndexed { idx, p ->
                // --- Page A: scanned image ---
                val imageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber++).create()
                val imgPage = doc.startPage(imageInfo)
                val imgCanvas = imgPage.canvas
                val bmp = runCatching {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(p.imagePath, opts)
                }.getOrNull()
                if (bmp != null) {
                    val availW = PAGE_W - 2 * MARGIN
                    val availH = PAGE_H - 2 * MARGIN - 30
                    val scale = minOf(availW.toFloat() / bmp.width, availH.toFloat() / bmp.height)
                    val drawW = (bmp.width * scale).toInt()
                    val drawH = (bmp.height * scale).toInt()
                    val x = (PAGE_W - drawW) / 2
                    val y = MARGIN
                    imgCanvas.drawBitmap(bmp, null, Rect(x, y, x + drawW, y + drawH), null)
                    bmp.recycle()
                }
                imgCanvas.drawText("Trang gốc ${idx + 1}", MARGIN.toFloat(), (PAGE_H - 20).toFloat(), labelPaint)
                doc.finishPage(imgPage)

                // --- Page B: Vietnamese translation ---
                drawTextPagesForPage(doc, p, idx + 1, titlePaint, bodyPaint, smallPaint, labelPaint, pageNumberRef = { pageNumber }, advance = { pageNumber++ })
            }

            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
        }

    /**
     * Render the translated text. If too long, spills to additional pages.
     */
    private fun drawTextPagesForPage(
        doc: PdfDocument,
        page: Page,
        humanIdx: Int,
        titlePaint: TextPaint,
        bodyPaint: TextPaint,
        smallPaint: TextPaint,
        labelPaint: Paint,
        pageNumberRef: () -> Int,
        advance: () -> Unit
    ) {
        val contentW = PAGE_W - 2 * MARGIN
        val vnText = page.vietnameseText.ifBlank { "(không có nội dung dịch)" }
        val jpText = page.japaneseText

        val vnLayout = makeLayout(vnText, bodyPaint, contentW)
        val jpHeaderH = 18
        val jpLayout = if (jpText.isNotBlank()) makeLayout(jpText, smallPaint, contentW) else null

        // We split vnLayout across pages if needed.
        val maxBodyH = PAGE_H - 2 * MARGIN - 40 // 40 reserved for title + page label
        var lineStart = 0
        while (lineStart < vnLayout.lineCount) {
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumberRef()).create()
            val pdfPage = doc.startPage(info)
            val canvas = pdfPage.canvas
            canvas.drawText("Bản dịch tiếng Việt - trang $humanIdx",
                MARGIN.toFloat(), (MARGIN + 4).toFloat(), titlePaint)

            // Determine how many lines fit
            var used = 0
            var lineEnd = lineStart
            while (lineEnd < vnLayout.lineCount) {
                val lh = vnLayout.getLineBottom(lineEnd) - (if (lineEnd == 0) 0 else vnLayout.getLineTop(lineStart))
                if (lh > maxBodyH) break
                lineEnd++
                used = vnLayout.getLineBottom(lineEnd - 1) - vnLayout.getLineTop(lineStart)
                if (used > maxBodyH - 20) break
            }
            if (lineEnd == lineStart) lineEnd = lineStart + 1 // ensure progress

            canvas.save()
            canvas.translate(MARGIN.toFloat(), (MARGIN + 30).toFloat())
            // Clip and shift to render only the selected lines
            val top = vnLayout.getLineTop(lineStart)
            canvas.translate(0f, -top.toFloat())
            canvas.clipRect(0, top, contentW, vnLayout.getLineBottom(lineEnd - 1))
            vnLayout.draw(canvas)
            canvas.restore()

            // Footer: original Japanese (only on last spillover page, small font)
            if (lineEnd >= vnLayout.lineCount && jpLayout != null) {
                val footerTop = PAGE_H - MARGIN - minOf(120, jpLayout.height + jpHeaderH)
                canvas.drawText("Tiếng Nhật gốc (OCR):", MARGIN.toFloat(), footerTop.toFloat(), labelPaint)
                canvas.save()
                canvas.translate(MARGIN.toFloat(), (footerTop + 6).toFloat())
                canvas.clipRect(0, 0, contentW, minOf(100, jpLayout.height))
                jpLayout.draw(canvas)
                canvas.restore()
            }

            canvas.drawText("— $humanIdx —", (PAGE_W / 2 - 12).toFloat(), (PAGE_H - 18).toFloat(), labelPaint)
            doc.finishPage(pdfPage)
            advance()
            lineStart = lineEnd
        }
    }

    private fun makeLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.1f)
            .setIncludePad(true)
            .build()
    }
}
