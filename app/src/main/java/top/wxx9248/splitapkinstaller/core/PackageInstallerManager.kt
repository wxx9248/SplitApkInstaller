package top.wxx9248.splitapkinstaller.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.receiver.InstallationResultReceiver
import top.wxx9248.splitapkinstaller.ui.components.LogEntry
import top.wxx9248.splitapkinstaller.ui.components.LogLevel
import top.wxx9248.splitapkinstaller.util.ApkUtil
import java.io.File

/**
 * Manages APK installation using Android's PackageInstaller API.
 * Handles extraction of APK files from ZIP archives or folders and installs them as split APKs.
 *
 * @param context The application context used for accessing system services and resources
 */
class PackageInstallerManager(private val context: Context) {

    /**
     * Callback interface for receiving installation progress updates and results.
     */
    interface InstallationCallback {
        /**
         * Called when a new log entry is generated during installation.
         *
         * @param log The log entry containing level and message information
         */
        fun onLogUpdate(log: LogEntry)

        /**
         * Called to report installation progress.
         *
         * @param progress Current progress value
         * @param total Total progress value
         */
        fun onProgress(progress: Int, total: Int)

        /**
         * Called when installation is complete.
         *
         * @param success Whether the installation was successful
         * @param resultCode The result code from the installation process, if available
         * @param message A descriptive message about the installation result
         */
        fun onInstallationComplete(success: Boolean, resultCode: Int?, message: String?)
    }

