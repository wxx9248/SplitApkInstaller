package top.wxx9248.splitapkinstaller.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.core.ApkCacheManager
import top.wxx9248.splitapkinstaller.core.PackageInstallerManager
import top.wxx9248.splitapkinstaller.ui.components.ErrorDialog
import top.wxx9248.splitapkinstaller.ui.components.LogEntry
import top.wxx9248.splitapkinstaller.ui.components.LogLevel
import top.wxx9248.splitapkinstaller.ui.components.LogView
import top.wxx9248.splitapkinstaller.ui.components.ProgressCard
import top.wxx9248.splitapkinstaller.ui.components.ProgressState
import top.wxx9248.splitapkinstaller.ui.components.ProgressTexts

@Serializable
data class InstallationRoute(
    val packageUriString: String, val isFile: Boolean, val selectedApkNamesString: String
)

/**
 * Installation screen that manages the APK installation process with progress tracking and logging.
 *
 * @param packageUri URI of the source package (file or folder)
 * @param isFile Whether the URI points to a file (true) or folder (false)
 * @param selectedApkNames List of selected APK file names to install
 * @param onNavigateBack Callback to navigate back to the previous screen
 * @param onNavigateToHome Callback to navigate to the home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationScreen(
    packageUri: Uri,
    isFile: Boolean,
    selectedApkNames: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isInstalling by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var totalProgress by remember { mutableIntStateOf(0) }
    var installationComplete by remember { mutableStateOf(false) }
    var installationSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var showContent by remember { mutableStateOf(false) }
    var errorException by remember { mutableStateOf<Throwable?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Helper function to handle navigation logic
    val handleNavigation = {
        if (isInstalling) {
            showCancelDialog = true
        } else if (installationComplete && installationSuccess) {
            // After successful installation, navigate to home with clean stack
            onNavigateToHome()
        } else {
            // Before installation starts or after failure, allow normal back navigation
            onNavigateBack()
        }
    }

    // Handle system back button
    BackHandler {
        handleNavigation()
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }

    LaunchedEffect(packageUri, selectedApkNames) {
        if (selectedApkNames.isEmpty() || packageUri.toString().isEmpty()) return@LaunchedEffect

        scope.launch {
            startInstallation(
                context = context,
                packageUri = packageUri,
                isFile = isFile,
                selectedApkNames = selectedApkNames,
                onInstallationStart = { isInstalling = true },
                onLogUpdate = { log ->
                    logs = logs + log
                    scope.launch {
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                },
                onProgress = { progressValue, total ->
                    progress = progressValue
                    totalProgress = total
                },
                onComplete = { success, message ->
                    isInstalling = false
                    installationComplete = true
                    installationSuccess = success
                    resultMessage = message

                    val finalLog = createFinalLog(context, success, message)
                    logs = logs + finalLog

                    // Clear cache after successful installation to prevent stale data
                    if (success) {
                        ApkCacheManager.clearCache(packageUri, isFile)
                    }

                    scope.launch {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                },
                onError = { error ->
                    errorException = error
                    isInstalling = false
                    installationComplete = true
                    installationSuccess = false
                })
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        InstallationTopBar(
            onNavigateBack = handleNavigation
        )

        InstallationContent(
            showContent = showContent,
            isInstalling = isInstalling,
            installationComplete = installationComplete,
            installationSuccess = installationSuccess,
            progress = progress,
            totalProgress = totalProgress,
            logs = logs,
            resultMessage = resultMessage,
            onNavigateBack = handleNavigation
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_installation_title)) },
            text = { Text(stringResource(R.string.cancel_installation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onNavigateBack()
                    }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            })
    }

    errorException?.let { exception ->
        ErrorDialog(
            throwable = exception, onDismiss = {
                errorException = null
            })
    }
}

/**
 * Top app bar for the installation screen with back navigation.
 *
 * @param onNavigateBack Callback to handle back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallationTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(title = {
        Text(
            text = stringResource(R.string.installing_apks),
            style = MaterialTheme.typography.titleLarge
        )
    }, navigationIcon = {
        IconButton(
            onClick = onNavigateBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }
    })
}

/**
 * Main content area of the installation screen that displays progress and logs.
 *
 * @param showContent Whether to show the content with animation
 * @param isInstalling Whether installation is currently in progress
 * @param installationComplete Whether installation has completed
 * @param installationSuccess Whether installation was successful
 * @param progress Current installation progress
 * @param totalProgress Total number of APKs to install
 * @param logs List of log entries to display
 * @param resultMessage Final result message from installation
 * @param onNavigateBack Callback to handle back navigation
 */
