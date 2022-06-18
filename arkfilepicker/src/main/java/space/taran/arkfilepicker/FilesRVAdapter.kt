package space.taran.arkfilepicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import space.taran.arkfilepicker.databinding.ArkFilePickerItemFileBinding
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name

internal class FilesRVAdapter(
    private val viewModel: ArkFilePickerViewModel,
    private val itemsPluralId: Int,
) : RecyclerView.Adapter<FolderViewHolder>() {
    var files = listOf<Path>()

    override fun getItemCount(): Int = files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FolderViewHolder(
            ArkFilePickerItemFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            itemsPluralId
        )

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val file = files[position]

        holder.bind(file)

        holder.itemView.setOnClickListener {
            viewModel.onItemClick(file)
        }
    }
}

internal class FolderViewHolder(
    val binding: ArkFilePickerItemFileBinding,
    private val itemsPluralId: Int
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(path: Path) {
        val context = binding.root.context
        binding.tvName.text = path.name
        if (path.isDirectory()) {
            val childrenCount =  try {
                path.listChildren().size
            } catch (e: Exception) {
                0
            }
            binding.tvDetails.text = context.resources.getQuantityString(
                itemsPluralId,
                childrenCount,
                childrenCount
            )

            Glide.with(binding.iv).clear(binding.iv)
            binding.iv.setImageResource(R.drawable.ark_file_picker_ic_folder)
        } else {
            binding.tvDetails.text = path.fileSize().formatSize()
            Glide.with(binding.iv)
                .load(path.toFile())
                .override(200)
                .placeholder(binding.iv.iconForExtension(path.extension.lowercase()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.iv)
        }
    }
}