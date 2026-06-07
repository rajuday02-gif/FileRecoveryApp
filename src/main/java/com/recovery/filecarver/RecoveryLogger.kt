package com.recovery.filecarver

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecoveryLogger(private val context: Context) {

    private val logDir = File(context.getExternalFilesDir(null), "recovery_logs")
    private var currentLogFile: File? = null
    private val logs = mutableListOf<LogEntry>()

    init {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String,
        val details: String = ""
    )

    fun startNewLog(fileName: String = "recovery_${System.currentTimeMillis()}.log") {
        logs.clear()
        currentLogFile = File(logDir, fileName)
        log("INFO", "=== Recovery Session Started ===")
        log("INFO", "Device: ${android.os.Build.DEVICE}")
        log("INFO", "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        log("INFO", "Timestamp: ${getCurrentTimestamp()}")
    }

    fun log(level: String, message: String, details: String = "") {
        val timestamp = getCurrentTimestamp()
        val entry = LogEntry(timestamp, level, message, details)
        logs.add(entry)

        currentLogFile?.let { file ->
            val logLine = "[$timestamp] [$level] $message${if (details.isNotEmpty()) "\n  Details: $details" else ""}"
            file.appendText(logLine + "\n")
        }
    }

    fun logFileFound(
        fileName: String,
        fileType: String,
        offset: Long,
        size: Long,
        confidence: Int = 100
    ) {
        log(
            "SUCCESS",
            "File Found: $fileName",
            "Type: $fileType | Offset: 0x${offset.toString(16).uppercase()} | Size: ${formatFileSize(size)} | Confidence: $confidence%"
        )
    }

    fun logRecoveryStart(inputPath: String, maxSize: Int, fileTypes: Array<String>) {
        log("INFO", "=== Recovery Started ===")
        log("INFO", "Input File: $inputPath")
        log("INFO", "Max Size Limit: ${formatFileSize(maxSize.toLong() * 1024 * 1024)}")
        log("INFO", "File Types: ${fileTypes.joinToString(", ")}")
    }

    fun logRecoveryProgress(processedBytes: Long, totalBytes: Long) {
        val percentage = (processedBytes * 100) / totalBytes
        log(
            "INFO",
            "Progress: $percentage%",
            "Processed: ${formatFileSize(processedBytes)} / ${formatFileSize(totalBytes)}"
        )
    }

    fun logSignatureSearch(signatureType: String, foundCount: Int) {
        log("INFO", "Searching for $signatureType signatures... Found: $foundCount")
    }

    fun logRecoveryComplete(totalRecovered: Int, duration: Long) {
        log("SUCCESS", "=== Recovery Complete ===")
        log("INFO", "Total Files Recovered: $totalRecovered")
        log("INFO", "Duration: ${formatDuration(duration)}")
        log("INFO", "Log saved: ${currentLogFile?.absolutePath}")
    }

    fun logError(message: String, exception: Throwable? = null) {
        val details = exception?.message ?: ""
        log("ERROR", message, details)
    }

    fun logWarning(message: String, details: String = "") {
        log("WARNING", message, details)
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun getFormattedLogs(): String {
        return logs.joinToString("\n") { entry ->
            "[$${entry.timestamp}] [$${entry.level}] $${entry.message}$${if (entry.details.isNotEmpty()) "\n  └─ $${entry.details}" else ""}"
        }
    }

    fun exportLog(): File? {
        return currentLogFile
    }

    fun getAllLogs(): List<File> {
        return logDir.listFiles()?.toList() ?: emptyList()
    }

    fun clearOldLogs(daysOld: Int = 7) {
        val currentTime = System.currentTimeMillis()
        val daysInMillis = daysOld * 24 * 60 * 60 * 1000L

        logDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > daysInMillis) {
                file.delete()
                log("INFO", "Deleted old log: $${file.name}")
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds % 60)
            else -> String.format("%02d seconds", seconds)
        }
    }
}
