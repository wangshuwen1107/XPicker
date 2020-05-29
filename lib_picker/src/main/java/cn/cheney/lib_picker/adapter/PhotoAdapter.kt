package cn.cheney.lib_picker.adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.XPickerConstant
import cn.cheney.lib_picker.entity.MediaEntity
import cn.cheney.lib_picker.util.timeParse
import java.io.File

typealias ItemCheckListener = (position: Int, mediaEntity: MediaEntity, holder: ViewHolder) -> Unit

class PhotoAdapter(var context: Context) : RecyclerView.Adapter<ViewHolder>() {

    var itemCheckListener: ItemCheckListener? = null

    val holderMap = mutableMapOf<Int, ViewHolder>()

    private lateinit var maskShowAnimation: Animation

    var mediaList: List<MediaEntity>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var hasLimit = false
        set(value) {
            val change = value != field
            field = value
            if (change) {
                notifyDataSetChanged()
            }
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
        //底部文件类型图标
        if (mediaEntity.fileType == XPickerConstant.TYPE_VIDEO) {
            holder.videoLayer.visibility = View.VISIBLE
            holder.videoDurationTv.text = "${timeParse(mediaEntity.duration.toLong())}"
            holder.gifMarkIv.visibility = View.GONE
        } else {
            if ("image/gif" == mediaEntity.mineType) {
                holder.gifMarkIv.visibility = View.VISIBLE
            } else {
                holder.gifMarkIv.visibility = View.GONE
            }
            holder.videoLayer.visibility = View.GONE
        }
        //图片加载
        XPicker.imageLoadListener?.invoke(
            Uri.fromFile(File(mediaEntity.localPath!!)),
            mediaViewHolder.photoIv
        )
        //图片选择
        updateItemCheck(position)
        holder.checkLayer.setOnClickListener {
            itemCheckListener?.invoke(position, mediaEntity, mediaViewHolder)
        }
    }


    fun updateItemCheck(position: Int, byClick: Boolean = false) {
        val mediaViewHolder = (holderMap[position] as MediaViewHolder)
        val mediaEntity = mediaList!![position]
        mediaViewHolder.checkTv.isSelected = mediaEntity.selected
        if (mediaEntity.selected) {
            mediaViewHolder.maskIv.setBackgroundColor(Color.parseColor("#80000000"))
            mediaViewHolder.maskIv.visibility = View.VISIBLE
            mediaViewHolder.checkTv.text = "${mediaEntity.selectedNum}"
            if (byClick) {
                playFadeInCheckAnimation(mediaViewHolder.maskIv)
                playFadeInCheckAnimation(mediaViewHolder.checkTv)
            }
        } else {
            mediaViewHolder.checkTv.text = ""
            if (hasLimit) {
                mediaViewHolder.maskIv.setBackgroundColor(Color.parseColor("#80FFFFFF"))
                mediaViewHolder.maskIv.visibility = View.VISIBLE
            } else {
                mediaViewHolder.maskIv.visibility = View.GONE
            }
        }
    }


    private fun playFadeInCheckAnimation(view: View) {
        view.clearAnimation()
        maskShowAnimation = AnimationUtils.loadAnimation(context, R.anim.picker_mask_show)
        view.startAnimation(maskShowAnimation)
    }

    class MediaViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var photoIv: ImageView = contentView.findViewById(R.id.photo_iv)
        var checkTv: TextView = contentView.findViewById(R.id.check_tv)
        var checkLayer: ViewGroup = contentView.findViewById(R.id.check_tv_layer)
        var maskIv: ImageView = contentView.findViewById(R.id.photo_mask_iv)
        var videoLayer: ViewGroup = contentView.findViewById(R.id.video_layer)
        var videoDurationTv: TextView = contentView.findViewById(R.id.video_duration_tv)
        var gifMarkIv: ImageView = contentView.findViewById(R.id.gif_mark_iv)

    }

}