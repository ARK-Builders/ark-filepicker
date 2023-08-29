package space.taran.arkfilepicker.presentation.filepicker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import space.taran.arkfilepicker.FileUtils
import space.taran.arkfilepicker.folders.ArkRootsScan
import space.taran.arkfilepicker.listChildren
import space.taran.arkfilepicker.folders.FoldersRepo
import java.nio.file.Path
import kotlin.io.path.isDirectory

enum class ArkFilePickerMode {
    FILE, FOLDER, ANY
}

internal data class FilePickerState(
    val devices: List<Path>,
    val selectedDevicePos: Int,
    val currentPath: Path,
    val files: List<Path>,
    val rootsWithFavs: Map<Path, List<Path>>
) {
    val currentDevice get() = devices[selectedDevicePos]
    val arkRootsAvailable get() = rootsWithFavs.isNotEmpty()
}

internal sealed class FilePickerSideEffect {
    object DismissDialog : FilePickerSideEffect()
    object ToastAccessDenied : FilePickerSideEffect()
    class NotifyPathPicked(val path: Path) : FilePickerSideEffect()
}

internal class ArkFilePickerViewModel(
    private val fileUtils: FileUtils,
    private val mode: ArkFilePickerMode,
    private val initialPath: Path?
): ViewModel(), ContainerHost<FilePickerState, FilePickerSideEffect> {

    private val foldersRepo = FoldersRepo.instance

    private val arkRootsScanner = ArkRootsScan()

    override val container: Container<FilePickerState, FilePickerSideEffect> =
        container(initialState())

    init {
        viewModelScope.launch {
            val rootsWithFavs = foldersRepo.provideFolders()
            intent {
                reduce {
                    state.copy(rootsWithFavs = rootsWithFavs)
                }
            }
        }
    }

    fun onItemClick(path: Path) = intent {
        if (path.isDirectory()) {
            try {
                reduce {
                    state.copy(
                        currentPath = path,
                        files = formatChildren(path)
                    )
                }
            } catch (e: Exception) {
                postSideEffect(FilePickerSideEffect.ToastAccessDenied)
            }
            return@intent
        }

        if (mode != ArkFilePickerMode.FOLDER)
            onPathPicked(path)
    }

    fun onPickBtnClick() = intent { onPathPicked(state.currentPath) }

    fun onDeviceSelected(selectedDevicePos: Int) = intent {
        val selectedDevice = state.devices[selectedDevicePos]
        reduce {
            state.copy(
                selectedDevicePos = selectedDevicePos,
                currentPath = selectedDevice,
                files = formatChildren(selectedDevice)
            )
        }
    }

    fun onBackClick() = intent {
        val isDevice = state.devices.any { device -> device == state.currentPath }
        if (isDevice) {
            postSideEffect(FilePickerSideEffect.DismissDialog)
            return@intent
        }
        val parent = state.currentPath.parent

        reduce {
            state.copy(
                currentPath = parent,
                files = formatChildren(parent)
            )
        }
    }

    private fun onScanArkRoots() {
        viewModelScope.launch {

        }
    }

    private fun onPathPicked(path: Path) = intent {
        postSideEffect(FilePickerSideEffect.NotifyPathPicked(path))
        postSideEffect(FilePickerSideEffect.DismissDialog)
    }

    private fun initialState(): FilePickerState {
        val devices = fileUtils.listDevices()
        val currentPath = initialPath ?: devices[0]
        val selectedDevice =
            devices.find { currentPath.startsWith(it) } ?: devices[0]
        val selectedDevicePos = devices.indexOf(selectedDevice)
        return FilePickerState(
            devices,
            selectedDevicePos,
            currentPath,
            formatChildren(currentPath),
            emptyMap()
        )
    }

    private fun formatChildren(path: Path): List<Path> {
        val (dirs, files) = path.listChildren().partition {
            it.isDirectory()
        }

        val children = mutableListOf<Path>()
        children.addAll(dirs.sorted())
        children.addAll(files.sorted())

        return children
    }
}

internal class ArkFilePickerViewModelFactory(
    private val fileUtils: FileUtils,
    private val mode: ArkFilePickerMode,
    private val initialPath: Path?
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ArkFilePickerViewModel(fileUtils, mode, initialPath) as T
}