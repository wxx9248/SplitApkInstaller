package top.wxx9248.splitapkinstaller.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.wxx9248.splitapkinstaller.R

@Composable
fun ErrorDialog(
    throwable: Throwable,
    onDismiss: () -> Unit
) {
    var copySuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(copySuccessMessage) {
        if (copySuccessMessage) {
            kotlinx.coroutines.delay(2000)
            copySuccessMessage = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        ErrorDialogContent(
            throwable = throwable,
            copySuccessMessage = copySuccessMessage,
            onCopySuccess = { copySuccessMessage = true },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ErrorDialogContent(
    throwable: Throwable,
    copySuccessMessage: Boolean,
    onCopySuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ErrorDialogHeader(onDismiss = onDismiss)

            Spacer(modifier = Modifier.height(12.dp))

            ErrorMessage(throwable = throwable)

            StackTraceSection(
                throwable = throwable,
                copySuccessMessage = copySuccessMessage,
                onCopySuccess = onCopySuccess
            )

            Spacer(modifier = Modifier.height(16.dp))

            CloseButton(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ErrorDialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.error_dialog_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ErrorMessage(throwable: Throwable) {
    Text(
        text = throwable.message ?: stringResource(R.string.unknown_error),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun StackTraceSection(
    throwable: Throwable,
    copySuccessMessage: Boolean,
    onCopySuccess: () -> Unit
) {
    val context = LocalContext.current

    StackTraceHeader(
        copySuccessMessage = copySuccessMessage,
        onCopyClick = {
            copyStackTraceToClipboard(context, throwable) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
                onCopySuccess()
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    StackTraceContent(throwable = throwable)
}

@Composable
private fun StackTraceHeader(
    copySuccessMessage: Boolean,
    onCopyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stack_trace),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyButton(onClick = onCopyClick)
        }
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = stringResource(R.string.copy),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StackTraceContent(throwable: Throwable) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        item {
            Text(
                text = formatStackTrace(throwable),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Text(stringResource(R.string.close))
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogPreview() {
    val sampleException = RuntimeException("Sample error message for preview")
    ErrorDialog(
        throwable = sampleException,
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogWithLongMessagePreview() {
    val longMessageException = IllegalStateException(
        "This is a very long error message that should demonstrate how the dialog handles lengthy error descriptions and wraps text appropriately within the available space."
    )
    ErrorDialog(
        throwable = longMessageException,
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogWithCausePreview() {
    val causeException = IllegalArgumentException("Invalid argument provided")
    val mainException = RuntimeException("Operation failed", causeException)
    ErrorDialog(
        throwable = mainException,
        onDismiss = {}
    )
}

private fun copyStackTraceToClipboard(
    context: Context,
    throwable: Throwable,
    onSuccess: () -> Unit
) {
    val stackTrace = formatFullStackTrace(throwable)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        context.getString(R.string.error_details),
        stackTrace
    )
    clipboard.setPrimaryClip(clip)
    onSuccess()
}

private fun formatStackTrace(throwable: Throwable): String {
    return buildString {
        appendLine("${throwable::class.java.simpleName}: ${throwable.message}")
        throwable.stackTrace.forEach { element ->
            appendLine("  at $element")
        }

        var cause = throwable.cause
        while (cause != null) {
            appendLine()
            appendLine("Caused by: ${cause::class.java.simpleName}: ${cause.message}")
            cause.stackTrace.forEach { element ->
                appendLine("  at $element")
            }
            cause = cause.cause
        }
    }
}

private fun formatFullStackTrace(throwable: Throwable): String {
    return buildString {
        appendLine("Error: ${throwable.message}")
        appendLine("Type: ${throwable::class.java.simpleName}")
        appendLine()
        appendLine("Stack Trace:")
        throwable.stackTrace.forEach { element ->
            appendLine("  at $element")
        }

        var cause = throwable.cause
        while (cause != null) {
            appendLine()
            appendLine("Caused by: ${cause::class.java.simpleName}: ${cause.message}")
            cause.stackTrace.forEach { element ->
                appendLine("  at $element")
            }
            cause = cause.cause
        }
    }
}
