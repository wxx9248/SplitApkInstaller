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

class PackageInstallerManager(private val context: Context) {

    interface InstallationCallback {
        fun onLogUpdate(log: LogEntry)
        fun onProgress(progress: Int, total: Int)
        fun onInstallationComplete(success: Boolean, resultCode: Int?, message: String?)
    }

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