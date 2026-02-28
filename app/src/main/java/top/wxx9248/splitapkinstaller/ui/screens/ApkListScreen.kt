package top.wxx9248.splitapkinstaller.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.core.ApkCacheManager
import top.wxx9248.splitapkinstaller.model.ApkInfo
import top.wxx9248.splitapkinstaller.model.SplitConfigType
import top.wxx9248.splitapkinstaller.ui.components.ErrorDialog
import top.wxx9248.splitapkinstaller.util.ApkSourceReader
import top.wxx9248.splitapkinstaller.util.ApkUtil
import top.wxx9248.splitapkinstaller.util.DeviceConfig
import top.wxx9248.splitapkinstaller.util.DeviceConfigUtil
import top.wxx9248.splitapkinstaller.util.SplitApkSelector

@Serializable
data class ApkListRoute(
    val packageUriString: String, val isFile: Boolean
)

/**
 * Main APK list screen that displays extracted APK files and allows selection for installation.
 *
 * @param packageUri URI of the selected package (file or folder)
 * @param isFile Whether the URI points to a file (true) or folder (false)
 * @param onNavigateBack Callback to navigate back to the previous screen
 * @param onNavigateToInstallation Callback to navigate to installation screen with selected APKs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkListScreen(
    packageUri: Uri,
    isFile: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToInstallation: (InstallationRoute) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apks by remember { mutableStateOf<List<ApkInfo>>(emptyList()) }
    var selectedApks by remember { mutableStateOf<Set<ApkInfo>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorException by remember { mutableStateOf<Throwable?>(null) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(packageUri) {
        scope.launch {
            // Check if we have cached data first
            val cachedData = ApkCacheManager.getCachedApkData(packageUri, isFile)

            if (cachedData != null) {
                // Use cached data
                apks = cachedData.apks
                selectedApks = cachedData.selectedApks

                isLoading = false
                kotlinx.coroutines.delay(100)
                showContent = true
            } else {
                // Parse APKs and cache the result
                val result = ApkSourceReader.readApksMetaFromPackage(
                    context, packageUri, isFile
                )

                val deviceConfig = DeviceConfigUtil.detectDeviceConfig(context)
                handleExtractionResult(result, context, isFile, onSuccess = { extractedApks ->
                    apks = extractedApks
                    selectedApks = selectInitialApks(extractedApks, deviceConfig)

                    // Cache the parsed data
                    ApkCacheManager.cacheApkData(packageUri, isFile, extractedApks, selectedApks)
                }, onError = { error, message ->
                    errorException = error
                    errorMessage = message
                })

                isLoading = false
                kotlinx.coroutines.delay(100)
                showContent = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ApkListTopBar(onNavigateBack)

        when {
            isLoading -> LoadingContent(isFile = isFile)
            errorMessage != null -> ErrorContent(
                errorMessage = errorMessage!!, onNavigateBack
            )

            else -> ApkListContent(
                showContent, apks, selectedApks, onSelectionChanged = { apk, isSelected ->
                    selectedApks = updateApkSelection(selectedApks, apk, isSelected)
                    // Update cache with new selection
                    ApkCacheManager.updateSelectedApks(packageUri, isFile, selectedApks)
                }, packageUri, isFile, onNavigateToInstallation
            )
        }
    }

    errorException?.let { exception ->
        ErrorDialog(
            throwable = exception, onDismiss = {
                errorException = null
                errorMessage = null
            })
    }
}

/**
 * Top app bar for the APK list screen with back navigation.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApkListTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(title = {
        Text(
            text = stringResource(R.string.select_apks_to_install),
            style = MaterialTheme.typography.titleLarge
        )
    }, navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }
    })
}

/**
 * Loading content displayed while APK extraction is in progress.
 *
 * @param isFile Whether the source is a file (true) or folder (false)
 */
@Composable
private fun LoadingContent(
    isFile: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = if (isFile) {
                    stringResource(R.string.reading_apk_package)
                } else {
                    stringResource(R.string.scanning_folder)
                }, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Error content displayed when APK extraction fails.
 *
 * @param errorMessage The error message to display
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@Composable
private fun ErrorContent(
    errorMessage: String, onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(onClick = onNavigateBack) {
                    Text(stringResource(R.string.go_back))
                }
            }
        }
    }
}

