package com.recovery.filecarver

import android.content.Context
import android.content.SharedPreferences
import java.io.File

class StorageAccessManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("storage_config", Context.MODE_PRIVATE)

    /**
     * Get list of available storage paths
     */
    fun getStoragePaths(): List<StoragePath> {
        val paths = mutableListOf<StoragePath>()

        // Internal storage
        val internalDir = context.filesDir.parentFile?.parentFile?.parentFile
        internalDir?.let {
            paths.add(
                StoragePath(
                    path = it.absolutePath,
                    name = "Internal Storage",
                    type = "internal",
                    totalSize = it.totalSpace,
                    freeSize = it.freeSpace,
                    isReadable = it.canRead(),
                    isWritable = it.canWrite()
                )
            )
        }

        // External storage (sdcard)
        val externalDir = context.getExternalFilesDir(null)
        externalDir?.let {
            paths.add(
                StoragePath(
                    path = it.absolutePath,
                    name = "External Storage",
                    type = "external",
                    totalSize = it.totalSpace,
                    freeSize = it.freeSpace,
                    isReadable = it.canRead(),
                    isWritable = it.canWrite()
                )
            )
        }

        return paths
    }

    /**
     * Scan cache directories for deleted files
     */
    fun scanCacheDirectories(): List<CachedFileInfo> {
        val cached = mutableListOf<CachedFileInfo>()

        val cacheDirs = listOf(
            context.cacheDir,
            context.getExternalCacheDir(),
            File("/data/cache"),
            File("/cache")
        )

        for (dir in cacheDirs) {
            if (dir?.exists() == true && dir.isDirectory) {
                dir.walk().forEach { file ->
                    if (file.isFile) {
                        cached.add(
                            CachedFileInfo(
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                canRecover = isRecoverable(file)
                            )
                        )
                    }
                }
            }
        }

        return cached
    }

    /**
     * Check if running with root privileges
     */
    fun checkRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.destroy()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if file is recoverable
     */
    private fun isRecoverable(file: File): Boolean {
        val recoverable = file.canRead() && file.length() > 0
        val supportedExtensions = listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "mp4", "mov", "avi",
            "pdf", "doc", "docx",
            "zip", "rar", "apk",
            "db", "sqlite"
        )
        val extension = file.extension.lowercase()
        return recoverable && supportedExtensions.contains(extension)
    }
}

data class StoragePath(
    val path: String,
    val name: String,
    val type: String,
    val totalSize: Long,
    val freeSize: Long,
    val isReadable: Boolean,
    val isWritable: Boolean
) {
    val usedSize: Long get() = totalSize - freeSize
    val usagePercent: Int get() = ((usedSize * 100) / totalSize).toInt()
}

data class CachedFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val canRecover: Boolean
)
