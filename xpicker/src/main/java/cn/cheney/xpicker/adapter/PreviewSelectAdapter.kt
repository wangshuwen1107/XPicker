package cn.cheney.xpicker.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
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

    var currentPreviewId: String? = null
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
            Uri.fromFile(File(selectList!![position].localPath!!)),
            holder.photoIv
        )
        if (selectList!![position].localPath == currentPreviewId) {
            holder.photoBg.visibility = View.VISIBLE
        } else {
            holder.photoBg.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(selectList!![position])
        }
    }


    class MediaSelectHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.select_iv)
        var photoBg: ImageView = contentView.findViewById(R.id.select_bg)
    }

}