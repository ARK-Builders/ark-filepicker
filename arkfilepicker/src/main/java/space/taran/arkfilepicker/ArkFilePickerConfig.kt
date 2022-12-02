package space.taran.arkfilepicker

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import java.nio.file.Path

class ArkFilePickerConfig(
    @StringRes
    val titleStringId: Int = R.string.ark_file_picker_pick_title,
    @StringRes
    val pickButtonStringId: Int = R.string.ark_file_picker_pick,
    @StringRes
    val cancelButtonStringId: Int = R.string.ark_file_picker_cancel,
    @StringRes
    val internalStorageStringId: Int = R.string.ark_file_picker_internal_storage,
    @StringRes
    val accessDeniedStringId: Int = R.string.ark_file_picker_access_denied,
    @PluralsRes
    val itemsPluralId: Int = R.plurals.ark_file_picker_items,
    @StyleRes
    val themeId: Int = com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog,

    val showRoots: Boolean = false,
    val rootsFirstPage: Boolean = false,
    val initialPath: Path? = null,
    val mode: ArkFilePickerMode = ArkFilePickerMode.FOLDER,
    val pathPickedRequestKey: String? = null,
)