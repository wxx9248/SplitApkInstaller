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

/**
 * Error dialog component that displays exception information with stack trace and copy functionality.
 *
 * @param throwable The exception/throwable to display
 * @param onDismiss Callback to dismiss the dialog
 */
/**
 * Error dialog component that displays exception information with stack trace and copy functionality.
 *
 * @param throwable The exception/throwable to display
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
@Composable
fun ErrorDialog(
    throwable: Throwable,
    onDismiss: () -> Unit
) {
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
            onDismiss = onDismiss
        )
    }
}

/**
 * Main content of the error dialog containing header, message, stack trace, and close button.
 *
 * @param throwable The exception/throwable to display
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
@Composable
private fun ErrorDialogContent(
    throwable: Throwable,
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

            StackTraceSection(throwable = throwable)

            Spacer(modifier = Modifier.height(16.dp))

            CloseButton(onDismiss = onDismiss)
        }
    }
}

/**
 * Header section of the error dialog with title and close button.
 *
 * @param onDismiss Callback invoked when the close button is pressed
 */
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

/**
 * Error message section displaying the exception message.
 *
 * @param throwable The exception/throwable whose message to display
 */
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

/**
 * Stack trace section with header and scrollable content.
 *
 * @param throwable The exception/throwable whose stack trace to display
 */
@Composable
private fun StackTraceSection(
    throwable: Throwable
) {
    val context = LocalContext.current
    val copiedToClipboardText = stringResource(R.string.copied_to_clipboard)

    StackTraceHeader(
        onCopyClick = {
            copyStackTraceToClipboard(context, throwable) {
                Toast.makeText(
                    context,
                    copiedToClipboardText,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    StackTraceContent(throwable = throwable)
}

/**
 * Header for the stack trace section with copy button.
 *
 * @param onCopyClick Callback invoked when copy button is clicked
 */
@Composable
private fun StackTraceHeader(
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

/**
 * Copy button for copying stack trace to clipboard.
 *
 * @param onClick Callback invoked when button is clicked
 */
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

/**
 * Scrollable content displaying the formatted stack trace.
 *
 * @param throwable The exception/throwable whose stack trace to display
 */
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

/**
 * Close button for dismissing the error dialog.
 *
 * @param onDismiss Callback invoked when button is clicked
 */
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

/**
 * Copies the stack trace of a throwable to the system clipboard.
 *
 * @param context Android context for accessing clipboard service
 * @param throwable The exception/throwable whose stack trace to copy
 * @param onSuccess Callback invoked when copy operation succeeds
 */
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

/**
 * Formats a throwable's stack trace for display in the dialog.
 *
 * @param throwable The exception/throwable to format
 * @return Formatted stack trace string for display
 */
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

/**
 * Formats a throwable's complete stack trace for clipboard copying.
 *
 * @param throwable The exception/throwable to format
 * @return Complete formatted stack trace string for copying
 */
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
