package top.wxx9248.splitapkinstaller.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApkInfo(
    val name: String, val size: Long, val isBase: Boolean = false
) : Parcelable
