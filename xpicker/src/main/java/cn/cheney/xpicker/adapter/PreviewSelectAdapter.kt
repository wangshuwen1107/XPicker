package cn.cheney.xpicker.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.entity.MediaEntity
import java.io.File

typealias PreviewSelectClickListener = (media: MediaEntity) -> Unit

class PreviewSelectAdapter : RecyclerView.Adapter<PreviewSelectAdapter.MediaSelectHolder>() {

    var itemClickListener: PreviewSelectClickListener? = null

    var selectList: ArrayList<MediaEntity>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var currentPreviewId: Uri? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaSelectHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.xpicker_item_preivew_select, parent, false)
        return MediaSelectHolder(view)
    }

    override fun getItemCount(): Int {
        return if (null == selectList) 0 else selectList!!.size
    }

    override fun onBindViewHolder(holder: MediaSelectHolder, position: Int) {
        XPicker.imageLoadListener?.invoke(
            selectList!![position].localUri!!,
            holder.photoIv
        )
        if (selectList!![position].localUri == currentPreviewId) {
            holder.photoBg.visibility = View.VISIBLE
        } else {
            holder.photoBg.visibility = View.GONE
        }
        if (selectList!![position].fileType == XPickerConstant.FILE_TYPE_VIDEO) {
            holder.videoIv.visibility = View.VISIBLE
        } else {
            holder.videoIv.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(selectList!![position])
        }
    }


    class MediaSelectHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.preview_item_select_iv)
        var photoBg: ImageView = contentView.findViewById(R.id.preview_item_select_bg)
        var videoIv: ImageView = contentView.findViewById(R.id.preview_item_video_iv)
    }

}