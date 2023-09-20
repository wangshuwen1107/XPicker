package cn.cheney.picker.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.util.timeParse
import com.bumptech.glide.Glide
import java.io.File


class DemoPhotoAdapter(var context: Context) :
    RecyclerView.Adapter<DemoPhotoAdapter.MediaViewHolder>() {

    var mediaList: List<MediaEntity>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.xpicker_item_photo_grid, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return if (mediaList.isNullOrEmpty()) {
            0
        } else mediaList!!.size
    }


    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaEntity = mediaList!![position]
        if (mediaEntity.fileType == MediaEntity.FILE_TYPE_VIDEO) {
            holder.videoLayer.visibility = View.VISIBLE
            holder.videoDurationTv.text = "${timeParse(mediaEntity.duration.toLong())}"
            holder.gifMarkIv.visibility = View.GONE
        } else {
            if (XPickerConstant.GIF == mediaEntity.mineType) {
                holder.gifMarkIv.visibility = View.VISIBLE
            } else {
                holder.gifMarkIv.visibility = View.GONE
            }
            holder.videoLayer.visibility = View.GONE
        }
        Glide.with(context)
            .load(File(mediaEntity.localPath!!))
            .into(holder.photoIv)
        holder.checkLayer.visibility = View.GONE
    }


    class MediaViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.photo_iv)
        var videoLayer: ViewGroup = contentView.findViewById(R.id.video_layer)
        var videoDurationTv: TextView = contentView.findViewById(R.id.video_duration_tv)
        var gifMarkIv: ImageView = contentView.findViewById(R.id.gif_mark_iv)
        var checkLayer: ViewGroup = contentView.findViewById(R.id.check_tv_layer)
    }


}