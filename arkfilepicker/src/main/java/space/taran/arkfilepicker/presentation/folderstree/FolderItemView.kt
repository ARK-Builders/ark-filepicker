package space.taran.arkfilepicker.presentation.folderstree

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import space.taran.arkfilepicker.databinding.ArkFilePickerItemDeviceBinding
import space.taran.arkfilepicker.databinding.ArkFilePickerItemFavoriteBinding
import space.taran.arkfilepicker.databinding.ArkFilePickerItemRootBinding

internal class DeviceFolderItem(
    private val node: DeviceNode,
    private val onExpandClick: (DeviceNode) -> Unit,
) : AbstractBindingItem<ArkFilePickerItemDeviceBinding>() {
    override val type = 0
    override var identifier: Long
        get() = node.path.hashCode().toLong()
        set(value) {}

    private var isExpanded = false
    private lateinit var animator: ValueAnimator

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemDeviceBinding
        .inflate(inflater, parent, false)
        .also { binding ->
            animator = ValueAnimator().apply {
                duration = 500L
                addUpdateListener {
                    binding.ivChevron.rotation = animatedValue as Float
                }
            }
        }

    override fun bindView(
        binding: ArkFilePickerItemDeviceBinding,
        payloads: List<Any>
    ) = with(binding) {
        ivChevron.rotation = if (node.isExpanded) 90f else 0f
        tvDeviceName.text = node.name
        root.setOnClickListener {
            animateExpanded(!node.isExpanded)
            onExpandClick(node)
        }
    }

    private fun animateExpanded(expanded: Boolean) {
        if (expanded) {
            animator.setFloatValues(0F, 90F)
            animator.start()
        } else {
            animator.setFloatValues(90F, 00F)
            animator.start()
        }

        isExpanded = expanded
        node.isExpanded = expanded
    }
}

internal class RootFolderItem(
    private val node: RootNode,
    private val onNavigateClick: (RootNode) -> Unit,
    private val onExpandClick: (RootNode) -> Unit,
    private val onAddClick: (RootNode) -> Unit,
    private val showAdd: Boolean
) : AbstractBindingItem<ArkFilePickerItemRootBinding>() {
    override val type = 1
    override var identifier: Long
        get() = node.path.hashCode().toLong()
        set(value) {}

    private var chevron: ImageView? = null
    private var isExpanded = false
    private val animator = ValueAnimator().apply {
        duration = 500L
        addUpdateListener {
            chevron?.rotation = animatedValue as Float
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemRootBinding
        .inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemRootBinding,
        payloads: List<Any>
    ) = with(binding) {
        this@RootFolderItem.chevron = ivChevron
        ivChevron.rotation = if (node.isExpanded) 90f else 0f
        layoutAdd.isVisible = showAdd
        tvRootName.text = node.name
        layoutChevron.setOnClickListener {
            animateExpanded(!node.isExpanded)
            onExpandClick(node)
        }
        layoutAdd.setOnClickListener {
            onAddClick(node)
        }
        root.setOnClickListener {
            onNavigateClick(node)
        }
    }

    private fun animateExpanded(expanded: Boolean) {
        if (expanded) {
            animator.setFloatValues(0F, 90F)
            animator.start()
        } else {
            animator.setFloatValues(90F, 00F)
            animator.start()
        }

        isExpanded = expanded
        node.isExpanded = expanded
    }
}

internal class FavoriteFolderItem(
    private val node: FavoriteNode,
    private val onNavigateClick: (FavoriteNode) -> Unit
) : AbstractBindingItem<ArkFilePickerItemFavoriteBinding>() {
    override val type = 2
    override var identifier: Long
        get() = node.path.hashCode().toLong()
        set(value) {}

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFavoriteBinding
        .inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFavoriteBinding,
        payloads: List<Any>
    ) = with(binding) {
        tvFavName.text = node.name
        root.setOnClickListener {
            onNavigateClick(node)
        }
    }
}