package com.recovery.filecarver

data class ScanProgress(
    val processedBytes: Long,
    val totalBytes: Long,
    val filesFound: Int,
    val confidenceAvg: Int
) {
    val percentComplete: Int
        get() = if (totalBytes > 0) ((processedBytes * 100) / totalBytes).toInt() else 0
}

data class RecoveredFileInfo(
    val path: String,
    val name: String,
    val type: String,
    val size: Long,
    val offset: Long,
    val confidence: Int
) {
    val formattedSize: String
        get() = when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    
    val formattedOffset: String
        get() = "0x${offset.toString(16).uppercase().padStart(8, '0')}"
    
    val statusLabel: String
        get() = when {
            confidence >= 90 -> "✅ Excellent"
            confidence >= 70 -> "👍 Good"
            confidence >= 50 -> "⚠️ Fair"
            else -> "❌ Poor"
        }
}
