package com.nghia.jptranslator

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.nghia.jptranslator.databinding.ActivityResultBinding

/**
 * Shows the captured image, recognized Japanese, and Vietnamese translation
 * for a single page. User can re-run OCR/translate or delete.
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var pageIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pageIndex = intent.getIntExtra(EXTRA_PAGE_INDEX, -1)
        val pages = PageStore.getAll(this)
        if (pageIndex !in pages.indices) {
            finish(); return
        }
        val page = pages[pageIndex]
        Glide.with(this).load(page.imagePath).into(binding.imgPreview)
        binding.tvJapanese.text = page.japaneseText.ifBlank { "(không nhận diện được chữ Nhật)" }
        binding.tvVietnamese.text = page.vietnameseText.ifBlank { "(chưa có bản dịch)" }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_page_title)
                .setMessage(R.string.delete_page_msg)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    PageStore.removeAt(this, pageIndex); finish()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    companion object { const val EXTRA_PAGE_INDEX = "page_index" }
}
