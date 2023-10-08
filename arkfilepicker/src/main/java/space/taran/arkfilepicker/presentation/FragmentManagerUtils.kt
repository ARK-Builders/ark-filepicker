package dev.arkbuilders.arkfilepicker.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
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