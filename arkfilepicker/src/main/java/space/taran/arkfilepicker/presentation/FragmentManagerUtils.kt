package space.taran.arkfilepicker.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import java.nio.file.Path
import kotlin.io.path.Path

fun FragmentManager.onArkPathPicked(
    lifecycleOwner: LifecycleOwner,
    customRequestKey: String? = null,
    listener: (Path) -> Unit,
) {
    setFragmentResultListener(
        customRequestKey ?: ArkFilePickerFragment.PATH_PICKED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkFilePickerFragment.PATH_PICKED_PATH_BUNDLE_KEY)!!
            )
        )
    }
}

fun FragmentManager.onArkFolderChange(
    lifecycleOwner: LifecycleOwner,
    listener: (Path) -> Unit
) {
    setFragmentResultListener(
        ArkFilePickerFragment.FOLDER_CHANGED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkFilePickerFragment.FOLDER_CHANGED_FOLDER_BUNDLE_KEY)!!
            )
        )
    }
}