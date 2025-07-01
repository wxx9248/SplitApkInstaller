package top.wxx9248.splitapkinstaller.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.wxx9248.splitapkinstaller.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class representing a log entry
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

/**
 * Enum representing different log levels
 */
enum class LogLevel {
    INFO, WARNING, ERROR, SUCCESS
}

/**
 * Interface for logging operations
 */
interface Logger {
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String)
    fun success(message: String)
    fun addLog(log: LogEntry)
    fun getLogs(): List<LogEntry>
    fun clearLogs()
}

/**
 * Simple in-memory logger implementation
 */
class MemoryLogger : Logger {
    private val logs = mutableListOf<LogEntry>()

    override fun info(message: String) {
        addLog(LogEntry(level = LogLevel.INFO, message = message))
    }

    override fun warning(message: String) {
        addLog(LogEntry(level = LogLevel.WARNING, message = message))
    }

    override fun error(message: String) {
        addLog(LogEntry(level = LogLevel.ERROR, message = message))
    }

    override fun success(message: String) {
        addLog(LogEntry(level = LogLevel.SUCCESS, message = message))
    }

    override fun addLog(log: LogEntry) {
        logs.add(log)
    }

    override fun getLogs(): List<LogEntry> {
        return logs.toList()
    }

    override fun clearLogs() {
        logs.clear()
    }
}

/**
 * LogView component for displaying logs in a scrollable text field
 *
 * @param logs List of log entries to display
 * @param modifier Modifier for the component
 * @param title Optional title for the log section
 * @param emptyMessage Message to show when no logs are available
 */
@Composable
fun LogView(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.installation_logs),
    emptyMessage: String = stringResource(R.string.no_logs_available)
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val logText = remember(logs) {
                if (logs.isEmpty()) {
                    emptyMessage
                } else {
                    formatLogsAsText(logs)
                }
            }

            TextField(
                value = logText,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

/**
 * Formats a list of log entries into a single text string
 */
private fun formatLogsAsText(logs: List<LogEntry>): String {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return logs.joinToString("\n") { log ->
        val timestamp = timeFormatter.format(Date(log.timestamp))
        "[$timestamp] ${log.level.name}: ${log.message}"
    }
}

// Preview composables for testing
@Preview(showBackground = true)
@Composable
fun LogViewEmptyPreview() {
    MaterialTheme {
        LogView(
            logs = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogViewWithLogsPreview() {
    MaterialTheme {
        val sampleLogs = listOf(
            LogEntry(level = LogLevel.INFO, message = "Starting installation process..."),
            LogEntry(level = LogLevel.INFO, message = "Extracting APK files..."),
            LogEntry(level = LogLevel.WARNING, message = "Large APK file detected"),
            LogEntry(level = LogLevel.SUCCESS, message = "Installation completed successfully"),
            LogEntry(
                level = LogLevel.ERROR,
                message = "Failed to install package: Permission denied"
            )
        )

        LogView(
            logs = sampleLogs,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogViewCustomTitlePreview() {
    MaterialTheme {
        val sampleLogs = listOf(
            LogEntry(level = LogLevel.INFO, message = "Custom log entry 1"),
            LogEntry(level = LogLevel.SUCCESS, message = "Custom log entry 2")
        )

        LogView(
            logs = sampleLogs,
            title = "Custom Log Title",
            emptyMessage = "No custom logs found",
            modifier = Modifier.padding(16.dp)
        )
    }
}