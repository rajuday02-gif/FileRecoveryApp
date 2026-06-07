package com.recovery.filecarver

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogViewerActivity : AppCompatActivity() {

    private lateinit var logListView: ListView
    private lateinit var logDetailsView: TextView
    private lateinit var logger: RecoveryLogger
    private var selectedLogFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        supportActionBar?.title = "Recovery Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        logListView = findViewById(R.id.log_list_view)
        logDetailsView = findViewById(R.id.log_details_view)
        logger = RecoveryLogger(this)

        loadLogs()
    }

    private fun loadLogs() {
        val logFiles = logger.getAllLogs()
        val logNames = logFiles.map { file ->
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(file.lastModified()))
            "${file.name}\n$date (${file.length() / 1024}KB)"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, logNames)
        logListView.adapter = adapter

        logListView.setOnItemClickListener { _, _, position, _ ->
            selectedLogFile = logFiles[position]
            displayLogContent(selectedLogFile!!)
        }
    }

    private fun displayLogContent(file: File) {
        val content = file.readText()
        logDetailsView.text = content
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.log_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                if (selectedLogFile != null) {
                    shareLog(selectedLogFile!!)
                }
                true
            }
            R.id.action_delete -> {
                if (selectedLogFile != null) {
                    deleteLog(selectedLogFile!!)
                }
                true
            }
            R.id.action_clear_all -> {
                clearAllLogs()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareLog(file: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, file.readText())
        intent.putExtra(Intent.EXTRA_SUBJECT, "Recovery Log: ${file.name}")
        startActivity(Intent.createChooser(intent, "Share Log"))
    }

    private fun deleteLog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete Log?")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                file.delete()
                loadLogs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllLogs() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Logs?")
            .setMessage("This will delete all recovery logs. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                logger.getAllLogs().forEach { it.delete() }
                loadLogs()
                logDetailsView.text = ""
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
