package top.wxx9248.splitapkinstaller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.wxx9248.splitapkinstaller.R
import top.wxx9248.splitapkinstaller.model.ApkInfo
import top.wxx9248.splitapkinstaller.model.SplitConfigType
import top.wxx9248.splitapkinstaller.util.ApkUtil

/**
 * Individual APK list item with selection checkbox and APK information.
 *
 * @param apk The APK information to display
 * @param isSelected Whether this APK is currently selected
 * @param onSelectionChanged Callback when selection state changes
 */
@Composable
fun ApkListItem(
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

/**
 * Returns a display label for a split config type, or null for None.
 */
internal fun splitConfigLabel(configType: SplitConfigType): String? {
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
internal fun splitConfigColors(configType: SplitConfigType): Pair<Color, Color> {
    return when (configType) {
        is SplitConfigType.Abi -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        is SplitConfigType.Density -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        is SplitConfigType.Language -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        is SplitConfigType.None -> Color.Transparent to Color.Transparent
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
