package top.wxx9248.splitapkinstaller.util

import top.wxx9248.splitapkinstaller.model.ApkInfo
import top.wxx9248.splitapkinstaller.model.SplitConfigType

object SplitApkSelector {

    /**
     * Selects APKs matching the device configuration.
     *
     * Rules:
     * - Base APKs and unclassified splits (None) are always selected.
     * - ABI: select only the split matching the device's primary ABI.
     * - Density: always select nodpi/anydpi; among the rest, select the matching density.
     * - Language: try language+region → language-only → "en" fallback → select all.
     * - If no classified configs exist at all, fall back to selecting everything.
     */
    fun selectApks(apks: List<ApkInfo>, deviceConfig: DeviceConfig): Set<ApkInfo> {
        val hasClassifiedConfig = apks.any { it.splitConfigType !is SplitConfigType.None }
        if (!hasClassifiedConfig) return apks.toSet()

        return apks.filterTo(mutableSetOf()) { apk ->
            when (val config = apk.splitConfigType) {
                is SplitConfigType.None -> true
                is SplitConfigType.Abi -> config.qualifier.equals(deviceConfig.primaryAbi, ignoreCase = true)
                is SplitConfigType.Density -> selectDensity(config.qualifier, deviceConfig.densityQualifier)
                is SplitConfigType.Language -> selectLanguage(config.qualifier, deviceConfig, apks)
            }
        }
    }

    private fun selectDensity(qualifier: String, deviceDensity: String): Boolean {
        val lower = qualifier.lowercase()
        if (lower == "nodpi" || lower == "anydpi") return true
        return lower == deviceDensity.lowercase()
    }

    private fun selectLanguage(
        qualifier: String,
        deviceConfig: DeviceConfig,
        allApks: List<ApkInfo>
    ): Boolean {
        val langApks = allApks.filter { it.splitConfigType is SplitConfigType.Language }
        val qualifiers = langApks.map { (it.splitConfigType as SplitConfigType.Language).qualifier }

        // Build the language+region code for the device (e.g. "en_US")
        val deviceLangRegion = if (deviceConfig.region != null) {
            "${deviceConfig.language}_${deviceConfig.region}"
        } else null

        // Try exact language+region match
        if (deviceLangRegion != null && qualifiers.any { it.equals(deviceLangRegion, ignoreCase = true) }) {
            return qualifier.equals(deviceLangRegion, ignoreCase = true)
        }

        // Try language-only match
        if (qualifiers.any { it.equals(deviceConfig.language, ignoreCase = true) }) {
            return qualifier.equals(deviceConfig.language, ignoreCase = true)
        }

        // Fallback to "en"
        if (qualifiers.any { it.equals("en", ignoreCase = true) }) {
            return qualifier.equals("en", ignoreCase = true)
        }

        // No match at all — select all language splits (conservative)
        return true
    }
}
