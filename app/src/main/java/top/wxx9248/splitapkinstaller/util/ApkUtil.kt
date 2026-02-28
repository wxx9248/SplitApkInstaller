package top.wxx9248.splitapkinstaller.util

import android.content.Context
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.model.ApkInfo

object ApkUtil {

    /**
     * Formats file size in human-readable format.
     */
    fun formatFileSize(context: Context, bytes: Long): String {
        if (bytes < 1024) return context.getString(R.string.file_size_bytes, bytes)
        val kb = bytes / 1024.0
        if (kb < 1024) return context.getString(R.string.file_size_kb, kb)
        val mb = kb / 1024.0
        if (mb < 1024) return context.getString(R.string.file_size_mb, mb)
        val gb = mb / 1024.0
        return context.getString(R.string.file_size_gb, gb)
    }

    /**
     * Checks if the APK list contains a base APK.
     */
    fun hasBaseApk(apks: List<ApkInfo>): Boolean {
        return apks.any { it.isBase }
    }

    /**
     * Validates APK selection ensuring base APK is included if present.
     */
    fun validateApkSelection(selectedApks: Set<ApkInfo>): Boolean {
        // Must have at least one APK selected
        if (selectedApks.isEmpty()) return false

        // If there's a base APK in the list, it must be selected
        val baseApk = selectedApks.find { it.isBase }
        return baseApk != null
    }
}
