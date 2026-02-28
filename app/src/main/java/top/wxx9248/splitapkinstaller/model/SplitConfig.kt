package top.wxx9248.splitapkinstaller.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface SplitConfigType : Parcelable {
    @Parcelize
    data class Abi(val qualifier: String) : SplitConfigType

    @Parcelize
    data class Density(val qualifier: String) : SplitConfigType

    @Parcelize
    data class Language(val qualifier: String) : SplitConfigType

    @Parcelize
    data object None : SplitConfigType
}
