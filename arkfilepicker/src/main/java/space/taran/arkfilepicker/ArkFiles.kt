package space.taran.arkfilepicker

import space.taran.arkfilepicker.ArkFiles.ARK_FOLDER
import space.taran.arkfilepicker.ArkFiles.FAVORITES_FILE
import space.taran.arkfilepicker.ArkFiles.PREVIEWS_FOLDER
import space.taran.arkfilepicker.ArkFiles.ROOTS_FILE
import space.taran.arkfilepicker.ArkFiles.TAGS_STORAGE_FILE
import space.taran.arkfilepicker.ArkFiles.THUMBNAILS_FOLDER
import java.nio.file.Path

object ArkFiles {
    const val ARK_FOLDER = ".ark"
    const val ROOTS_FILE = "roots"
    const val FAVORITES_FILE = "favorites"
    const val TAGS_STORAGE_FILE = "tags"
    const val PREVIEWS_FOLDER = "previews"
    const val THUMBNAILS_FOLDER = "thumbnails"
}

fun Path.arkFolder() = resolve(ARK_FOLDER)
fun Path.arkRoots() = resolve(ROOTS_FILE)
fun Path.arkFavorites() = resolve(FAVORITES_FILE)
fun Path.arkTagsStorage() = resolve(TAGS_STORAGE_FILE)
fun Path.arkPreviews() = resolve(PREVIEWS_FOLDER)
fun Path.arkThumbnails() = resolve(THUMBNAILS_FOLDER)