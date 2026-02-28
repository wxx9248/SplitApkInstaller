package top.wxx9248.splitapkinstaller.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.model.ApkInfo
import top.wxx9248.splitapkinstaller.model.SplitConfigType
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ApkUtil {

    data class Result<T>(
        val entries: T? = null,
        val error: Throwable? = null
    ) {
        val isSuccess: Boolean get() = error == null && entries != null
        val isFailure: Boolean get() = !isSuccess
    }

    // ========== Metadata Operations ==========

    /**
     * Reads APK metadata from a package (ZIP file or folder).
     */
    suspend fun readApksMetaFromPackage(
        context: Context,
        packageUri: Uri,
        isFile: Boolean
    ): Result<List<ApkInfo>> {
        val result = if (isFile) {
            processZipForMetadata(context, packageUri)
        } else {
            processFolderForMetadata(context, packageUri)
        }
        return result
    }

    /**
     * Processes ZIP entries to extract APK metadata.
     */
    suspend fun processZipForMetadata(
        context: Context,
        zipUri: Uri
    ): Result<List<ApkInfo>> = processZip(context, zipUri) { zipStream ->
        val apks = mutableListOf<ApkInfo>()
        processZipEntries(zipStream) { entry ->
            val apkName = entry.name.substringAfterLast("/")
            apks.add(
                ApkInfo(
                    name = apkName,
                    size = if (entry.size >= 0) entry.size else 0
                )
            )
        }
        enrichApkMetadata(apks)
    }

    /**
     * Processes folder contents to extract APK metadata.
     */
    suspend fun processFolderForMetadata(
        context: Context,
        folderUri: Uri
    ): Result<List<ApkInfo>> = processFolder(context, folderUri) { documentFile ->
        val apks = mutableListOf<ApkInfo>()
        documentFile.listFiles().forEach { file ->
            if (isApkFile(file)) {
                // If we can reach here, name of the file shouldn't be null - already checked in `isApkFile`.
                apks.add(
                    ApkInfo(
                        name = file.name!!,
                        size = file.length()
                    )
                )
            }
        }
        enrichApkMetadata(apks)
    }

    // ========== File Extraction Operations ==========

    /**
     * Processes ZIP entries to extract APK files to temporary storage.
     */
    suspend fun processZipForExtraction(
        context: Context,
        zipUri: Uri,
        selectedApkNames: List<String>
    ): Result<List<File>> = processZip(context, zipUri) { zipStream ->
        val tempFiles = mutableListOf<File>()
        processZipEntries(zipStream) { entry ->
            val fileName = entry.name.substringAfterLast("/")
            if (selectedApkNames.contains(fileName)) {
                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { output ->
                    zipStream.copyTo(output)
                }
                tempFiles.add(tempFile)
            }
        }
        tempFiles
    }

    /**
     * Processes folder contents to extract APK files to temporary storage.
     */
    suspend fun processFolderForExtraction(
        context: Context,
        folderUri: Uri,
        selectedApkNames: List<String>
    ): Result<List<File>> = processFolder(context, folderUri) { documentFile ->
        val tempFiles = mutableListOf<File>()
        documentFile.listFiles().forEach { file ->
            if (isApkFile(file) && selectedApkNames.contains(file.name)) {
                val fileName = file.name!!
                val tempFile = File(context.cacheDir, fileName)

                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFiles.add(tempFile)
            }
        }
        tempFiles
    }

    // ========== Utility Functions ==========

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

    /**
     * Validates if a DocumentFile is an APK file.
     */
    fun isApkFile(file: DocumentFile): Boolean {
        return file.isFile && file.name?.endsWith(".apk", ignoreCase = true) == true
    }

    // ========== Private Helper Functions ==========

    /**
     * Generic ZIP processing function that handles common ZIP operations.
     */
    private suspend inline fun <T> processZip(
        context: Context,
        zipUri: Uri,
        crossinline operation: (ZipInputStream) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(zipUri)
                ?: throw SecurityException("Cannot access file")

            inputStream.use { stream ->
                ZipInputStream(stream).use { zipStream ->
                    Result(entries = operation(zipStream))
                }
            }
        } catch (e: Exception) {
            Result(error = e)
        }
    }

    /**
     * Generic folder processing function that handles common folder operations.
     */
    private suspend inline fun <T> processFolder(
        context: Context,
        folderUri: Uri,
        crossinline operation: (DocumentFile) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw SecurityException("Cannot access folder")

            Result(entries = operation(documentFile))
        } catch (e: Exception) {
            Result(error = e)
        }
    }

    /**
     * Processes ZIP entries with a callback for each APK entry found.
     */
    private inline fun processZipEntries(
        zipStream: ZipInputStream,
        onApkEntry: (entry: java.util.zip.ZipEntry) -> Unit
    ) {
        var entry = zipStream.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                onApkEntry(entry)
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }
    }

    // ========== Split Config Classification ==========

    private val ABI_QUALIFIERS = setOf(
        "arm64_v8a", "armeabi_v7a", "armeabi", "x86", "x86_64", "mips", "mips64"
    )

    private val DENSITY_QUALIFIERS = setOf(
        "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi"
    )

    private val LANGUAGE_PATTERN = Regex("^[a-z]{2,3}(_[A-Z]{2})?$")

    /**
     * Extracts the config qualifier from a canonical APK name.
     *
     * Handles two patterns:
     * - Prefix: `split_config.`, `split_config_`, `config.` at the start
     *   e.g. `split_config.en` → `en`, `split_config_arm64_v8a` → `arm64_v8a`
     * - Infix: `.config.` embedded after a feature name
     *   e.g. `split_phonesky_webrtc_native_lib.config.x86` → `x86`
     *
     * @return The qualifier string, or null if no config pattern is found.
     */
    private fun extractConfigQualifier(canonicalName: String): String? {
        // Check prefix patterns first
        val prefixes = listOf("split_config.", "split_config_", "config.")
        for (prefix in prefixes) {
            if (canonicalName.startsWith(prefix, ignoreCase = true)) {
                return canonicalName.substring(prefix.length)
            }
        }

        // Check infix `.config.` pattern (e.g. feature_name.config.qualifier)
        val infixMarker = ".config."
        val infixIndex = canonicalName.lastIndexOf(infixMarker, ignoreCase = true)
        if (infixIndex >= 0) {
            return canonicalName.substring(infixIndex + infixMarker.length)
        }

        return null
    }

    /**
     * Classifies a canonical APK name into its split config type.
     * Priority: ABI → Density → Language → None.
     */
    internal fun classifySplitConfig(canonicalName: String): SplitConfigType {
        val qualifier = extractConfigQualifier(canonicalName) ?: return SplitConfigType.None
        if (qualifier.isEmpty()) return SplitConfigType.None

        if (qualifier.lowercase() in ABI_QUALIFIERS) return SplitConfigType.Abi(qualifier)
        if (qualifier.lowercase() in DENSITY_QUALIFIERS) return SplitConfigType.Density(qualifier)
        if (LANGUAGE_PATTERN.matches(qualifier)) return SplitConfigType.Language(qualifier)

        return SplitConfigType.None
    }

    /**
     * Computes canonical names by stripping common prefix/suffix affixes
     * (e.g. tool-added suffixes like `-430-lspatched`) from APK filenames.
     *
     * Shared between base APK detection and config classification.
     */
    internal fun computeCanonicalNames(apkFileNames: List<String>): List<String> {
        if (apkFileNames.size <= 1) {
            return apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }
        }

        val stems = apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }

        val rawPrefix = longestCommonPrefix(stems)
        val prefixEnd = rawPrefix.lastIndexOfAny(SEPARATORS)
        val prefix = if (prefixEnd >= 0) rawPrefix.substring(0, prefixEnd + 1) else ""

        val afterPrefix = stems.map { it.removePrefix(prefix) }
        val rawSuffix = longestCommonSuffix(afterPrefix)
        val suffixStart = rawSuffix.indexOfAny(SEPARATORS)
        val suffix = if (suffixStart >= 0) rawSuffix.substring(suffixStart) else ""

        return stems.map { it.removePrefix(prefix).removeSuffix(suffix) }
    }

    /**
     * Enriches raw APK metadata with base detection and split config classification.
     * Applies affix-stripping once, then uses canonical names for both operations.
     */
    private fun enrichApkMetadata(apks: List<ApkInfo>): List<ApkInfo> {
        val fileNames = apks.map { it.name }
        val baseApkNames = resolveBaseApks(fileNames)
        val canonicalNames = computeCanonicalNames(fileNames)
        return apks.mapIndexed { i, apk ->
            apk.copy(
                isBase = apk.name in baseApkNames,
                splitConfigType = if (apk.name in baseApkNames) SplitConfigType.None
                else classifySplitConfig(canonicalNames[i])
            )
        }.sortedWith(compareBy<ApkInfo> { !it.isBase }.thenBy { it.name })
    }

    // ========== Base APK Resolution ==========

    private val SEPARATORS = charArrayOf('-', '_')

    /**
     * Determines which APK filenames correspond to the base APK by stripping
     * common prefix/suffix (at separator boundaries) to recover canonical names.
     *
     * For example, LSPatch-modified names like `base-430-lspatched.apk` and
     * `split_config_arm64_v8a-430-lspatched.apk` share the suffix `-430-lspatched`.
     * Stripping it yields `base.apk`, which is identified as the base APK.
     *
     * @return Set of original filenames that are base APKs (typically 0 or 1 element).
     */
    private fun resolveBaseApks(apkFileNames: List<String>): Set<String> {
        if (apkFileNames.size <= 1) {
            // Single APK: fall back to exact name check (no common affixes computable)
            return apkFileNames.filterTo(mutableSetOf()) {
                it.equals("base.apk", ignoreCase = true)
            }
        }

        val stems = apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }

        // Compute common prefix, truncated to last separator boundary
        val rawPrefix = longestCommonPrefix(stems)
        val prefixEnd = rawPrefix.lastIndexOfAny(SEPARATORS)
        val prefix = if (prefixEnd >= 0) rawPrefix.substring(0, prefixEnd + 1) else ""

        // Strip prefix, then compute common suffix truncated to first separator boundary
        val afterPrefix = stems.map { it.removePrefix(prefix) }
        val rawSuffix = longestCommonSuffix(afterPrefix)
        val suffixStart = rawSuffix.indexOfAny(SEPARATORS)
        val suffix = if (suffixStart >= 0) rawSuffix.substring(suffixStart) else ""

        return apkFileNames.zip(stems).mapNotNullTo(mutableSetOf()) { (original, stem) ->
            val canonical = stem.removePrefix(prefix).removeSuffix(suffix)
            if (canonical.equals("base", ignoreCase = true)) original else null
        }
    }

    /**
     * Computes the longest common prefix of a list of strings, character-by-character.
     * O(N * min_len) time, O(1) extra space.
     */
    private fun longestCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        val minLen = strings.minOf { it.length }
        for (i in 0 until minLen) {
            val c = strings[0][i]
            if (strings.any { it[i] != c }) return strings[0].substring(0, i)
        }
        return strings[0].substring(0, minLen)
    }

    /**
     * Computes the longest common suffix of a list of strings, character-by-character.
     * O(N * min_len) time, O(1) extra space.
     */
    private fun longestCommonSuffix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        val minLen = strings.minOf { it.length }
        for (i in 1..minLen) {
            val c = strings[0][strings[0].length - i]
            if (strings.any { it[it.length - i] != c }) {
                return strings[0].substring(strings[0].length - i + 1)
            }
        }
        return strings[0].substring(strings[0].length - minLen)
    }
}
