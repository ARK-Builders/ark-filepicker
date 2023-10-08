package dev.arkbuilders.arkfilepicker.presentation.folderstree

import java.nio.file.Path

sealed class FolderNode(
    val name: String,
    val path: Path,
    val children: List<FolderNode>,
    var isExpanded: Boolean = false
)

class DeviceNode(name: String, path: Path, children: List<RootNode>) :
    FolderNode(name, path, children)

class RootNode(name: String, path: Path, children: List<FavoriteNode>) :
    FolderNode(name, path, children)

class FavoriteNode(name: String, path: Path, val root: Path) :
    FolderNode(name, path, emptyList())