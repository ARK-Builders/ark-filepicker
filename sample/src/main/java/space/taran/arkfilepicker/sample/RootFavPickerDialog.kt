package space.taran.arkfilepicker.sample

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import java.nio.file.Path

class RootFavPickerDialog : ArkFilePickerFragment() {
    var rootNotFavorite = false

    override fun onFolderChanged(currentFolder: Path) {
        lifecycleScope.launch {
            val folders = FoldersRepo.instance.provideFolders()
            val roots = folders.keys
            val favorites = folders.values.flatten()
            val root = roots.find { root -> currentFolder.startsWith(root) }
            root?.let {
                if (root == currentFolder) {
                    rootNotFavorite = true
                    binding.btnPick.text = "Root"
                    binding.btnPick.isEnabled = false
                } else {
                    var foundAsFavorite = false
                    favorites.forEach {
                        if (currentFolder.endsWith(it)) {
                            foundAsFavorite = true
                            return@forEach
                        }
                    }
                    rootNotFavorite = false
                    binding.btnPick.text = "Favorite"
                    binding.btnPick.isEnabled = !foundAsFavorite
                }
            } ?: let {
                rootNotFavorite = true
                binding.btnPick.text = "Root"
                binding.btnPick.isEnabled = true
            }
        }
    }

    override fun onPick(pickedPath: Path) {
        Toast.makeText(
            requireContext(),
            "rootNotFavorite [$rootNotFavorite]",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        fun newInstance() = RootFavPickerDialog().apply {
            setConfig(ArkFilePickerConfig())
        }
    }
}