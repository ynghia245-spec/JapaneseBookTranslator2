package com.nghia.jptranslator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.nghia.jptranslator.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener { ensureCameraThenStart() }
        binding.btnExportPdf.setOnClickListener { exportPdfFromCapturedPages() }
        binding.btnClear.setOnClickListener { clearPages() }

        refreshPageCount()
        // Pre-warm translation model in background
        lifecycleScope.launch { TranslationManager.ensureModelDownloaded(this@MainActivity) }
    }

    override fun onResume() {
        super.onResume()
        refreshPageCount()
    }

    private fun refreshPageCount() {
        val count = PageStore.getAll(this).size
        binding.tvPageCount.text = getString(R.string.page_count_format, count)
        binding.btnExportPdf.isEnabled = count > 0
        binding.btnClear.isEnabled = count > 0
    }

    private fun ensureCameraThenStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        startActivity(Intent(this, CameraActivity::class.java))
    }

    private fun clearPages() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_confirm_title)
            .setMessage(R.string.clear_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                PageStore.clearAll(this)
                refreshPageCount()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun exportPdfFromCapturedPages() {
        val pages = PageStore.getAll(this)
        if (pages.isEmpty()) {
            Toast.makeText(this, R.string.no_pages, Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.isIndeterminate = true
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvStatus.text = getString(R.string.exporting_pdf)

        lifecycleScope.launch {
            try {
                val outFile = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "TranslatedBook_${System.currentTimeMillis()}.pdf"
                )
                PdfExporter.exportPagesToPdf(this@MainActivity, pages, outFile)
                binding.tvStatus.text = getString(R.string.export_done)
                sharePdf(outFile)
            } catch (e: Exception) {
                binding.tvStatus.text = getString(R.string.export_failed, e.message ?: "?")
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun sharePdf(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(openIntent, getString(R.string.open_pdf_with)))
        } catch (_: Exception) {
            Toast.makeText(this, file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }
}
