package cn.cheney.lib_picker.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.entity.MediaEntity
import java.io.File

typealias ItemClickListener = (position: Int, mediaEntity: MediaEntity, holder: ViewHolder) -> Unit

class PhotoAdapter : RecyclerView.Adapter<ViewHolder>() {

    var itemClickListener: ItemClickListener? = null

    val holderMap = mutableMapOf<Int, ViewHolder>()

    var mediaList: List<MediaEntity>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.xpicker_item_photo_grid, parent, false)
        return MediaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (null == mediaList) 0 else mediaList!!.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holderMap[position] = holder
        val mediaViewHolder = (holder as MediaViewHolder)
        val mediaEntity = mediaList!![position]
        XPicker.imageLoadListener?.invoke(
            Uri.fromFile(File(mediaEntity.localPath!!)),
            mediaViewHolder.photoIv
        )
        updateItemCheck(position)
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(position, mediaEntity, mediaViewHolder)
        }
    }


    fun updateItemCheck(position: Int) {
        val mediaViewHolder = (holderMap[position] as MediaViewHolder)
        val mediaEntity = mediaList!![position]
        mediaViewHolder.checkTv.isSelected = mediaEntity.selected
        if (mediaEntity.selected) {
            mediaViewHolder.checkTv.text = "${mediaEntity.selectedNum}"
        } else {
            mediaViewHolder.checkTv.text = ""
        }
    }

    fun autoCheck(){


    }
    class MediaViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.photo_iv)
        var checkTv: TextView = contentView.findViewById(R.id.check_tv)

    }

}