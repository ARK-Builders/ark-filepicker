package space.taran.arkfilepicker.roots

import android.net.Uri

object RootsProviderContract {
    const val AUTHORITY = "space.taran.arknavigator"
    const val ALL_ROOTS_PATH = "roots"
    val ALL_ROOTS_URI = Uri.parse("content://$AUTHORITY/$ALL_ROOTS_PATH")
}