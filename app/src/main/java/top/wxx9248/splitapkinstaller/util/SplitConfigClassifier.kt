package top.wxx9248.splitapkinstaller.util

import top.wxx9248.splitapkinstaller.model.ApkInfo
import top.wxx9248.splitapkinstaller.model.SplitConfigType

object SplitConfigClassifier {

    private val ABI_QUALIFIERS = setOf(
        "arm64_v8a", "armeabi_v7a", "armeabi", "x86", "x86_64", "mips", "mips64"
    )

    private val DENSITY_QUALIFIERS = setOf(
        "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi"
    )

    private val LANGUAGE_PATTERN = Regex("^[a-z]{2,3}(_[A-Z]{2})?$")

    private val SEPARATORS = charArrayOf('-', '_')

    // ========== Public API ==========

    /**
     * Enriches raw APK metadata with base detection and split config classification.
     * Applies affix-stripping once, then uses canonical names for both operations.
     */
    fun enrichApkMetadata(apks: List<ApkInfo>): List<ApkInfo> {
        val fileNames = apks.map { it.name }
        val baseApkNames = resolveBaseApks(fileNames)
        val canonicalNames = computeCanonicalNames(fileNames)
        return apks.mapIndexed { i, apk ->
            apk.copy(
                isBase = apk.name in baseApkNames,
                splitConfigType = if (apk.name in baseApkNames) SplitConfigType.None
                else classifySplitConfig(canonicalNames[i])
            )
        }.sortedWith(compareBy<ApkInfo> { !it.isBase }.thenBy { it.name })
    }

    /**
     * Classifies a canonical APK name into its split config type.
     * Priority: ABI -> Density -> Language -> None.
     */
    internal fun classifySplitConfig(canonicalName: String): SplitConfigType {
        val qualifier = extractConfigQualifier(canonicalName) ?: return SplitConfigType.None
        if (qualifier.isEmpty()) return SplitConfigType.None

        if (qualifier.lowercase() in ABI_QUALIFIERS) return SplitConfigType.Abi(qualifier)
        if (qualifier.lowercase() in DENSITY_QUALIFIERS) return SplitConfigType.Density(qualifier)
        if (LANGUAGE_PATTERN.matches(qualifier)) return SplitConfigType.Language(qualifier)

        return SplitConfigType.None
    }

    /**
     * Computes canonical names by stripping common prefix/suffix affixes
     * (e.g. tool-added suffixes like `-430-lspatched`) from APK filenames.
     *
     * Shared between base APK detection and config classification.
     */
    internal fun computeCanonicalNames(apkFileNames: List<String>): List<String> {
        if (apkFileNames.size <= 1) {
            return apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }
        }

        val stems = apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }

        val rawPrefix = longestCommonPrefix(stems)
        val prefixEnd = rawPrefix.lastIndexOfAny(SEPARATORS)
        val prefix = if (prefixEnd >= 0) rawPrefix.substring(0, prefixEnd + 1) else ""

        val afterPrefix = stems.map { it.removePrefix(prefix) }
        val rawSuffix = longestCommonSuffix(afterPrefix)
        val suffixStart = rawSuffix.indexOfAny(SEPARATORS)
        val suffix = if (suffixStart >= 0) rawSuffix.substring(suffixStart) else ""

        return stems.map { it.removePrefix(prefix).removeSuffix(suffix) }
    }

    // ========== Config Qualifier Extraction ==========

    /**
     * Extracts the config qualifier from a canonical APK name.
     *
     * Handles two patterns:
     * - Prefix: `split_config.`, `split_config_`, `config.` at the start
     *   e.g. `split_config.en` -> `en`, `split_config_arm64_v8a` -> `arm64_v8a`
     * - Infix: `.config.` embedded after a feature name
     *   e.g. `split_phonesky_webrtc_native_lib.config.x86` -> `x86`
     *
     * @return The qualifier string, or null if no config pattern is found.
     */
    private fun extractConfigQualifier(canonicalName: String): String? {
        // Check prefix patterns first
        val prefixes = listOf("split_config.", "split_config_", "config.")
        for (prefix in prefixes) {
            if (canonicalName.startsWith(prefix, ignoreCase = true)) {
                return canonicalName.substring(prefix.length)
            }
        }

        // Check infix `.config.` pattern (e.g. feature_name.config.qualifier)
        val infixMarker = ".config."
        val infixIndex = canonicalName.lastIndexOf(infixMarker, ignoreCase = true)
        if (infixIndex >= 0) {
            return canonicalName.substring(infixIndex + infixMarker.length)
        }

        return null
    }

    // ========== Base APK Resolution ==========

    /**
     * Determines which APK filenames correspond to the base APK by stripping
     * common prefix/suffix (at separator boundaries) to recover canonical names.
     *
     * For example, LSPatch-modified names like `base-430-lspatched.apk` and
     * `split_config_arm64_v8a-430-lspatched.apk` share the suffix `-430-lspatched`.
     * Stripping it yields `base.apk`, which is identified as the base APK.
     *
     * @return Set of original filenames that are base APKs (typically 0 or 1 element).
     */
    private fun resolveBaseApks(apkFileNames: List<String>): Set<String> {
        if (apkFileNames.size <= 1) {
            // Single APK: fall back to exact name check (no common affixes computable)
            return apkFileNames.filterTo(mutableSetOf()) {
                it.equals("base.apk", ignoreCase = true)
            }
        }

        val stems = apkFileNames.map { it.removeSuffix(".apk").removeSuffix(".APK") }

        // Compute common prefix, truncated to last separator boundary
        val rawPrefix = longestCommonPrefix(stems)
        val prefixEnd = rawPrefix.lastIndexOfAny(SEPARATORS)
        val prefix = if (prefixEnd >= 0) rawPrefix.substring(0, prefixEnd + 1) else ""

        // Strip prefix, then compute common suffix truncated to first separator boundary
        val afterPrefix = stems.map { it.removePrefix(prefix) }
        val rawSuffix = longestCommonSuffix(afterPrefix)
        val suffixStart = rawSuffix.indexOfAny(SEPARATORS)
        val suffix = if (suffixStart >= 0) rawSuffix.substring(suffixStart) else ""

        return apkFileNames.zip(stems).mapNotNullTo(mutableSetOf()) { (original, stem) ->
            val canonical = stem.removePrefix(prefix).removeSuffix(suffix)
            if (canonical.equals("base", ignoreCase = true)) original else null
        }
    }

    // ========== String Helpers ==========

    /**
     * Computes the longest common prefix of a list of strings, character-by-character.
     * O(N * min_len) time, O(1) extra space.
     */
    private fun longestCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        val minLen = strings.minOf { it.length }
        for (i in 0 until minLen) {
            val c = strings[0][i]
            if (strings.any { it[i] != c }) return strings[0].substring(0, i)
        }
        return strings[0].substring(0, minLen)
    }

    /**
     * Computes the longest common suffix of a list of strings, character-by-character.
     * O(N * min_len) time, O(1) extra space.
     */
    private fun longestCommonSuffix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        val minLen = strings.minOf { it.length }
        for (i in 1..minLen) {
            val c = strings[0][strings[0].length - i]
            if (strings.any { it[it.length - i] != c }) {
                return strings[0].substring(strings[0].length - i + 1)
            }
        }
        return strings[0].substring(strings[0].length - minLen)
    }
}