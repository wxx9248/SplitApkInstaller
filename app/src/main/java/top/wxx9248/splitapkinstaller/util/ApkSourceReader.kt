package top.wxx9248.splitapkinstaller.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.wxx9248.splitapkinstaller.model.ApkInfo
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ApkSourceReader {

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
        SplitConfigClassifier.enrichApkMetadata(apks)
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
        SplitConfigClassifier.enrichApkMetadata(apks)
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

    /**
     * Validates if a DocumentFile is an APK file.
     */
    private fun isApkFile(file: DocumentFile): Boolean {
        return file.isFile && file.name?.endsWith(".apk", ignoreCase = true) == true
    }
}
