package space.taran.arkfilepicker.folders

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.FileUtils
import space.taran.arkfilepicker.LogTags
import space.taran.arkfilepicker.arkFolder
import java.nio.file.Path
import java.util.LinkedList
import java.util.Queue
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

class ArkRootsScan {

    private val roots = mutableListOf<Path>()
    private val queue: Queue<Path> = LinkedList()

    fun getRoots() = roots

    suspend fun scan(devices: List<Path>) = withContext(Dispatchers.IO) {
        queue.addAll(devices)

        while (queue.isNotEmpty()) {
            ensureActive()
            scanFolder(queue.poll()!!)
        }
    }

    private fun scanFolder(folder: Path) = try {
        if (folder.arkFolder().exists()) {
            roots.add(folder)
        } else
            queue.addAll(folder.listDirectoryEntries().filter(Path::isDirectory))
    } catch (e: Exception) {
        Log.w(LogTags.FILES, "Can't scan $folder due to $e")
    }
}