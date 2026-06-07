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
        val level: String, // INFO, WARNING, ERROR, SUCCESS
        val message: String,
        val details: String = ""
    )

    fun startNewLog(fileName: String = "recovery_${System.currentTimeMillis()}.log") {
        logs.clear()
        currentLogFile = File(logDir, fileName)
        log("INFO", "=== Recovery Session Started ===")
        log("INFO", "Device: ${android.os.Build.DEVICE}\")\n        log(\"INFO\", \"Android Version: ${android.os.Build.VERSION.SDK_INT}\")\n        log(\"INFO\", \"Timestamp: ${getCurrentTimestamp()}\")\n    }\n\n    fun log(level: String, message: String, details: String = \"\") {\n        val timestamp = getCurrentTimestamp()\n        val entry = LogEntry(timestamp, level, message, details)\n        logs.add(entry)\n\n        // Write to file\n        currentLogFile?.let { file ->\n            val logLine = \"[$timestamp] [$level] $message${if (details.isNotEmpty()) \"\\n  Details: $details\" else \"\"}\"\n            file.appendText(logLine + \"\\n\")\n        }\n    }\n\n    fun logFileFound(\n        fileName: String,\n        fileType: String,\n        offset: Long,\n        size: Long,\n        confidence: Int = 100\n    ) {\n        log(\n            \"SUCCESS\",\n            \"File Found: $fileName\",\n            \"Type: $fileType | Offset: 0x${offset.toString(16).uppercase()} | Size: ${formatFileSize(size)} | Confidence: $confidence%\"\n        )\n    }\n\n    fun logRecoveryStart(inputPath: String, maxSize: Int, fileTypes: Array<String>) {\n        log(\"INFO\", \"=== Recovery Started ===\")\n        log(\"INFO\", \"Input File: $inputPath\")\n        log(\"INFO\", \"Max Size Limit: ${formatFileSize(maxSize.toLong() * 1024 * 1024)}\")\n        log(\"INFO\", \"File Types: ${fileTypes.joinToString(\", \")}\")\n    }\n\n    fun logRecoveryProgress(processedBytes: Long, totalBytes: Long) {\n        val percentage = (processedBytes * 100) / totalBytes\n        val progressBar = \"[\".padEnd(20 + (percentage / 5).toInt(), '█').padEnd(21, '░') + \"]\"\n        log(\n            \"INFO\",\n            \"Progress: $progressBar $percentage%\",\n            \"Processed: ${formatFileSize(processedBytes)} / ${formatFileSize(totalBytes)}\"\n        )\n    }\n\n    fun logSignatureSearch(signatureType: String, foundCount: Int) {\n        log(\"INFO\", \"Searching for $signatureType signatures... Found: $foundCount\")\n    }\n\n    fun logRecoveryComplete(totalRecovered: Int, duration: Long) {\n        log(\"SUCCESS\", \"=== Recovery Complete ===\")\n        log(\"INFO\", \"Total Files Recovered: $totalRecovered\")\n        log(\"INFO\", \"Duration: ${formatDuration(duration)}\")\n        log(\"INFO\", \"Log saved: ${currentLogFile?.absolutePath}\")\n    }\n\n    fun logError(message: String, exception: Throwable? = null) {\n        val details = exception?.message ?: \"\"\n        log(\"ERROR\", message, details)\n    }\n\n    fun logWarning(message: String, details: String = \"\") {\n        log(\"WARNING\", message, details)\n    }\n\n    fun getLogs(): List<LogEntry> = logs.toList()\n\n    fun getFormattedLogs(): String {\n        return logs.joinToString(\"\\n\") { entry ->\n            \"[${entry.timestamp}] [${entry.level}] ${entry.message}${if (entry.details.isNotEmpty()) \"\\n  └─ ${entry.details}\" else \"\"}\"\n        }\n    }\n\n    fun exportLog(): File? {\n        return currentLogFile\n    }\n\n    fun getAllLogs(): List<File> {\n        return logDir.listFiles()?.toList() ?: emptyList()\n    }\n\n    fun clearOldLogs(daysOld: Int = 7) {\n        val currentTime = System.currentTimeMillis()\n        val daysInMillis = daysOld * 24 * 60 * 60 * 1000L\n\n        logDir.listFiles()?.forEach { file ->\n            if (currentTime - file.lastModified() > daysInMillis) {\n                file.delete()\n                log(\"INFO\", \"Deleted old log: ${file.name}\")\n            }\n        }\n    }\n\n    private fun getCurrentTimestamp(): String {\n        return SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss.SSS\", Locale.getDefault()).format(Date())\n    }\n\n    private fun formatFileSize(bytes: Long): String {\n        return when {\n            bytes >= 1024 * 1024 * 1024 -> String.format(\"%.2f GB\", bytes / (1024.0 * 1024.0 * 1024.0))\n            bytes >= 1024 * 1024 -> String.format(\"%.2f MB\", bytes / (1024.0 * 1024.0))\n            bytes >= 1024 -> String.format(\"%.2f KB\", bytes / 1024.0)\n            else -> \"$bytes B\"\n        }\n    }\n\n    private fun formatDuration(millis: Long): String {\n        val seconds = millis / 1000\n        val minutes = seconds / 60\n        val hours = minutes / 60\n\n        return when {\n            hours > 0 -> String.format(\"%02d:%02d:%02d\", hours, minutes % 60, seconds % 60)\n            minutes > 0 -> String.format(\"%02d:%02d\", minutes, seconds % 60)\n            else -> String.format(\"%02d seconds\", seconds)\n        }\n    }\n}\n