/**
 * Main content displaying the list of APK files with selection capabilities.
 *
 * @param showContent Whether to show the content with animation
 * @param apks List of extracted APK information
 * @param selectedApks Set of currently selected APKs
 * @param onSelectionChanged Callback when APK selection changes
 * @param packageUri URI of the source package
 * @param isFile Whether the source is a file (true) or folder (false)
 * @param onNavigateToInstallation Callback to navigate to installation screen
 */
@Composable
private fun ApkListContent(
    showContent: Boolean,
    apks: List<ApkInfo>,
    selectedApks: Set<ApkInfo>,
    onSelectionChanged: (ApkInfo, Boolean) -> Unit,
    packageUri: Uri,
    isFile: Boolean,
    onNavigateToInstallation: (InstallationRoute) -> Unit
) {
    AnimatedVisibility(
        visible = showContent, enter = fadeIn(), exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!ApkUtil.hasBaseApk(apks)) {
                NoBaseApkWarning()
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apks) { apk ->
                    ApkListItem(
                        apk = apk,
                        isSelected = selectedApks.contains(apk),
                        onSelectionChanged = { isSelected ->
                            onSelectionChanged(apk, isSelected)
                        })
                }
            }

            InstallButton(
                selectedApks, packageUri, isFile, onNavigateToInstallation
            )
        }
    }
}

/**
 * Warning component displayed when no base APK is selected.
 */
@Composable
private fun NoBaseApkWarning() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = stringResource(R.string.warning_no_base_apk),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Install button that validates selection and navigates to installation screen.
 *
 * @param selectedApks Set of currently selected APKs
 * @param packageUri URI of the source package
 * @param isFile Whether the source is a file (true) or folder (false)
 * @param onNavigateToInstallation Callback to navigate to installation screen
 */
