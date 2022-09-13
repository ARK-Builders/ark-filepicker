package space.taran.arkfilepicker.presentation.folderstree

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import java.nio.file.Path

class FolderTreeView(
    private val rv: RecyclerView,
    private val onNavigateClick: (FolderNode) -> Unit,
    private val onAddClick: (FolderNode) -> Unit,
    private val showAdd: Boolean
) {
    private val nodeAdapter = ItemAdapter<GenericItem>()
    private var nodes = mutableListOf<FolderNode>()

    init {
        rv.adapter = FastAdapter.with(nodeAdapter)
    }

    fun set(devices: List<Path>, rootsWithFavs: Map<Path, List<Path>>) {
        val deviceNodes = buildDeviceNodes(devices, rootsWithFavs)
        val expandedDevicesNodes = deviceNodes.toMutableList()
        deviceNodes.forEach {
            it.isExpanded = true
            expandedDevicesNodes.addAll(
                expandedDevicesNodes.indexOf(it) + 1,
                it.children
            )
        }
        val restoredNodes = restoreExpandedState(expandedDevicesNodes)
        set(restoredNodes)
    }

    private fun set(nodes: List<FolderNode>) {
        this.nodes = nodes.toMutableList()
        val items = nodes.map { node ->
            when (node) {
                is DeviceNode -> DeviceFolderItem(node, ::onExpandClick)
                is RootNode -> RootFolderItem(
                    node,
                    onNavigateClick,
                    ::onExpandClick,
                    onAddClick,
                    showAdd
                )
                is FavoriteNode -> FavoriteFolderItem(node, onNavigateClick)
            }
        }
        FastAdapterDiffUtil[nodeAdapter] = items
    }

    private fun restoreExpandedState(
        newNodes: MutableList<FolderNode>
    ): List<FolderNode> {
        val tmpNodes = mutableListOf<FolderNode>()
        newNodes.forEach { newNode ->
            tmpNodes.add(newNode)
            restoreNode(newNode, tmpNodes)
        }
        return tmpNodes
    }

    private fun restoreNode(node: FolderNode, tmpNodes: MutableList<FolderNode>) {
        val oldNode = nodes.find { it.path == node.path }
        val pos = tmpNodes.indexOf(node)
        oldNode?.let {
            if (it.isExpanded) {
                node.isExpanded = true
                tmpNodes.addAll(pos + 1, node.children)
            }
        }
        node.children.forEach { children ->
            restoreNode(children, tmpNodes)
        }
    }

    private fun onExpandClick(node: FolderNode) {
        if (node.isExpanded)
            insertChildren(node)
        else
            removeChildrenCascade(node)

        set(nodes)
    }

    private fun insertChildren(parent: FolderNode) {
        val parentPos = nodes.indexOf(parent)
        parent.isExpanded = true
        nodes.addAll(parentPos + 1, parent.children)
    }

    private fun removeChildrenCascade(parent: FolderNode) {
        parent.isExpanded = false
        parent.children.forEach {
            removeChildrenCascade(it)
            nodes.remove(it)
        }
    }

    private fun buildDeviceNodes(
        devices: List<Path>,
        rootsWithFavs: Map<Path, List<Path>>
    ): MutableList<FolderNode> {
        Log.d(LOG_TAG, "preparing FoldersTree to display")
        Log.d(LOG_TAG, "devices = $devices")
        Log.d(LOG_TAG, "folders = $rootsWithFavs")

        return rootsWithFavs
            .mapKeys { (root, _) ->
                val idx = devices.indexOfFirst { root.startsWith(it) }
                if (idx < 0) {
                    throw IllegalStateException("No device contains $root")
                }

                idx to root
            }
            .toList()
            .groupBy { (deviceAndRoot, _) ->
                val (device, _) = deviceAndRoot
                device
            }.map { (idx, folders) ->
                val device = devices[idx]

                val roots = folders.map { (deviceAndRoot, _favorites) ->
                    val (_, root) = deviceAndRoot

                    val favorites = _favorites.map {
                        FavoriteNode(
                            it.toString(),
                            root.resolve(it),
                            root
                        )
                    }

                    Log.d(
                        LOG_TAG,
                        "root $root contains favorites ${
                            favorites.map { it.path }
                        }"
                    )
                    RootNode(
                        device.relativize(root).toString(),
                        root,
                        favorites
                    )
                }

                Log.d(
                    LOG_TAG,
                    "device $device contains roots ${
                        roots.map { it.path }
                    }"
                )
                DeviceNode(
                    device.getName(1).toString(),
                    device,
                    roots
                )
            }
            .toMutableList()
    }

    companion object {
        private const val LOG_TAG = "folders-tree"
    }
}