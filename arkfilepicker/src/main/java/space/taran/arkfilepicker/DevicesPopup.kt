package space.taran.arkfilepicker

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import java.nio.file.Path

internal class DevicesPopup(
    val context: Context,
    val devices: List<Path>,
    val viewModel: ArkFilePickerViewModel
) {
    val popupWindow: PopupWindow
    val layout = createLayout()

    init {
        layout.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        popupWindow = PopupWindow(context)
        popupWindow.apply {
            contentView = layout
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height = LinearLayout.LayoutParams.WRAP_CONTENT
            isFocusable = true
            animationStyle = R.style.ARKFilePickerFadeAnimation
            setBackgroundDrawable(ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ark_file_picker_bg_round_16,
                null
            ))
            elevation = context.dpToPx(24f).toFloat()
        }
    }

    fun showBelow(target: View) {
        val targetRect = getViewRectOnScreen(target)
        val xOffset = (target.width - layout.measuredWidth) / 2
        val popupLeft = targetRect.left + xOffset
        popupWindow.showAtLocation(
            target,
            Gravity.NO_GRAVITY,
            popupLeft,
            targetRect.bottom + context.dpToPx(4f)
        )
    }

    private fun getViewRectOnScreen(view: View): Rect {
        val location = IntArray(2).apply {
            view.getLocationInWindow(this)
        }
        return Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
    }

    private fun createLayout(): View {
        val linear = LinearLayout(context)
        linear.orientation = LinearLayout.VERTICAL
        val padding = context.dpToPx(14f)
        devices.forEachIndexed { index, path ->
            val tv = TextView(context)
            val text = if (path == INTERNAL_STORAGE)
                context.getString(R.string.ark_file_picker_internal_storage)
            else
                path.last().toString()

            tv.text = text
            tv.setTextColor(context.resources.getColor(R.color.ark_file_picker_black, null))
            tv.setPadding(padding, padding, padding, padding)
            tv.setOnClickListener {
                viewModel.onDeviceSelected(index)
                popupWindow.dismiss()
            }
            linear.addView(tv)
        }
        return linear
    }
}