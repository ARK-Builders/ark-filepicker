package space.taran.arkfilepicker.roots

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.notExists

internal class FoldersRepo(
    private val appCtx: Context
) {
    var folders = mapOf<Path, List<Path>>()
        private set

    private val klaxon = Klaxon()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            query()
        }
        observeRootChanges()
    }

    private fun observeRootChanges() {
        appCtx.contentResolver.registerContentObserver(
            RootsProviderContract.ALL_ROOTS_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    scope.launch {
                        query()
                    }
                }
            })
    }

    private suspend fun query() = withContext(Dispatchers.IO) {
        folders = queryRoots().associateWith {
            readFavorites(it)
        }
    }

    private fun queryRoots(): List<Path> {
        val cursor = appCtx.contentResolver.query(
            RootsProviderContract.ALL_ROOTS_URI,
            null,
            null,
            null,
            null
        )
        val roots = mutableListOf<Path>()
        cursor?.let {
            while (cursor.moveToNext()) {
                roots.add(Path(cursor.getString(0)))
            }
        }
        return roots
    }

    private fun readFavorites(root: Path): List<Path> {
        val arkFolder = root.resolve(".ark")
        if (arkFolder.notExists()) error("Ark folder must exist")
        val favoritesFile = arkFolder.resolve("favorites")

        return try {
            val jsonFavorites = klaxon.parse<JsonFavorites>(favoritesFile.toFile())
            jsonFavorites!!.favorites.map { Path(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private data class JsonFavorites(val favorites: Set<String>)