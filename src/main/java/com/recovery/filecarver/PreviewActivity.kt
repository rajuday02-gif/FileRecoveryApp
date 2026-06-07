package com.recovery.filecarver

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val filePath = intent.getStringExtra("file_path") ?: return
        val fileName = intent.getStringExtra("file_name") ?: "Unknown"
        val fileSize = intent.getLongExtra("file_size", 0)

        supportActionBar?.title = fileName

        val file = File(filePath)
        val extension = file.extension.lowercase()

        val scrollView = findViewById<ScrollView>(R.id.preview_scroll)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // File info header
        val infoText = TextView(this).apply {
            text = """File: $fileName
Size: ${formatFileSize(fileSize)}
Type: ${extension.uppercase()}
Path: $filePath
Last Modified: ${file.lastModified()}"""
            textSize = 12f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }
        container.addView(infoText)

        // Preview based on file type
        when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> {
                try {
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    if (bitmap != null) {
                        val imageView = ImageView(this).apply {
                            setImageBitmap(bitmap)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                800
                            )
                        }
                        container.addView(imageView)
                    } else {
                        val errorText = TextView(this).apply {
                            text = "Failed to load image"
                            textSize = 14f
                            setPadding(16, 16, 16, 16)
                        }
                        container.addView(errorText)
                    }
                } catch (e: Exception) {
                    val errorText = TextView(this).apply {
                        text = "Error: ${e.message}"
                        textSize = 12f
                        setPadding(16, 16, 16, 16)
                    }
                    container.addView(errorText)
                }
            }
            "txt", "log", "csv" -> {
                val textView = TextView(this).apply {
                    text = file.readText()
                    textSize = 12f
                    setPadding(16, 16, 16, 16)
                }
                container.addView(textView)
            }
            else -> {
                val hexPreview = TextView(this).apply {
                    text = "Hex Preview:\n" + generateHexPreview(file)
                    textSize = 10f
                    typeface = android.graphics.Typeface.MONOSPACE
                    setPadding(16, 16, 16, 16)
                }
                container.addView(hexPreview)
            }
        }

        scrollView.addView(container)
    }

    private fun generateHexPreview(file: File, lines: Int = 10): String {
        val bytes = file.readBytes().take(lines * 16).toByteArray()
        val sb = StringBuilder()

        for (i in bytes.indices step 16) {
            val hexPart = bytes.slice(i until minOf(i + 16, bytes.size))
                .joinToString(" ") { "%02X".format(it) }
            val asciiPart = bytes.slice(i until minOf(i + 16, bytes.size))
                .map { if (it in 32..126) it.toChar() else '.' }
                .joinToString("")
            sb.append("%08X  %-48s %s\n".format(i, hexPart, asciiPart))
        }

        return sb.toString()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
