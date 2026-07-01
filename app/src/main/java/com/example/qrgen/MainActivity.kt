package com.example.qrgen

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.qrgen.databinding.ActivityMainBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBitmap: Bitmap? = null
    private var lastData: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGenerate.setOnClickListener { generate() }
        binding.btnSave.setOnClickListener { saveToGallery() }
        binding.btnShare.setOnClickListener { share() }

        setActionsEnabled(false)
    }

    private fun setActionsEnabled(enabled: Boolean) {
        binding.btnSave.isEnabled = enabled
        binding.btnShare.isEnabled = enabled
    }

    private fun generate() {
        val material = binding.etMaterial.text.toString().trim()
        val gas = binding.etGas.text.toString().trim()
        val thickness = binding.etThickness.text.toString().trim()
        val numTest = binding.etNumTest.text.toString().trim()
        val machineId = binding.etMachineId.text.toString().trim()

        if (material.isEmpty() || gas.isEmpty() || thickness.isEmpty() ||
            numTest.isEmpty() || machineId.isEmpty()
        ) {
            toast("Please fill in all fields.")
            return
        }

        // Same schema as the original Python (';' delimited), whitespace cleaned up.
        val data = "$material;$gas;$thickness;$numTest;$machineId"
        lastData = data

        try {
            val bmp = encodeAsQr(data, 800)
            currentBitmap = bmp
            binding.ivQr.setImageBitmap(bmp)
            binding.tvData.text = data
            setActionsEnabled(true)
        } catch (e: Exception) {
            toast("Failed to generate QR: ${e.message}")
        }
    }

    private fun encodeAsQr(text: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2
        )
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private fun fileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "QR_$ts.png"
    }

    private fun saveToGallery() {
        val bmp = currentBitmap ?: return
        val name = fileName()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/QRCodes"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = contentResolver
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: throw Exception("MediaStore insert returned null")

                resolver.openOutputStream(uri).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out!!)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                toast("Saved to Pictures/QRCodes")
            } else {
                // API 26-28: write to app-specific external dir (no permission needed).
                val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(dir, name)
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                toast("Saved: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            toast("Save failed: ${e.message}")
        }
    }

    private fun share() {
        val bmp = currentBitmap ?: return
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName())
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, lastData)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share QR code"))
        } catch (e: Exception) {
            toast("Share failed: ${e.message}")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
