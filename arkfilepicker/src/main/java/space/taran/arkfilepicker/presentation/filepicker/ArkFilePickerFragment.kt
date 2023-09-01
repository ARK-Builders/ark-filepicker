package space.taran.arkfilepicker.presentation.filepicker

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.viewmodel.observe
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.presentation.DevicesPopup
import space.taran.arkfilepicker.FileUtils
import space.taran.arkfilepicker.INTERNAL_STORAGE
import space.taran.arkfilepicker.R
import space.taran.arkfilepicker.databinding.ArkFilePickerHostFragmentBinding
import space.taran.arkfilepicker.databinding.ArkFilePickerItemFileBinding
import space.taran.arkfilepicker.databinding.ArkFilePickerItemFilesRootsPageBinding
import space.taran.arkfilepicker.dpToPx
import space.taran.arkfilepicker.formatSize
import space.taran.arkfilepicker.iconForExtension
import space.taran.arkfilepicker.listChildren
import space.taran.arkfilepicker.presentation.args
import space.taran.arkfilepicker.presentation.folderstree.FolderTreeView
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.setDragSensitivity
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name

open class ArkFilePickerFragment :
    DialogFragment(R.layout.ark_file_picker_host_fragment) {

    var titleStringId by args<Int>()
    var pickButtonStringId by args<Int>()
    var cancelButtonStringId by args<Int>()
    var internalStorageStringId by args<Int>()
    var itemsPluralId by args<Int>()
    var themeId by args<Int>()
    var accessDeniedStringId by args<Int>()
    var mode by args<Int>()
    var initialPath by args<String>()
    var showRoots by args<Boolean>()
    var pathPickedRequestKey by args<String>()
    var rootsFirstPage by args<Boolean>()

    var currentFolder: Path? = null
    val binding by viewBinding(ArkFilePickerHostFragmentBinding::bind)
    private val viewModel by viewModels<ArkFilePickerViewModel> {
        ArkFilePickerViewModelFactory(
            FileUtils(requireContext().applicationContext),
            ArkFilePickerMode.values()[mode!!],
            initialPath?.let { Path(it) }
        )
    }

    private val pagesAdapter = ItemAdapter<GenericItem>()

    open fun onFolderChanged(currentFolder: Path) {}
    open fun onPick(pickedPath: Path) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initBackButtonListener()
        viewModel.observe(
            this,
            state = ::render,
            sideEffect = ::handleSideEffect
        )
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(
                requireContext().dpToPx(DIALOG_WIDTH),
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun initUI() = with(binding) {
        btnPick.text = getString(pickButtonStringId!!)
        btnCancel.text = getString(cancelButtonStringId!!)
        tvTitle.text = getString(titleStringId!!)
        if (mode == ArkFilePickerMode.FILE.ordinal)
            btnPick.isVisible = false

        btnCancel.setOnClickListener {
            dismiss()
        }
        btnPick.setOnClickListener {
            viewModel.onPickBtnClick()
        }
        vp.adapter = FastAdapter.with(pagesAdapter)
        vp.offscreenPageLimit = 2
        if (!showRoots!!) {
            vp.getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            vp.setDragSensitivity(10)
        }
        val pages = getPages()

        pagesAdapter.set(pages)
        val tabsTitle = resources
            .getStringArray(R.array.ark_file_picker_tabs)
            .apply {
                if (!rootsFirstPage!!)
                    reverse()
            }

        if (showRoots!!) {
            TabLayoutMediator(tabs, vp) { tab, pos ->
                tab.text = tabsTitle[pos]
            }.attach()
        } else {
            tabs.isVisible = false
        }
    }

    private fun render(state: FilePickerState) = binding.apply {
        displayPath(state)

        val deviceText = if (state.currentDevice == INTERNAL_STORAGE)
            getString(internalStorageStringId!!)
        else
            state.currentDevice.last().toString()

        if (state.currentPath.isDirectory()) {
            if (state.currentPath != currentFolder) {
                currentFolder = state.currentPath
                onFolderChanged(currentFolder!!)
            }
        }

        tvDevice.text = deviceText
        if (state.currentPath == state.currentDevice)
            tvDevice.setTextColor(
                resources.getColor(
                    R.color.ark_file_picker_black,
                    null
                )
            )
        else
            tvDevice.setTextColor(
                resources.getColor(
                    R.color.ark_file_picker_gray,
                    null
                )
            )


        tvDevice.setOnClickListener {
            if (state.devices.size == 1)
                viewModel.onItemClick(state.currentDevice)
            else
                DevicesPopup(
                    requireContext(),
                    state.devices,
                    viewModel
                ).showBelow(it)
        }
    }

    private fun handleSideEffect(effect: FilePickerSideEffect) = when (effect) {
        FilePickerSideEffect.DismissDialog -> dismiss()
        FilePickerSideEffect.ToastAccessDenied -> Toast.makeText(
            requireContext(),
            accessDeniedStringId!!,
            Toast.LENGTH_SHORT
        ).show()

        is FilePickerSideEffect.NotifyPathPicked -> {
            onPick(effect.path)
            setFragmentResult(
                pathPickedRequestKey ?: PATH_PICKED_REQUEST_KEY,
                Bundle().apply {
                    putString(
                        PATH_PICKED_PATH_BUNDLE_KEY,
                        effect.path.toString()
                    )
                })
        }
    }


    private fun displayPath(state: FilePickerState) = binding.apply {
        layoutPath.removeViews(1, layoutPath.childCount - 1)
        val pathWithoutDevice =
            state.currentDevice.relativize(state.currentPath)

        val padding = requireContext().dpToPx(PATH_PART_PADDING)
        var tmpPath = state.currentDevice
        pathWithoutDevice
            .filter { it.toString().isNotEmpty() }
            .forEach { part ->
                tmpPath = tmpPath.resolve(part)
                val fullPathToPart = tmpPath
                val tv = TextView(requireContext())
                val text = "/$part"
                val outValue = TypedValue()
                requireContext().theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                tv.setBackgroundResource(outValue.resourceId)
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                tv.setPadding(padding, 0, 0, 0)
                tv.isClickable = true
                tv.text = text
                if (pathWithoutDevice.last() == part)
                    tv.setTextColor(
                        resources.getColor(
                            R.color.ark_file_picker_black,
                            null
                        )
                    )
                else {
                    tv.setTextColor(
                        resources.getColor(
                            R.color.ark_file_picker_gray,
                            null
                        )
                    )
                    tv.setOnClickListener {
                        viewModel.onItemClick(fullPathToPart)
                    }
                }
                layoutPath.addView(tv)
            }
        scrollPath.post {
            scrollPath.fullScroll(ScrollView.FOCUS_RIGHT)
        }
    }

    override fun getTheme() = themeId!!

    private fun initBackButtonListener() {
        requireDialog().setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                keyEvent.action == KeyEvent.ACTION_UP
            ) {
                viewModel.onBackClick()
            }
            return@setOnKeyListener true
        }
    }

    private fun getPages() = if (showRoots!!) {
        if (rootsFirstPage!!) {
            listOf(
                RootsPage(this, viewModel),
                FilesPage(this, viewModel, itemsPluralId!!)
            )
        } else {
            listOf(
                FilesPage(this, viewModel, itemsPluralId!!),
                RootsPage(this, viewModel)
            )
        }
    } else {
        listOf(
            FilesPage(this, viewModel, itemsPluralId!!)
        )
    }


    fun setConfig(config: ArkFilePickerConfig) {
        titleStringId = config.titleStringId
        pickButtonStringId = config.pickButtonStringId
        cancelButtonStringId = config.cancelButtonStringId
        internalStorageStringId = config.internalStorageStringId
        accessDeniedStringId = config.accessDeniedStringId
        itemsPluralId = config.itemsPluralId
        themeId = config.themeId
        initialPath = config.initialPath?.toString()
        showRoots = config.showRoots
        pathPickedRequestKey = config.pathPickedRequestKey
        rootsFirstPage = config.rootsFirstPage
        mode = config.mode.ordinal
    }

    companion object {
        const val PATH_PICKED_REQUEST_KEY = "arkFilePickerPathPicked"
        const val PATH_PICKED_PATH_BUNDLE_KEY = "arkFilePickerPathPickedPathKey"

        fun newInstance(config: ArkFilePickerConfig) =
            ArkFilePickerFragment().apply {
                setConfig(config)
            }

        private const val DIALOG_WIDTH = 300f
        private const val PATH_PART_PADDING = 4f
    }
}

