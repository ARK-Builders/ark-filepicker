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
    private val onForgetClick: (FolderNode) -> Unit,
    private val showOptions: Boolean
) {
    private val nodeAdapter = ItemAdapter<GenericItem>()
    private var nodes = mutableListOf<FolderNode>()

    init {
        rv.adapter = FastAdapter.with(nodeAdapter)
    }

    fun set(devices: List<Path>, rootsWithFavs: Map<Path, List<Path>>) {
        val deviceNodes = buildDeviceNodes(devices, rootsWithFavs)
        if (nodes.isEmpty()) {
            deviceNodes.forEachIndexed { index, node ->
                if (node is DeviceNode) {
                    node.isExpanded = true
                    deviceNodes.addAll(
                        index + 1,
                        node.children
                    )
                }
            }
        }
        val restoredNodes = restoreExpandedState(deviceNodes)
        setNodes(restoredNodes)
    }

    private fun setNodes(newNodes: MutableList<FolderNode>) {
        nodes = newNodes
        val items = nodes.map { node ->
            when (node) {
                is DeviceNode -> DeviceFolderItem(node, ::onExpandClick)
                is RootNode -> RootFolderItem(
                    node,
                    onNavigateClick,
                    ::onExpandClick,
                    onAddClick,
                    onForgetClick,
                    showOptions
                )
                is FavoriteNode -> FavoriteFolderItem(node, onNavigateClick, onForgetClick)
            }
        }
        FastAdapterDiffUtil[nodeAdapter] = items
    }

    private fun restoreExpandedState(
        newNodes: MutableList<FolderNode>
    ): MutableList<FolderNode> {
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
        val actualNode = nodes.find { it.path == node.path }!!

        if (actualNode.isExpanded)
            removeChildrenCascade(actualNode)
        else
            insertChildren(actualNode)

        setNodes(nodes)
    }

    private fun insertChildren(parent: FolderNode) {
        parent.isExpanded = true
        val parentPos = nodes.indexOfFirst { it.path == parent.path }
        nodes.addAll(parentPos + 1, parent.children)
    }

    private fun removeChildrenCascade(parent: FolderNode) {
        parent.isExpanded = false
        parent.children.forEach { child ->
            removeChildrenCascade(child)
            nodes
                .find { it.path == child.path }
                ?.let { nodes.remove(it) }
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