    /**
     * Installs selected APK files from a package (ZIP file or folder).
     * This is the main entry point for APK installation.
     *
     * @param packageUri URI pointing to the package containing APK files
     * @param isFile Whether the package is a file (true) or folder (false)
     * @param selectedApkNames List of APK file names to install
     * @param callback Callback interface for receiving progress updates and results
     */
    suspend fun installApks(
        packageUri: Uri,
        isFile: Boolean,
        selectedApkNames: List<String>,
        callback: InstallationCallback
    ) = withContext(Dispatchers.IO) {
        try {
            callback.onLogUpdate(
                LogEntry(
                    level = LogLevel.INFO,
                    message = context.getString(R.string.starting_apk_installation)
                )
            )

            val tempFiles = mutableListOf<File>()

            try {
                // Extract APKs to temporary files
                callback.onLogUpdate(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = context.getString(R.string.extracting_apk_files)
                    )
                )

                if (isFile) {
                    tempFiles.addAll(extractApksFromZipFile(packageUri, selectedApkNames, callback))
                } else {
                    tempFiles.addAll(extractApksFromFolder(packageUri, selectedApkNames, callback))
                }

                if (tempFiles.isEmpty()) {
                    callback.onLogUpdate(
                        LogEntry(
                            level = LogLevel.ERROR,
                            message = context.getString(R.string.no_apk_files_extracted)
                        )
                    )
                    callback.onInstallationComplete(
                        false,
                        null,
                        context.getString(R.string.no_apk_files_extracted)
                    )
                    return@withContext
                }

                callback.onLogUpdate(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = context.getString(R.string.extracted_files_count, tempFiles.size)
                    )
                )

                // Install using PackageInstaller
                installUsingPackageInstaller(tempFiles, callback)

            } finally {
                // Clean up temporary files
                callback.onLogUpdate(
                    LogEntry(
                        level = LogLevel.INFO,
                        message = context.getString(R.string.cleaning_up_temp_files)
                    )
                )
                cleanupTempFiles(tempFiles, callback)
            }

        } catch (e: Exception) {
            handleInstallationError(e, callback)
        }
    }

    /**
     * Handles installation errors by logging them and notifying the callback.
     *
     * @param e The exception that occurred during installation
     * @param callback The callback to notify about the error
     */
    private fun handleInstallationError(
        e: Exception,
        callback: InstallationCallback
    ) {
        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.ERROR,
                message = context.getString(
                    R.string.error_during_installation,
                    e.message ?: context.getString(R.string.unknown_error)
                )
            )
        )
        callback.onInstallationComplete(
            false,
            null,
            context.getString(
                R.string.installation_failed_error_message,
                e.message ?: context.getString(R.string.unknown_error)
            )
        )
    }

    /**
     * Creates a new PackageInstaller session for installing APK files.
     *
     * @param packageInstaller The PackageInstaller instance to use
     * @param callback The callback to notify about session creation progress
     * @return The session ID of the created installation session
     */
    private fun createInstallationSession(
        packageInstaller: PackageInstaller,
        callback: InstallationCallback
    ): Int {
        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.INFO,
                message = context.getString(R.string.creating_installation_session)
            )
        )

        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(null) // Let system determine package name

        val sessionId = packageInstaller.createSession(params)
        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.INFO,
                message = context.getString(R.string.installation_session_created, sessionId)
            )
        )

        return sessionId
    }

    /**
     * Writes APK files to the installation session.
     *
     * @param session The PackageInstaller session to write files to
     * @param apkFiles List of APK files to write to the session
     * @param callback The callback to notify about write progress
     */
    private fun writeApkFilesToSession(
        session: PackageInstaller.Session,
        apkFiles: List<File>,
        callback: InstallationCallback
    ) {
        apkFiles.forEachIndexed { index, apkFile ->
            callback.onLogUpdate(
                LogEntry(
                    level = LogLevel.INFO,
                    message = context.getString(R.string.writing_file_to_session, apkFile.name)
                )
            )

            session.openWrite(apkFile.name, 0, apkFile.length()).use { outputStream ->
                apkFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            callback.onLogUpdate(
                LogEntry(
                    level = LogLevel.SUCCESS,
                    message = context.getString(R.string.written_file_success, apkFile.name)
                )
            )
            callback.onProgress(index + 1, apkFiles.size)
        }

        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.INFO,
                message = context.getString(R.string.all_files_written_to_session)
            )
        )
    }

    /**
     * Commits the installation session to start the actual installation process.
     *
     * @param session The PackageInstaller session to commit
     * @param callback The callback to notify about commit progress
     */
    private fun commitInstallationSession(
        session: PackageInstaller.Session,
        callback: InstallationCallback
    ) {
        val intent = Intent(context, InstallationResultReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.INFO,
                message = context.getString(R.string.committing_installation_session)
            )
        )

        session.commit(pendingIntent.intentSender)

        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.INFO,
                message = context.getString(R.string.installation_session_committed)
            )
        )
    }

    /**
     * Handles errors that occur during session operations.
     *
     * @param session The PackageInstaller session where the error occurred
     * @param e The exception that occurred
     * @param callback The callback to notify about the error
     */
    private fun handleSessionError(
        session: PackageInstaller.Session,
        e: Exception,
        callback: InstallationCallback
    ) {
        callback.onLogUpdate(
            LogEntry(
                level = LogLevel.ERROR,
                message = context.getString(
                    R.string.error_during_installation,
                    e.message ?: context.getString(R.string.unknown_error)
                )
            )
        )
        session.abandon()
        callback.onInstallationComplete(
            false,
            null,
            context.getString(
                R.string.installation_failed_error_message,
                e.message ?: context.getString(R.string.unknown_error)
            )
        )
    }

    /**
     * Extracts selected APK files from a ZIP archive to temporary storage.
     *
     * @param packageUri URI pointing to the ZIP file
     * @param selectedApkNames List of APK file names to extract
     * @param callback The callback to notify about extraction progress
     * @return List of extracted temporary files
     */
    private suspend fun extractApksFromZipFile(
        packageUri: Uri,
        selectedApkNames: List<String>,
        callback: InstallationCallback
    ): List<File> = withContext(Dispatchers.IO) {
        val result = ApkUtil.processZipForExtraction(context, packageUri, selectedApkNames)

        if (result.isFailure) {
            throw result.error ?: Exception("Unknown error during ZIP extraction")
        }

        callback.onProgress(selectedApkNames.size, selectedApkNames.size)
        result.entries ?: emptyList()
    }

    /**
     * Extracts selected APK files from a folder to temporary storage.
     *
     * @param packageUri URI pointing to the folder
     * @param selectedApkNames List of APK file names to extract
     * @param callback The callback to notify about extraction progress
     * @return List of extracted temporary files
     */
    private suspend fun extractApksFromFolder(
        packageUri: Uri,
        selectedApkNames: List<String>,
        callback: InstallationCallback
    ): List<File> = withContext(Dispatchers.IO) {
        val result = ApkUtil.processFolderForExtraction(context, packageUri, selectedApkNames)

        if (result.isFailure) {
            throw result.error ?: Exception("Unknown error during folder extraction")
        }

        callback.onProgress(selectedApkNames.size, selectedApkNames.size)
        result.entries ?: emptyList()
    }

    /**
     * Performs the actual APK installation using Android's PackageInstaller API.
     *
     * @param apkFiles List of APK files to install
     * @param callback The callback to notify about installation progress
     */
    private suspend fun installUsingPackageInstaller(
        apkFiles: List<File>,
        callback: InstallationCallback
    ) = withContext(Dispatchers.IO) {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionId = createInstallationSession(packageInstaller, callback)
        val session = packageInstaller.openSession(sessionId)

        try {
            InstallationResultReceiver.Companion.registerCallback(sessionId, callback)

            writeApkFilesToSession(session, apkFiles, callback)
            commitInstallationSession(session, callback)
        } catch (e: Exception) {
            InstallationResultReceiver.Companion.unregisterCallback(sessionId)
            handleSessionError(session, e, callback)
        } finally {
            session.close()
        }
    }

    /**
     * Cleans up temporary files created during the installation process.
     *
     * @param tempFiles List of temporary files to delete
     * @param callback The callback to notify about cleanup progress
     */
    private fun cleanupTempFiles(
        tempFiles: List<File>,
        callback: InstallationCallback
    ) {
        tempFiles.forEach { file ->
            try {
                if (file.exists() && file.delete()) {
                    callback.onLogUpdate(
                        LogEntry(
                            level = LogLevel.INFO,
                            message = context.getString(R.string.deleted_temp_file, file.name)
                        )
                    )
                }
            } catch (e: Exception) {
                callback.onLogUpdate(
                    LogEntry(
                        level = LogLevel.WARNING,
                        message = context.getString(
                            R.string.failed_delete_temp_file,
                            file.name,
                            e.message ?: context.getString(R.string.unknown_error)
                        )
                    )
                )
            }
        }
    }
}
