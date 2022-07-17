package space.taran.arkfilepicker

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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.taran.arkfilepicker.databinding.ArkFilePickerFragmentBinding
import java.nio.file.Path
import kotlin.io.path.Path

open class ArkFilePickerFragment : DialogFragment() {
    var titleStringId by args<Int>()
    var pickButtonStringId by args<Int>()
    var cancelButtonStringId by args<Int>()
    var internalStorageStringId by args<Int>()
    var itemsPluralId by args<Int>()
    var themeId by args<Int>()
    var accessDeniedStringId by args<Int>()
    var mode by args<Int>()
    var initialPath by args<String>()
    var pathPickedRequestKey by args<String>()

    val binding get() = _binding!!

    private var _binding: ArkFilePickerFragmentBinding? = null
    private val viewModel: ArkFilePickerViewModel by viewModels {
        ArkFilePickerViewModelFactory(
            FileUtils(requireContext().applicationContext),
            ArkFilePickerMode.values()[mode!!],
            initialPath?.let { Path(it) }
        )
    }
    private var filesAdapter: FilesRVAdapter? = null

    open fun onFolderChanged(folder: Path) {}

    fun setup(config: ArkFilePickerConfig): ArkFilePickerFragment {
        titleStringId = config.titleStringId
        pickButtonStringId = config.pickButtonStringId
        cancelButtonStringId = config.cancelButtonStringId
        internalStorageStringId = config.internalStorageStringId
        accessDeniedStringId = config.accessDeniedStringId
        itemsPluralId = config.itemsPluralId
        themeId = config.themeId
        initialPath = config.initialPath?.toString()
        mode = config.mode.ordinal
        pathPickedRequestKey = config.pathPickedRequestKey
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ArkFilePickerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initBackButtonListener()
        viewModel.stateFlow.onEach { state ->
            render(state)
        }.launchIn(lifecycleScope)
        viewModel.sideEffectFlow.onEach { effect ->
            handleSideEffect(effect)
        }.launchIn(lifecycleScope)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initUI() = binding.apply {
        btnPick.text = getString(pickButtonStringId!!)
        btnCancel.text = getString(cancelButtonStringId!!)
        tvTitle.text = getString(titleStringId!!)
        if (mode == ArkFilePickerMode.FILE.ordinal)
            btnPick.isVisible = false


        rvRootsDialog.layoutManager = LinearLayoutManager(requireContext())
        filesAdapter = FilesRVAdapter(viewModel, itemsPluralId!!)
        rvRootsDialog.adapter = filesAdapter

        btnCancel.setOnClickListener {
            dismiss()
        }
        btnPick.setOnClickListener {
            viewModel.onPickBtnClick()
        }
    }

    private fun render(state: State) = binding.apply {
        displayPath(state)

        val deviceText = if (state.currentDevice == INTERNAL_STORAGE)
            getString(internalStorageStringId!!)
        else
            state.currentDevice.last().toString()

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

        filesAdapter?.files = state.files
        filesAdapter?.notifyDataSetChanged()
    }

    private fun handleSideEffect(effect: SideEffect) = when (effect) {
        SideEffect.DismissDialog -> dismiss()
        SideEffect.ToastAccessDenied -> Toast.makeText(
            requireContext(),
            accessDeniedStringId!!,
            Toast.LENGTH_SHORT
        ).show()
        is SideEffect.NotifyFolderChanged -> {
            onFolderChanged(effect.folder)
            setFragmentResult(
                FOLDER_CHANGED_REQUEST_KEY,
                Bundle().apply
                {
                    putString(
                        FOLDER_CHANGED_FOLDER_BUNDLE_KEY,
                        effect.folder.toString()
                    )
                })
        }
        is SideEffect.NotifyPathPicked ->
            setFragmentResult(
                pathPickedRequestKey ?: PATH_PICKED_REQUEST_KEY,
                Bundle().apply
                {
                    putString(
                        PATH_PICKED_PATH_BUNDLE_KEY,
                        effect.path.toString()
                    )
                })
    }


    private fun displayPath(state: State) = binding.apply {
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
                if (!viewModel.onBackClick())
                    dismiss()
            }
            return@setOnKeyListener true
        }
    }

    companion object {
        const val PATH_PICKED_REQUEST_KEY = "arkFilePickerPathPicked"
        const val PATH_PICKED_PATH_BUNDLE_KEY = "arkFilePickerPathPickedPathKey"
        const val FOLDER_CHANGED_REQUEST_KEY = "arkFilePickerFolderChanged"
        const val FOLDER_CHANGED_FOLDER_BUNDLE_KEY =
            "arkFilePickerFolderChangedFolderKey"

        fun newInstance(config: ArkFilePickerConfig) = ArkFilePickerFragment().apply {
            setup(config)
        }

        private const val DIALOG_WIDTH = 300f
        private const val PATH_PART_PADDING = 4f
    }
}

fun FragmentManager.onArkPathPicked(
    lifecycleOwner: LifecycleOwner,
    customRequestKey: String? = null,
    listener: (Path) -> Unit,
) {
    setFragmentResultListener(
        customRequestKey ?: ArkFilePickerFragment.PATH_PICKED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkFilePickerFragment.PATH_PICKED_PATH_BUNDLE_KEY)!!
            )
        )
    }
}

fun FragmentManager.onArkFolderChange(
    lifecycleOwner: LifecycleOwner,
    listener: (Path) -> Unit
) {
    setFragmentResultListener(
        ArkFilePickerFragment.FOLDER_CHANGED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkFilePickerFragment.FOLDER_CHANGED_FOLDER_BUNDLE_KEY)!!
            )
        )
    }
}

