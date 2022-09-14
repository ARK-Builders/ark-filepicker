package space.taran.arkfilepicker

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.roots.FoldersRepo

object ArkFilePicker {
    internal lateinit var foldersRepo: FoldersRepo

    fun init(appCtx: Context) {
        foldersRepo = FoldersRepo(appCtx)
    }
}