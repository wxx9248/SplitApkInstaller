package top.wxx9248.splitapkinstaller.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import top.wxx9248.splitapkinstaller.R

@Serializable
object HomeRoute

@Composable
fun HomeScreen(
    onNavigateToApkList: (ApkListRoute) -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleFilePickerResult(result, context, onNavigateToApkList)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleFolderPickerResult(result, context, onNavigateToApkList)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TitleSection()
        Spacer(modifier = Modifier.height(72.dp))
        ActionButtonsSection(
            filePickerLauncher = filePickerLauncher, folderPickerLauncher = folderPickerLauncher
        )
        Spacer(modifier = Modifier.height(72.dp))
        InfoSection()
    }
}

@Composable
private fun TitleSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.split_apk_installer_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtonsSection(
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    folderPickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.choose_installation_source),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            FilePickerButton(filePickerLauncher)
            FolderPickerButton(folderPickerLauncher)
        }
    }
}

@Composable
private fun FilePickerButton(
    filePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    Button(
        onClick = {
            val intent = createFilePickerIntent()
            filePickerLauncher.launch(intent)
        }, modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.install_from_file),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun FolderPickerButton(
    folderPickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    Button(
        onClick = {
            val intent = createFolderPickerIntent()
            folderPickerLauncher.launch(intent)
        }, modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.install_from_folder),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.supported_formats),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = stringResource(R.string.supported_formats_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onNavigateToApkList = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleSectionPreview() {
    MaterialTheme {
        TitleSection()
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionButtonsSectionPreview() {
    val mockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    MaterialTheme {
        ActionButtonsSection(
            filePickerLauncher = mockLauncher,
            folderPickerLauncher = mockLauncher
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FilePickerButtonPreview() {
    val mockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    MaterialTheme {
        FilePickerButton(
            filePickerLauncher = mockLauncher
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderPickerButtonPreview() {
    val mockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    MaterialTheme {
        FolderPickerButton(
            folderPickerLauncher = mockLauncher
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoSectionPreview() {
    MaterialTheme {
        InfoSection()
    }
}


private fun createFilePickerIntent(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        // Allow all mime type since some file extensions are not recognized as a zip mime
        type = "*/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun createFolderPickerIntent(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun handleFilePickerResult(
    result: androidx.activity.result.ActivityResult,
    context: android.content.Context,
    onNavigateToApkList: (ApkListRoute) -> Unit
) {
    if (result.resultCode != android.app.Activity.RESULT_OK) return

    val uri = result.data?.data ?: return
    onNavigateToApkList(ApkListRoute(uri.toString(), true))
}

private fun handleFolderPickerResult(
    result: androidx.activity.result.ActivityResult,
    context: android.content.Context,
    onNavigateToApkList: (ApkListRoute) -> Unit
) {
    if (result.resultCode != android.app.Activity.RESULT_OK) return

    val uri = result.data?.data ?: return
    onNavigateToApkList(ApkListRoute(uri.toString(), false))
}
