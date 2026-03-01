package top.wxx9248.splitapkinstaller.core

import android.net.Uri
import top.wxx9248.splitapkinstaller.model.ApkInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache manager for storing parsed APK data to avoid re-parsing when navigating back
 * from InstallationScreen to ApkListScreen.
 */
object ApkCacheManager {
    private val cache = ConcurrentHashMap<String, CachedApkData>()

    data class CachedApkData(
        val apks: List<ApkInfo>,
        val selectedApks: Set<ApkInfo>,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Generate a cache key based on package URI and file type
     */
    private fun generateCacheKey(packageUri: Uri, isFile: Boolean): String {
        return "${packageUri}_${isFile}"
    }

    /**
     * Store parsed APK data in cache
     */
    fun cacheApkData(
        packageUri: Uri,
        isFile: Boolean,
        apks: List<ApkInfo>,
        selectedApks: Set<ApkInfo>
    ) {
        val key = generateCacheKey(packageUri, isFile)
        cache[key] = CachedApkData(apks, selectedApks)
    }

    /**
     * Retrieve cached APK data if available
     */
    fun getCachedApkData(packageUri: Uri, isFile: Boolean): CachedApkData? {
        val key = generateCacheKey(packageUri, isFile)
        return cache[key]
    }

    /**
     * Update selected APKs in cache without re-parsing
     */
    fun updateSelectedApks(
        packageUri: Uri,
        isFile: Boolean,
        selectedApks: Set<ApkInfo>
    ) {
        val key = generateCacheKey(packageUri, isFile)
        cache[key]?.let { cachedData ->
            cache[key] = cachedData.copy(selectedApks = selectedApks)
        }
    }

    /**
     * Clear cache entry for specific package
     */
    fun clearCache(packageUri: Uri, isFile: Boolean) {
        val key = generateCacheKey(packageUri, isFile)
        cache.remove(key)
    }

}
