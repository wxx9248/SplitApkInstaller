package top.wxx9248.splitapkinstaller.util

import android.content.Context
import android.os.Build

data class DeviceConfig(
    val primaryAbi: String,
    val densityQualifier: String,
    val language: String,
    val region: String?
)

object DeviceConfigUtil {

    fun detectDeviceConfig(context: Context): DeviceConfig {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()?.replace('-', '_') ?: "arm64_v8a"
        val densityQualifier = mapDpiToQualifier(context.resources.configuration.densityDpi)

        val locale = context.resources.configuration.locales[0]
        val language = locale.language
        val region = locale.country.ifEmpty { null }

        return DeviceConfig(
            primaryAbi = primaryAbi,
            densityQualifier = densityQualifier,
            language = language,
            region = region
        )
    }

    internal fun mapDpiToQualifier(dpi: Int): String {
        // Midpoints between adjacent density buckets
        return when {
            dpi <= 140  -> "ldpi"     // (120+160)/2 = 140
            dpi <= 186  -> "mdpi"     // (160+213)/2 ≈ 186
            dpi <= 226  -> "tvdpi"    // (213+240)/2 ≈ 226
            dpi <= 280  -> "hdpi"     // (240+320)/2 = 280
            dpi <= 400  -> "xhdpi"    // (320+480)/2 = 400
            dpi <= 560  -> "xxhdpi"   // (480+640)/2 = 560
            else        -> "xxxhdpi"
        }
    }
}
