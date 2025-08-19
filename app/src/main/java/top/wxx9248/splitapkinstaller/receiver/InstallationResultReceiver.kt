package top.wxx9248.splitapkinstaller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.core.PackageInstallerManager
import java.util.concurrent.ConcurrentHashMap

class InstallationResultReceiver : BroadcastReceiver() {
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

    private fun notifyCallback(sessionId: Int, success: Boolean, resultCode: Int, message: String) {
        if (sessionId != -1) {
            val callback = pendingCallbacks[sessionId]
            callback?.onInstallationComplete(success, resultCode, message)
            pendingCallbacks.remove(sessionId)
        }
    }

    companion object {
        private val pendingCallbacks =
            ConcurrentHashMap<Int, PackageInstallerManager.InstallationCallback>()

        fun registerCallback(
            sessionId: Int,
            callback: PackageInstallerManager.InstallationCallback
        ) {
            pendingCallbacks[sessionId] = callback
        }

        fun unregisterCallback(sessionId: Int) {
            pendingCallbacks.remove(sessionId)
        }
    }
}
