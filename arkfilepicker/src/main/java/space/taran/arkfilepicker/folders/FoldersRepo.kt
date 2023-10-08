@file:Suppress("BlockingMethodInNonBlockingContext")

package dev.arkbuilders.arkfilepicker.folders

import android.content.Context
import android.util.Log
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.arkbuilders.arkfilepicker.FileUtils
import dev.arkbuilders.arkfilepicker.PartialResult
import dev.arkbuilders.arkfilepicker.arkFavorites
import dev.arkbuilders.arkfilepicker.arkFolder
import dev.arkbuilders.arkfilepicker.arkGlobal
import dev.arkbuilders.arkfilepicker.arkRoots
import java.nio.file.Path
import kotlin.io.path.*

typealias Folders = Map<Path, List<Path>>

class FoldersRepo(private val appCtx: Context) {
    private val provideMutex = Mutex()
    private val klaxon = Klaxon()
    private val fileUtils = FileUtils(appCtx)
    private lateinit var folders: Folders
    private val deviceRoot: Path
        get() = fileUtils.listDevices().first()

    suspend fun provideFolders(): Folders =
        provideWithMissing().succeeded

    suspend fun provideWithMissing(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
            provideMutex.withLock {
                if (::folders.isInitialized) {
                    return@withContext PartialResult(
                        succeeded = folders,
                        failed = listOf()
                    )
                }

                val foldersResult = query()
                if (foldersResult.failed.isNotEmpty())
                    Log.w(
                        LOG_TAG,
                        "Failed to verify the following paths: \n ${
                            foldersResult.failed.joinToString("\n")
                        }"
                    )

                folders = foldersResult.succeeded
                Log.d(LOG_TAG, "folders loaded: $folders")

                return@withContext foldersResult
            }
        }

    suspend fun resolveRoots(rootAndFav: RootAndFav): List<Path> {
        return if (!rootAndFav.isAllRoots())
            listOf(rootAndFav.root!!)
        else
            provideFolders().keys.toList()
    }

    fun findRootByPath(path: Path): Path? = folders.keys.find { root ->
        path.startsWith(root)
    }

    private suspend fun query(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
            val missingPaths = mutableListOf<Path>()

            val roots = readRoots().mapNotNull { root ->
                if (root.notExists()) {
                    missingPaths.add(root)
                    return@mapNotNull null
                }
                val arkFolder = root.arkFolder()
                if (root.notExists()) {
                    missingPaths.add(arkFolder)
                    return@mapNotNull null
                }
                root
            }

            val favoritesByRoot = roots.associateWith { root ->
                val favorites = readFavorites(root)
                val (valid, missing) = checkFavorites(root, favorites)
                missingPaths.addAll(missing)
                valid
            }

            return@withContext PartialResult(
                favoritesByRoot.toMap(),
                missingPaths.toList()
            )
        }

    suspend fun addRoot(root: Path) = withContext(Dispatchers.IO) {
        root.arkFolder().createDirectories()
        folders = provideFolders() + mapOf(root to readFavorites(root))

        writeRoots()
    }

    suspend fun addFavorite(root: Path, fav: Path) = withContext(Dispatchers.IO) {
        val mutFolders = provideFolders().toMutableMap()
        mutFolders[root] = mutFolders[root]?.let {
            it + listOf(fav)
        } ?: listOf(fav)
        folders = mutFolders

        writeFavorites(root)
    }

    suspend fun forgetRoot(root: Path) =
        withContext(Dispatchers.IO) {
            if (folders.containsKey(root)) {
                val folders = this@FoldersRepo.folders
                    .toMutableMap()
                folders.remove(root)
                this@FoldersRepo.folders = folders
                writeRoots()
            }
        }

    suspend fun deleteRoot(root: Path) =
        withContext(Dispatchers.IO) {
            if (folders.containsKey(root)) {
                forgetRoot(root)
                Log.d(
                    LOG_TAG,
                    "$root forgotten successfully"
                )
                if (deleteFilesRecursively(root)) {
                    Log.d(
                        LOG_TAG,
                        "$root deleted successfully"
                    )
                } else
                    Log.d(
                        LOG_TAG,
                        "failed to delete $root"
                    )
            }
        }

    suspend fun forgetFavorite(root: Path, favorite: Path) =
        withContext(Dispatchers.IO) {
            if (folders.containsKey(root)) {
                var favorites = folders[root]
                val favRelative = root.relativize(favorite)
                if (favorites != null && favorites.contains(favRelative)) {
                    favorites = favorites.minus(favRelative)
                    folders = folders.plus(mapOf(root to favorites))
                    writeFavorites(root)
                }
            }
        }

    suspend fun deleteFavorite(root: Path, favorite: Path) =
        withContext(Dispatchers.IO) {
            forgetFavorite(root, favorite)
            Log.d(
                LOG_TAG,
                "$favorite forgotten successfully"
            )
            if (deleteFilesRecursively(favorite)) {
                Log.d(
                    LOG_TAG,
                    "$favorite deleted successfully"
                )
            } else
                Log.d(
                    LOG_TAG,
                    " failed to delete $favorite"
                )
        }

    private fun deleteFilesRecursively(path: Path) =
        path.toFile().deleteRecursively()

    private fun readRoots(): List<Path> {
        val arkGlobal = deviceRoot.arkGlobal()
        if (arkGlobal.notExists()) return emptyList()
        val rootsFile = arkGlobal.arkRoots()

        return try {
            val jsonRoots = klaxon.parse<JsonRoots>(rootsFile.toFile())
            jsonRoots!!.roots.map { Path(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeRoots() {
        val arkGlobal = deviceRoot.arkGlobal().createDirectories()
        val rootsFile = arkGlobal.arkRoots()
        val jsonRoots = JsonRoots(folders.keys.map { it.toString() })
        rootsFile.writeText(klaxon.toJsonString(jsonRoots))
    }

    private fun readFavorites(root: Path): List<Path> {
        val arkFolder = root.arkFolder()
        require(arkFolder.exists()) { "Ark folder must exist" }
        val favoritesFile = arkFolder.arkFavorites()

        return try {
            val jsonFavorites = klaxon.parse<JsonFavorites>(favoritesFile.toFile())
            jsonFavorites!!.favorites.map { Path(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeFavorites(root: Path) {
        val arkFolder = root.arkFolder()
        require(arkFolder.exists()) { "Ark folder must exist" }
        val favsFile = arkFolder.arkFavorites()
        val jsonFavs = JsonFavorites(folders[root]!!.map(Path::toString))
        favsFile.writeText(klaxon.toJsonString(jsonFavs))
    }

    private fun checkFavorites(
        root: Path,
        favoritesRelatives: List<Path>
    ): PartialResult<List<Path>, List<Path>> {
        val missingPaths = mutableListOf<Path>()

        val validFavoritesRelatives = favoritesRelatives.filter {
            val favorite = root.resolve(it)
            val valid = favorite.exists()
            if (!valid) missingPaths.add(favorite)
            valid
        }

        return PartialResult(validFavoritesRelatives, missingPaths)
    }

    companion object {
        private const val LOG_TAG = "FoldersRepo"
        const val FORGET_ROOT_KEY = "forget root key"
        const val FORGET_FAVORITE_KEY = "forget favorite key"
        const val ROOT_KEY = "root key"
        const val FAVORITE_KEY = "favorite key"
        const val DELETE_FOLDER_KEY = "delete folder key"
        lateinit var instance: FoldersRepo

        fun init(appCtx: Context) {
            instance = FoldersRepo(appCtx)
        }
    }
}

private data class JsonFavorites(val favorites: List<String>)
private data class JsonRoots(val roots: List<String>)