package space.taran.arkfilepicker.presentation.folderstree

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.skydoves.balloon.Balloon
import space.taran.arkfilepicker.INTERNAL_STORAGE
import space.taran.arkfilepicker.R
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

    private var isExpanded = node.isExpanded
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
        val context = root.context
        ivChevron.rotation = if (isExpanded) 90f else 0f
        tvDeviceName.text =
            if (node.path == INTERNAL_STORAGE)
                context.getString(R.string.ark_file_picker_internal_storage)
            else node.name
        root.setOnClickListener {
            animateExpanded(!isExpanded)
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
    }
}

internal class RootFolderItem(
    private val node: RootNode,
    private val onNavigateClick: (RootNode) -> Unit,
    private val onExpandClick: (RootNode) -> Unit,
    private val onAddClick: (RootNode) -> Unit,
    private val onForgetClick: (RootNode) -> Unit,
    private val showOptions: Boolean
) : AbstractBindingItem<ArkFilePickerItemRootBinding>() {
    override val type = 1
    override var identifier: Long
        get() = node.path.hashCode().toLong()
        set(value) {}

    private var chevron: ImageView? = null
    private var isExpanded = node.isExpanded
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
        ivChevron.rotation = if (isExpanded) 90f else 0f
        tvRootName.text = node.name
        layoutChevron.setOnClickListener {
            animateExpanded(!isExpanded)
            onExpandClick(node)
        }
        root.setOnClickListener {
            onNavigateClick(node)
        }
        with(layoutMoreOptions) {
            isVisible = showOptions
            setOnClickListener {
                val lifecycleOwner = it.findViewTreeLifecycleOwner()
                val balloon = Balloon.Builder(it.context)
                    .setLayout(R.layout.root_options)
                    .setBackgroundColorResource(R.color.ark_file_picker_white)
                    .setArrowSize(0)
                    .setLifecycleOwner(lifecycleOwner)
                    .build()
                balloon.showAsDropDown(it)
                val addRoot: View = balloon.getContentView()
                    .findViewById(R.id.layout_add)
                val forgetRoot: View = balloon.getContentView()
                    .findViewById(R.id.layout_forget)
                addRoot.setOnClickListener {
                    onAddClick(node)
                    balloon.dismiss()
                }
                forgetRoot.setOnClickListener {
                    onForgetClick(node)
                    balloon.dismiss()
                }
            }
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
    }
}

internal class FavoriteFolderItem(
    private val node: FavoriteNode,
    private val onNavigateClick: (FavoriteNode) -> Unit,
    private val onForgetClick: (FavoriteNode) -> Unit
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
        layoutMoreOptions.setOnClickListener {
            val lifecycleOwner = it.findViewTreeLifecycleOwner()
            val balloon = Balloon.Builder(it.context)
                .setLayout(R.layout.favorite_options)
                .setBackgroundColorResource(R.color.ark_file_picker_white)
                .setLifecycleOwner(lifecycleOwner)
                .setArrowSize(0)
                .build()
            val forgetFavoriteBtn: View = balloon.getContentView()
                .findViewById(R.id.layout_forget)
            balloon.showAsDropDown(it)
            forgetFavoriteBtn.setOnClickListener {
                onForgetClick(node)
                balloon.dismiss()
            }
        }
    }
}