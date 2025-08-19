package top.wxx9248.splitapkinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.wxx9248.splitapkinstaller.R

/**
 * Represents the current state of a progress operation
 */
enum class ProgressState {
    IDLE,
    IN_PROGRESS,
    COMPLETED_SUCCESS,
    COMPLETED_FAILURE
}

/**
 * Configuration for customizable status texts
 */
data class ProgressTexts(
    val title: String,
    val inProgressText: String,
    val completedSuccessText: String,
    val completedFailureText: String,
    val filesProcessedFormat: String
)

/**
 * A reusable progress card component that displays progress information
 * with customizable status texts and well-defined APIs.
 *
 * @param state Current progress state
 * @param progress Current progress value
 * @param totalProgress Total progress value
 * @param texts Customizable status texts
 * @param modifier Modifier for the card
 */
@Composable
fun ProgressCard(
    state: ProgressState,
    progress: Int,
    totalProgress: Int,
    texts: ProgressTexts,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressHeader(
                state = state,
                texts = texts
            )

            LinearProgressIndicator(
                progress = {
                    if (totalProgress > 0) {
                        progress.toFloat() / totalProgress.toFloat()
                    } else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            ProgressDetails(
                progress = progress,
                totalProgress = totalProgress,
                filesProcessedFormat = texts.filesProcessedFormat
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    state: ProgressState,
    texts: ProgressTexts
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texts.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (state) {
            ProgressState.IN_PROGRESS -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = texts.inProgressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            ProgressState.COMPLETED_SUCCESS -> {
                Text(
                    text = texts.completedSuccessText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            ProgressState.COMPLETED_FAILURE -> {
                Text(
                    text = texts.completedFailureText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            ProgressState.IDLE -> {
                // No status indicator for idle state
            }
        }
    }
}

@Composable
private fun ProgressDetails(
    progress: Int,
    totalProgress: Int,
    filesProcessedFormat: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = String.format(filesProcessedFormat, progress, totalProgress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = context.getString(
                R.string.progress_percentage,
                if (totalProgress > 0) ((progress.toFloat() / totalProgress) * 100).toInt() else 0
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Preview functions for testing
@Preview(showBackground = true)
@Composable
fun ProgressCardIdlePreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.IDLE,
            progress = 0,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Installation completed successfully",
                completedFailureText = "Installation failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardInProgressPreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.IN_PROGRESS,
            progress = 3,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Installation completed successfully",
                completedFailureText = "Installation failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardCompletedSuccessPreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.COMPLETED_SUCCESS,
            progress = 10,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Installation completed successfully",
                completedFailureText = "Installation failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardCompletedFailurePreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.COMPLETED_FAILURE,
            progress = 5,
            totalProgress = 10,
            texts = ProgressTexts(
                title = "Installation Progress",
                inProgressText = "Installing...",
                completedSuccessText = "Installation completed successfully",
                completedFailureText = "Installation failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardCustomTextsPreview() {
    MaterialTheme {
        ProgressCard(
            state = ProgressState.IN_PROGRESS,
            progress = 7,
            totalProgress = 15,
            texts = ProgressTexts(
                title = "Download Progress",
                inProgressText = "Downloading...",
                completedSuccessText = "Download complete",
                completedFailureText = "Download failed",
                filesProcessedFormat = "Items downloaded"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
