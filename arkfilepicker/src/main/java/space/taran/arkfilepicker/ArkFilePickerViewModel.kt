package space.taran.arkfilepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal data class State(
    val devices: List<Path>,
    val selectedDevicePos: Int,
    val currentPath: Path,
    val files: List<Path>
) {
    val currentDevice get() = devices[selectedDevicePos]
}

internal sealed class SideEffect {
    object DismissDialog : SideEffect()
    object ToastAccessDenied : SideEffect()
    class NotifyPathPicked(val path: Path) : SideEffect()
    class NotifyFolderChanged(val folder: Path) : SideEffect()
}

enum class ArkFilePickerMode {
    FILE, FOLDER, ANY
}

internal class ArkFilePickerViewModel(
    private val fileUtils: FileUtils,
    private val mode: ArkFilePickerMode,
    private val initialPath: Path?
) : ViewModel() {
    val stateFlow = MutableStateFlow(initialState())
    val sideEffectFlow = MutableSharedFlow<SideEffect>()

    private val state get() = stateFlow.value
    private fun postState(state: State) {
        stateFlow.value = state
    }

    private fun postSideEffect(effect: SideEffect) =
        viewModelScope.launch {
            sideEffectFlow.emit(effect)
        }

    fun onItemClick(path: Path) = viewModelScope.launch {
        if (path.isDirectory()) {
            try {
                postState(
                    state.copy(
                        currentPath = path,
                        files = formatChildren(path)
                    )
                )
                postSideEffect(SideEffect.NotifyFolderChanged(path))
            } catch (e: Exception) {
                postSideEffect(SideEffect.ToastAccessDenied)
            }
            return@launch
        }

        if (mode != ArkFilePickerMode.FOLDER)
            onPathPicked(path)
    }

    fun onPickBtnClick() = onPathPicked(state.currentPath)

    fun onDeviceSelected(selectedDevicePos: Int) {
        val selectedDevice = state.devices[selectedDevicePos]
        postState(
            state.copy(
                selectedDevicePos = selectedDevicePos,
                currentPath = selectedDevice,
                files = formatChildren(selectedDevice)
            )
        )
        postSideEffect(SideEffect.NotifyFolderChanged(selectedDevice))
    }

    fun onBackClick(): Boolean {
        val isDevice = state.devices.any { device -> device == state.currentPath }
        if (isDevice)
            return false
        val parent = state.currentPath.parent

        postState(
            state.copy(
                currentPath = parent,
                files = formatChildren(parent)
            )
        )
        postSideEffect(
            SideEffect.NotifyFolderChanged(parent)
        )
        return true
    }

    private fun onPathPicked(path: Path) {
        postSideEffect(SideEffect.NotifyPathPicked(path))
        postSideEffect(SideEffect.DismissDialog)
    }

    private fun initialState(): State {
        val devices = fileUtils.listDevices()
        val currentPath = initialPath ?: devices[0]
        val selectedDevice =
            devices.find { currentPath.startsWith(it) } ?: devices[0]
        val selectedDevicePos = devices.indexOf(selectedDevice)
        return State(
            devices,
            selectedDevicePos,
            currentPath,
            formatChildren(currentPath)
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ArkFilePickerViewModel(fileUtils, mode, initialPath) as T
    }
}