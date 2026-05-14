package com.nghia.jptranslator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nghia.jptranslator.databinding.ActivityCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnShutter.setOnClickListener { takePhoto() }
        binding.btnDone.setOnClickListener { finish() }

        startCameraPreview()
        updatePageCount()
    }

    private fun updatePageCount() {
        val n = PageStore.getAll(this).size
        binding.tvPageCount.text = getString(R.string.page_count_format, n)
    }

    private fun startCameraPreview() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val ic = imageCapture ?: return
        binding.btnShutter.isEnabled = false
        binding.tvStatus.setText(R.string.capturing)

        val name = "page_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        val dir = File(filesDir, "images").apply { mkdirs() }
        val file = File(dir, name)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        ic.takePicture(options, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                binding.btnShutter.isEnabled = true
                binding.tvStatus.text = getString(R.string.capture_failed)
                Toast.makeText(this@CameraActivity, exception.message ?: "?", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                processCapturedImage(file)
            }
        })
    }

    private fun processCapturedImage(file: File) {
        binding.tvStatus.setText(R.string.processing)
        lifecycleScope.launch {
            try {
                // Load bitmap (downscale for speed)
                val bitmap = withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } ?: throw IllegalStateException("Cannot decode image")

                // OCR Japanese
                val japaneseText = OcrEngine.recognizeJapanese(bitmap)

                // Translate to Vietnamese
                val vietnameseText = if (japaneseText.isBlank()) "" else TranslationManager.translate(this@CameraActivity, japaneseText)

                // Save record
                val page = Page(
                    imagePath = file.absolutePath,
                    japaneseText = japaneseText,
                    vietnameseText = vietnameseText,
                    timestamp = System.currentTimeMillis()
                )
                PageStore.add(this@CameraActivity, page)

                binding.tvStatus.text = getString(R.string.captured_ok)
                updatePageCount()

                // Show preview of result
                val intent = Intent(this@CameraActivity, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_PAGE_INDEX, PageStore.getAll(this@CameraActivity).size - 1)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Process failed", e)
                binding.tvStatus.text = getString(R.string.processing_failed, e.message ?: "?")
            } finally {
                binding.btnShutter.isEnabled = true
            }
        }
    }

    companion object { private const val TAG = "CameraActivity" }
}