internal class FilesPage(
    private val fragment: Fragment,
    private val viewModel: ArkFilePickerViewModel,
    private val itemsPluralId: Int
) : AbstractBindingItem<ArkFilePickerItemFilesRootsPageBinding>() {
    private val filesAdapter = ItemAdapter<FileItem>()
    private var currentFiles = emptyList<Path>()

    override val type = 0

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFilesRootsPageBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFilesRootsPageBinding,
        payloads: List<Any>
    ) = with(binding) {
        rvFiles.adapter = FastAdapter.with(filesAdapter)
        viewModel.observe(fragment, state = ::render)
    }

    private fun render(state: FilePickerState) {
        if (currentFiles == state.files) return

        filesAdapter.setNewList(state.files.map {
            FileItem(it, viewModel, itemsPluralId)
        })

        currentFiles = state.files
    }
}

internal class FileItem(
    private val file: Path,
    private val viewModel: ArkFilePickerViewModel,
    private val itemsPluralId: Int
) : AbstractBindingItem<ArkFilePickerItemFileBinding>() {
    override val type = 0

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFileBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFileBinding,
        payloads: List<Any>
    ) = with(binding) {
        root.setOnClickListener {
            viewModel.onItemClick(file)
        }
        binding.tvName.text = file.name
        if (file.isDirectory()) bindFolder(file, this)
        else bindRegularFile(file, this)
        return@with
    }

    private fun bindRegularFile(
        file: Path,
        binding: ArkFilePickerItemFileBinding
    ) = with(binding) {
        binding.tvDetails.text = file.fileSize().formatSize()
        Glide.with(binding.iv)
            .load(file.toFile())
            .override(200)
            .placeholder(binding.iv.iconForExtension(file.extension.lowercase()))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.iv)
    }

    private fun bindFolder(
        folder: Path,
        binding: ArkFilePickerItemFileBinding
    ) = with(binding) {
        val childrenCount = try {
            folder.listChildren().size
        } catch (e: Exception) {
            0
        }
        binding.tvDetails.text = binding.root.context.resources.getQuantityString(
            itemsPluralId,
            childrenCount,
            childrenCount
        )

        Glide.with(binding.iv).clear(binding.iv)
        binding.iv.setImageResource(R.drawable.ark_file_picker_ic_folder)
    }
}

internal class RootsPage(
    private val fragment: Fragment,
    private val viewModel: ArkFilePickerViewModel
) : AbstractBindingItem<ArkFilePickerItemFilesRootsPageBinding>() {
    private lateinit var folderTreeView: FolderTreeView
    private var currentRootsWithFavs = mapOf<Path, List<Path>>()

    override val type = 1

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFilesRootsPageBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFilesRootsPageBinding,
        payloads: List<Any>
    ) = with(binding) {
        folderTreeView = FolderTreeView(
            rvFiles,
            onNavigateClick = { node -> viewModel.onItemClick(node.path) },
            onAddClick = {},
            onForgetClick = {},
            showOptions = false
        )
        viewModel.observe(fragment, state = ::render)
    }

    private fun render(state: FilePickerState) {
        if (currentRootsWithFavs == state.rootsWithFavs) return

        folderTreeView.set(state.devices, state.rootsWithFavs)

        currentRootsWithFavs = state.rootsWithFavs
    }
}