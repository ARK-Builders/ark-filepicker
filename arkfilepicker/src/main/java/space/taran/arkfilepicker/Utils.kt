package dev.arkbuilders.arkfilepicker

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.nio.file.Path
import java.text.DecimalFormat
import kotlin.io.path.Path
import kotlin.io.path.isHidden
import kotlin.io.path.listDirectoryEntries
import kotlin.math.pow

data class PartialResult<S, F>(val succeeded: S, val failed: F)

internal val ROOT_PATH: Path = Path("/")

internal val ANDROID_DIRECTORY: Path = Path("Android")

internal fun Path.listChildren(): List<Path> =
    listDirectoryEntries().filter { !it.isHidden() }

internal fun View.iconForExtension(ext: String): Int {
    val drawableID = this.resources
        .getIdentifier(
            "ark_file_picker_ic_file_$ext",
            "drawable",
            this.context.packageName
        )

    return if (drawableID > 0) drawableID
    else R.drawable.ark_file_picker_ic_file
}

internal val INTERNAL_STORAGE = Path("/storage/emulated/0")

internal fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        this.resources.displayMetrics
    ).toInt()

internal class FileUtils(private val appContext: Context) {
    fun listDevices(): List<Path> =
        appContext.getExternalFilesDirs(null)
            .toList()
            .filterNotNull()
            .filter { it.exists() }
            .map {
                it.toPath().toRealPath()
                    .takeWhile { part ->
                        part != ANDROID_DIRECTORY
                    }
                    .fold(ROOT_PATH) { parent, child ->
                        parent.resolve(child)
                    }
            }
}

internal fun Long.formatSize(): String {
    if (this <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
}

internal fun ViewPager2.setDragSensitivity(f: Int) {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView

    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop*f)
}

fun View.setMargin(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(left, top, right, bottom)
    layoutParams = params
}