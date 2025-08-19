package top.wxx9248.splitapkinstaller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.core.PackageInstallerManager
import java.util.concurrent.ConcurrentHashMap

/**
 * BroadcastReceiver that handles installation results from Android's PackageInstaller.
 * Receives installation status updates and notifies registered callbacks about the results.
 */
class InstallationResultReceiver : BroadcastReceiver() {

    /**
     * Receives and processes installation result broadcasts from the system.
     * Handles various installation statuses and notifies appropriate callbacks.
     *
     * @param context The application context
     * @param intent The broadcast intent containing installation result data
     */
    override fun onReceive(context: Context, intent: Intent) {
        val status =
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Handle user action requirement - launch the system installer UI
                val confirmIntent =
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                // Don't call callback yet, wait for user action result
            }

            PackageInstaller.STATUS_SUCCESS -> {
                // Handle successful installation
                notifyCallback(
                    sessionId,
                    true,
                    status,
                    context.getString(R.string.installation_completed_successfully)
                )
            }

            PackageInstaller.STATUS_FAILURE -> {
                // Handle installation failure
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.installation_failed_generic)
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // Handle aborted installation
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    context.getString(R.string.installation_aborted_by_user)
                )
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                // Handle blocked installation
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.installation_blocked)
                )
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                // Handle installation conflict
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.installation_conflict)
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                // Handle incompatible package
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.package_incompatible)
                )
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                // Handle invalid package
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.package_invalid)
                )
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                // Handle storage issues
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.insufficient_storage)
                )
            }

            else -> {
                // Handle unknown status
                notifyCallback(
                    sessionId,
                    false,
                    status,
                    message ?: context.getString(R.string.unknown_installation_error)
                )
            }
        }
    }

    /**
     * Notifies the registered callback about installation completion and cleans up.
     *
     * @param sessionId The installation session ID
     * @param success Whether the installation was successful
     * @param resultCode The result code from the installation process
     * @param message A descriptive message about the installation result
     */
    private fun notifyCallback(sessionId: Int, success: Boolean, resultCode: Int, message: String) {
        if (sessionId != -1) {
            val callback = pendingCallbacks[sessionId]
            callback?.onInstallationComplete(success, resultCode, message)
            pendingCallbacks.remove(sessionId)
        }
    }

    companion object {
        /**
         * Thread-safe map storing callbacks for pending installation sessions.
         */
        private val pendingCallbacks =
            ConcurrentHashMap<Int, PackageInstallerManager.InstallationCallback>()

        /**
         * Registers a callback for a specific installation session.
         *
         * @param sessionId The installation session ID
         * @param callback The callback to register for receiving installation results
         */
        fun registerCallback(
            sessionId: Int,
            callback: PackageInstallerManager.InstallationCallback
        ) {
            pendingCallbacks[sessionId] = callback
        }

        /**
         * Unregisters and removes a callback for a specific installation session.
         *
         * @param sessionId The installation session ID to unregister
         */
        fun unregisterCallback(sessionId: Int) {
            pendingCallbacks.remove(sessionId)
        }
    }
}