@Composable
private fun InstallationContent(
    showContent: Boolean,
    isInstalling: Boolean,
    installationComplete: Boolean,
    installationSuccess: Boolean,
    progress: Int,
    totalProgress: Int,
    logs: List<LogEntry>,
    resultMessage: String?,
    onNavigateBack: () -> Unit
) {
    AnimatedVisibility(
        visible = showContent, enter = fadeIn(), exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ProgressCard(
                state = when {
                    isInstalling -> ProgressState.IN_PROGRESS
                    installationComplete && installationSuccess -> ProgressState.COMPLETED_SUCCESS
                    installationComplete && !installationSuccess -> ProgressState.COMPLETED_FAILURE
                    else -> ProgressState.IDLE
                }, progress = progress, totalProgress = totalProgress, texts = ProgressTexts(
                    title = stringResource(R.string.installation_progress),
                    inProgressText = stringResource(R.string.installing),
                    completedSuccessText = stringResource(R.string.completed),
                    completedFailureText = stringResource(R.string.failed),
                    filesProcessedFormat = stringResource(R.string.files_processed_format)
                ), modifier = Modifier.padding(16.dp)
            )

            LogView(
                logs = logs, modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            if (installationComplete) {
                CompletionSection(
                    installationSuccess = installationSuccess,
                    resultMessage = resultMessage,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}


@Composable
private fun CompletionSection(
    installationSuccess: Boolean, resultMessage: String?, onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (installationSuccess) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (installationSuccess) {
                    stringResource(R.string.installation_completed_success_message)
                } else {
                    stringResource(
                        R.string.installation_failed_error_message,
                        resultMessage ?: stringResource(R.string.unknown_error)
                    )
                }, style = MaterialTheme.typography.bodyLarge, color = if (installationSuccess) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }, textAlign = TextAlign.Center
            )

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun InstallationScreenPreview() {
    MaterialTheme {
        InstallationScreen(
            packageUri = "content://example.apk".toUri(),
            isFile = true,
            selectedApkNames = listOf("base.apk", "config.apk"),
            onNavigateBack = {},
            onNavigateToHome = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InstallationTopBarPreview() {
    MaterialTheme {
        InstallationTopBar(
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InstallationContentPreview() {
    MaterialTheme {
        InstallationContent(
            showContent = true,
            isInstalling = true,
            installationComplete = false,
            installationSuccess = false,
            progress = 3,
            totalProgress = 10,
            logs = listOf(
                LogEntry(level = LogLevel.INFO, message = "Starting installation..."),
                LogEntry(level = LogLevel.INFO, message = "Processing APK files..."),
                LogEntry(level = LogLevel.INFO, message = "Installing base.apk...")
            ),
            resultMessage = null,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardInInstallationPreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.IN_PROGRESS,
            progress = 3,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Completed",
                completedFailureText = "Failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardCompleteInInstallationPreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.COMPLETED_SUCCESS,
            progress = 10,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Completed",
                completedFailureText = "Failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun CompletionSectionSuccessPreview() {
    MaterialTheme {
        CompletionSection(
            installationSuccess = true,
            resultMessage = "Installation completed successfully!",
            onNavigateBack = { })
    }
}

@Preview(showBackground = true)
@Composable
fun CompletionSectionErrorPreview() {
    MaterialTheme {
        CompletionSection(
            installationSuccess = false,
            resultMessage = "Installation failed: Permission denied",
            onNavigateBack = { })
    }
}

/**
 * Starts the APK installation process with progress tracking and logging.
 *
 * @param context Android context for installation operations
 * @param packageUri URI of the source package (file or folder)
 * @param isFile Whether the URI points to a file (true) or folder (false)
 * @param selectedApkNames List of selected APK file names to install
 * @param onInstallationStart Callback invoked when installation begins
 * @param onLogUpdate Callback invoked when a new log entry is added
 * @param onProgress Callback invoked when installation progress updates
 * @param onComplete Callback invoked when installation completes successfully
 * @param onError Callback invoked when installation encounters an error
 */
private suspend fun startInstallation(
    context: android.content.Context,
    packageUri: Uri,
    isFile: Boolean,
    selectedApkNames: List<String>,
    onInstallationStart: () -> Unit,
    onLogUpdate: (LogEntry) -> Unit,
    onProgress: (Int, Int) -> Unit,
    onComplete: (Boolean, String?) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        onInstallationStart()

        val installer = PackageInstallerManager(context)

        installer.installApks(
            packageUri = packageUri,
            isFile = isFile,
            selectedApkNames = selectedApkNames,
            callback = object : PackageInstallerManager.InstallationCallback {
                override fun onLogUpdate(log: LogEntry) {
                    onLogUpdate(log)
                }

                override fun onProgress(progress: Int, total: Int) {
                    onProgress(progress, total)
                }

                override fun onInstallationComplete(
                    success: Boolean, resultCode: Int?, message: String?
                ) {
                    onComplete(success, message)
                }
            })
    } catch (e: Exception) {
        onError(e)
    }
}

/**
 * Creates a final log entry based on installation result.
 *
 * @param context Android context for accessing string resources
 * @param success Whether installation was successful
 * @param message Result message from installation
 * @return LogEntry representing the final installation result
 */
private fun createFinalLog(
    context: android.content.Context, success: Boolean, message: String?
): LogEntry {
    return LogEntry(
        level = if (success) LogLevel.SUCCESS else LogLevel.ERROR, message = if (success) {
            context.getString(R.string.installation_completed_success_message)
        } else {
            context.getString(
                R.string.installation_failed_error_message,
                message ?: context.getString(R.string.unknown_error)
            )
        }
    )
}
