package cn.cheney.xpicker.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.entity.MediaFolder
import java.io.File


typealias FolderClickListener = (position: Int, mediaFolder: MediaFolder) -> Unit

class FolderAdapter : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    var itemClickListener: FolderClickListener? = null


    var folderList: List<MediaFolder>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var chooseName: String? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.xpicker_item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (null == folderList) 0 else folderList!!.size
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val mediaFolder = folderList!![position]
        val localPath = mediaFolder.firstImagePath
        XPicker.onImageLoad(File(localPath), holder.photoIv, mediaFolder.firstImageMineType)
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(position, mediaFolder)
        }
        holder.nameTv.text = mediaFolder.name
        holder.numTv.text = "(${mediaFolder.imageNum})"
        if (position == (folderList!!.size - 1)) {
            holder.line.visibility = View.GONE
        } else {
            holder.line.visibility = View.VISIBLE
        }
        if (chooseName.equals(mediaFolder.name)) {
            holder.chooseIv.visibility = View.VISIBLE
        } else {
            holder.chooseIv.visibility = View.GONE
        }

    }


    class FolderViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.folder_iv)
        var nameTv: TextView = contentView.findViewById(R.id.folder_name_tv)
        var numTv: TextView = contentView.findViewById(R.id.folder_num_tv)
        var chooseIv: ImageView = contentView.findViewById(R.id.folder_choose_iv)
        var line: View = contentView.findViewById(R.id.folder_line)

    }


}