@Composable
private fun InstallButton(
    selectedApks: Set<ApkInfo>,
    packageUri: Uri,
    isFile: Boolean,
    onNavigateToInstallation: (InstallationRoute) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedApkNames = selectedApks.map { it -> it.name }

    Surface(
        modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val isValidSelection = ApkUtil.validateApkSelection(selectedApks)
            val validationError = getValidationError(context, selectedApks)

            validationError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        onNavigateToInstallation(
                            InstallationRoute(
                                packageUri.toString(), isFile, Json.encodeToString(selectedApkNames)
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValidSelection && selectedApks.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.install_selected_apks, selectedApks.size
                    ), style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Individual APK list item with selection checkbox and APK information.
 *
 * @param apk The APK information to display
 * @param isSelected Whether this APK is currently selected
 * @param onSelectionChanged Callback when selection state changes
 */
@Composable
private fun ApkListItem(
    apk: ApkInfo, isSelected: Boolean, onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelectionChanged(!isSelected)
            }, shape = RoundedCornerShape(12.dp), colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = apk.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (apk.isBase) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (apk.isBase) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryFixed,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.base_apk_tag),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryFixed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    splitConfigLabel(apk.splitConfigType)?.let { label ->
                        val (bgColor, fgColor) = splitConfigColors(apk.splitConfigType)
                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = fgColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Only show file size if it's known (> 0)
                if (apk.size > 0) {
                    Text(
                        text = ApkUtil.formatFileSize(LocalContext.current, apk.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (apk.isBase) {
                    Text(
                        text = stringResource(R.string.required_for_installation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ApkListScreenPreview() {
    MaterialTheme {
        ApkListScreen(
            packageUri = "content://example.apk".toUri(),
            isFile = true,
            onNavigateBack = { },
            onNavigateToInstallation = { })
    }
}

@Preview(showBackground = true)
@Composable
fun ApkListTopBarPreview() {
    MaterialTheme {
        ApkListTopBar(
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ApkListContentPreview() {
    MaterialTheme {
        ApkListContent(
            showContent = true,
            apks = listOf(
                ApkInfo(name = "base.apk", size = 1024000L, isBase = true),
                ApkInfo(name = "config.apk", size = 512000L, isBase = false),
                ApkInfo(name = "split_config.apk", size = 256000L, isBase = false)
            ),
            selectedApks = setOf(
                ApkInfo(name = "base.apk", size = 1024000L, isBase = true),
                ApkInfo(name = "config.apk", size = 512000L, isBase = false)
            ),
            onSelectionChanged = { _, _ -> },
            packageUri = "content://example.apk".toUri(),
            isFile = true,
            onNavigateToInstallation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InstallButtonPreview() {
    MaterialTheme {
        InstallButton(
            selectedApks = setOf(
                ApkInfo(name = "base.apk", size = 1024000L, isBase = true),
                ApkInfo(name = "config.apk", size = 512000L, isBase = false)
            ),
            packageUri = "content://example.apk".toUri(),
            isFile = true,
            onNavigateToInstallation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ApkListItemPreview() {
    MaterialTheme {
        ApkListItem(
            apk = ApkInfo(
                name = "base.apk", size = 1024000L, isBase = true
            ), isSelected = true, onSelectionChanged = { })
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingContentPreview() {
    MaterialTheme {
        LoadingContent(isFile = true)
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorContentPreview() {
    MaterialTheme {
        ErrorContent(
            errorMessage = "Failed to extract APK files", onNavigateBack = { })
    }
}

@Preview(showBackground = true)
@Composable
fun NoBaseApkWarningPreview() {
    MaterialTheme {
        NoBaseApkWarning()
    }
}


/**
 * Handles the result of APK extraction operation.
 *
 * @param result The extraction result containing APKs or error
 * @param context Android context for accessing resources
 * @param isFile Whether the source is a file (true) or folder (false)
 * @param onSuccess Callback called with extracted APKs on success
 * @param onError Callback called with error information on failure
 */
private fun handleExtractionResult(
    result: ApkSourceReader.Result<List<ApkInfo>>,
    context: android.content.Context,
    isFile: Boolean,
    onSuccess: (List<ApkInfo>) -> Unit,
    onError: (Throwable?, String) -> Unit
) {
    if (result.error != null) {
        val errorMessage = context.getString(
            if (isFile) R.string.error_reading_package else R.string.error_scanning_folder,
            result.error.message ?: context.getString(R.string.unknown_error)
        )
        onError(result.error, errorMessage)
        return
    }

    if (result.entries.isNullOrEmpty()) {
        onError(
            null,
            context.getString(if (isFile) R.string.no_apk_files_found_in_package else R.string.no_apk_files_found_in_folder)
        )
        return
    }

    onSuccess(result.entries)
}

/**
 * Selects initial APKs based on device configuration.
 * When a base APK is present, auto-selects only matching config splits.
 * Falls back to empty selection when no base APK is found.
 *
 * @param apks List of available APKs
 * @param deviceConfig The device's configuration for matching splits
 * @return Set of initially selected APKs
 */
private fun selectInitialApks(apks: List<ApkInfo>, deviceConfig: DeviceConfig): Set<ApkInfo> {
    val baseApks = apks.filter { it.isBase }
    return if (baseApks.isNotEmpty()) {
        SplitApkSelector.selectApks(apks, deviceConfig)
    } else {
        emptySet()
    }
}

/**
 * Updates APK selection by adding or removing an APK from the current selection.
 *
 * @param currentSelection Current set of selected APKs
 * @param apk The APK to add or remove
 * @param isSelected Whether to add (true) or remove (false) the APK
 * @return Updated set of selected APKs
 */
private fun updateApkSelection(
    currentSelection: Set<ApkInfo>, apk: ApkInfo, isSelected: Boolean
): Set<ApkInfo> {
    return if (isSelected) {
        currentSelection + apk
    } else {
        if (apk.isBase) {
            currentSelection // Don't allow deselecting base.apk
        } else {
            currentSelection - apk
        }
    }
}

/**
 * Returns a display label for a split config type, or null for None.
 */
private fun splitConfigLabel(configType: SplitConfigType): String? {
    return when (configType) {
        is SplitConfigType.Abi -> configType.qualifier
        is SplitConfigType.Density -> configType.qualifier
        is SplitConfigType.Language -> configType.qualifier
        is SplitConfigType.None -> null
    }
}

/**
 * Returns a background/foreground color pair for a split config tag.
 */
@Composable
private fun splitConfigColors(configType: SplitConfigType): Pair<Color, Color> {
    return when (configType) {
        is SplitConfigType.Abi -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        is SplitConfigType.Density -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        is SplitConfigType.Language -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        is SplitConfigType.None -> Color.Transparent to Color.Transparent
    }
}

/**
 * Validates the current APK selection and returns an error message if invalid.
 *
 * @param context Android context for accessing string resources
 * @param selectedApks Set of currently selected APKs
 * @return Error message if validation fails, null if valid
 */
private fun getValidationError(
    context: android.content.Context, selectedApks: Set<ApkInfo>
): String? {
    if (selectedApks.isEmpty()) {
        return context.getString(R.string.please_select_at_least_one_apk)
    }

    if (!selectedApks.any { it.isBase }) {
        return context.getString(R.string.base_apk_must_be_selected)
    }

    return null
}
