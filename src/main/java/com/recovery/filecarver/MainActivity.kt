package com.recovery.filecarver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var carver: CarverEngine
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fileListView: ListView
    private lateinit var selectFileBtn: Button
    private lateinit var startCarvingBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var tierInfo: TextView
    private lateinit var recoveryScope: RadioGroup

    private var selectedFilePath: String? = null
    private var currentTier = "FREE"
    private var carvingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        carver = CarverEngine()
        carver.initCarver()

        // Check Developer Mode
        if (isDeveloperMode()) {
            statusText.text = "✅ Developer Mode Enabled"
            showAdvancedOptions()
        } else {
            statusText.text = "❌ Developer Mode OFF - Enable it first!"
            selectFileBtn.isEnabled = false
            startCarvingBtn.isEnabled = false
        }

        requestPermissions()
        setupListeners()
        checkSubscription()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)
        fileListView = findViewById(R.id.file_list)
        selectFileBtn = findViewById(R.id.select_file_btn)
        startCarvingBtn = findViewById(R.id.start_carving_btn)
        stopBtn = findViewById(R.id.stop_btn)
        tierInfo = findViewById(R.id.tier_info)
        recoveryScope = findViewById(R.id.recovery_scope)
    }

    private fun setupListeners() {
        selectFileBtn.setOnClickListener { openFilePicker() }
        startCarvingBtn.setOnClickListener { startRecovery() }
        stopBtn.setOnClickListener { stopRecovery() }
    }

    private fun isDeveloperMode(): Boolean {
        return Settings.Global.getInt(
            contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) == 1
    }

    private fun showAdvancedOptions() {
        val advancedPanel = findViewById<LinearLayout>(R.id.advanced_panel)
        advancedPanel.visibility = View.VISIBLE
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
            }
        }
    }

    private fun checkSubscription() {
        val prefs = getSharedPreferences("FileRecovery", MODE_PRIVATE)
        currentTier = prefs.getString("tier", "FREE") ?: "FREE"
        updateTierUI()
    }

    private fun updateTierUI() {
        when (currentTier) {
            "FREE" -> {
                tierInfo.text = "🆓 Free Tier (500MB limit, 5/day)"
                tierInfo.setTextColor(android.graphics.Color.GRAY)
            }
            "PRO" -> {
                tierInfo.text = "💎 Pro Tier (10GB limit, Unlimited)"
                tierInfo.setTextColor(android.graphics.Color.BLUE)
            }
            "PREMIUM" -> {
                tierInfo.text = "👑 Premium Tier (Unlimited, All Features)"
                tierInfo.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            }
        }
    }

    private fun openFilePicker() {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "storage_dumps"
        )

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val files = downloadsDir.listFiles() ?: arrayOf()

        if (files.isEmpty()) {
            Toast.makeText(this, "No dump files found in Downloads/storage_dumps", Toast.LENGTH_SHORT).show()
            return
        }

        val fileNames = files.map { "${it.name} (${it.length() / 1024 / 1024}MB)" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Storage Dump")
            .setItems(fileNames) { _, which ->
                selectedFilePath = files[which].absolutePath
                statusText.text = "📁 Selected: ${files[which].name}"
            }
            .show()
    }

    private fun startRecovery() {
        if (selectedFilePath == null) {
            Toast.makeText(this, "Please select a storage dump file first", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        startCarvingBtn.isEnabled = false
        stopBtn.isEnabled = true
        statusText.text = "🔄 Recovery in progress..."

        carvingJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val outputDir = File(
                    getExternalFilesDir(null),
                    "recovered_files"
                ).apply { mkdirs() }

                val maxSize = when (currentTier) {
                    "FREE" -> 500 // MB
                    "PRO" -> 10 * 1024 // 10GB
                    else -> 100 * 1024 // 100GB for premium
                }

                val fileTypes = getSelectedFileTypes()

                carver.startCarving(selectedFilePath!!, outputDir.absolutePath, maxSize, fileTypes)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    startCarvingBtn.isEnabled = true
                    stopBtn.isEnabled = false
                    displayResults(outputDir)
                }
            } catch (e: Exception) {
                Log.e("FileCarver", "Error during recovery", e)
                runOnUiThread {
                    statusText.text = "❌ Error: ${e.message}"
                    progressBar.visibility = View.GONE
                    startCarvingBtn.isEnabled = true
                    stopBtn.isEnabled = false
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopRecovery() {
        carver.stopCarving()
        carvingJob?.cancel()
        statusText.text = "⏹️ Recovery stopped"
        progressBar.visibility = View.GONE
        startCarvingBtn.isEnabled = true
        stopBtn.isEnabled = false
    }

    private fun getSelectedFileTypes(): Array<String> {
        return arrayOf("JPEG", "PNG", "PDF", "MP4", "GIF", "BMP")
    }

    private fun displayResults(outputDir: File) {
        val recoveredFiles = outputDir.listFiles()?.toList() ?: emptyList()
        val count = carver.getRecoveredCount()

        statusText.text = "✅ Found $count files!"

        val fileInfo = recoveredFiles.map { file ->
            val sizeKB = file.length() / 1024
            val sizeMB = sizeKB / 1024
            val sizeStr = if (sizeMB > 0) "${sizeMB}MB" else "${sizeKB}KB"
            "${file.name} ($sizeStr)"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            fileInfo
        )
        fileListView.adapter = adapter

        fileListView.setOnItemClickListener { _, _, position, _ ->
            val file = recoveredFiles[position]
            previewFile(file)
        }
    }

    private fun previewFile(file: File) {
        val extension = file.extension.lowercase()
        when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp" -> {
                // Show image preview
                AlertDialog.Builder(this)
                    .setTitle(file.name)
                    .setMessage("File: ${file.absolutePath}\nSize: ${file.length()} bytes")
                    .setPositiveButton("OK", null)
                    .show()
            }
            else -> {
                Toast.makeText(this, "Preview not available for ${extension.uppercase()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        carver.destroyCarver()
        super.onDestroy()
    }
}
