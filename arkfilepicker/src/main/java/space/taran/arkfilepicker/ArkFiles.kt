package space.taran.arkfilepicker

import space.taran.arkfilepicker.ArkFiles.ARK_FOLDER
import space.taran.arkfilepicker.ArkFiles.FAVORITES_FILE
import space.taran.arkfilepicker.ArkFiles.GLOBAL_FOLDER
import space.taran.arkfilepicker.ArkFiles.PREVIEWS_FOLDER
import space.taran.arkfilepicker.ArkFiles.ROOTS_FILE
import space.taran.arkfilepicker.ArkFiles.TAGS_STORAGE_FILE
import space.taran.arkfilepicker.ArkFiles.THUMBNAILS_FOLDER
import java.nio.file.Path

internal object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val GLOBAL_FOLDER = ".ark-global"
    const val ROOTS_FILE = "roots"
    const val FAVORITES_FILE = "favorites"
    const val TAGS_STORAGE_FILE = "tags"
    const val PREVIEWS_FOLDER = "previews"
    const val THUMBNAILS_FOLDER = "thumbnails"
}

internal fun Path.arkFolder() = resolve(ARK_FOLDER)
internal fun Path.arkRoots() = resolve(ROOTS_FILE)
internal fun Path.arkGlobal() = resolve(GLOBAL_FOLDER)
internal fun Path.arkFavorites() = resolve(FAVORITES_FILE)
internal fun Path.arkTagsStorage() = resolve(TAGS_STORAGE_FILE)
internal fun Path.arkPreviews() = resolve(PREVIEWS_FOLDER)
internal fun Path.arkThumbnails() = resolve(THUMBNAILS_FOLDER)