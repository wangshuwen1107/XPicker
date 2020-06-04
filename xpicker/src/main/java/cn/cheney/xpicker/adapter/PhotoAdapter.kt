package cn.cheney.xpicker.adapter

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
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.util.timeParse
import java.io.File

typealias ItemCheckListener = (position: Int, mediaEntity: MediaEntity, holder: ViewHolder) -> Unit
typealias ItemClickListener = (position: Int, isCamera: Boolean) -> Unit

class PhotoAdapter(var context: Context) : RecyclerView.Adapter<ViewHolder>() {

    var itemCheckListener: ItemCheckListener? = null
    var itemClickListener: ItemClickListener? = null

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

    var haveCamera = false

    override fun getItemViewType(position: Int): Int {
        if (position == 0 && haveCamera) {
            return CAMERA_TYPE
        }
        return MEDIA_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == CAMERA_TYPE) {
            CameraViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.xpicker_item_camera_grid, parent, false)
            )
        } else {
            MediaViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.xpicker_item_photo_grid, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return if (mediaList.isNullOrEmpty() && !haveCamera) {
            0
        } else if (haveCamera) {
            return if (mediaList.isNullOrEmpty()) {
                1
            } else {
                mediaList!!.size + 1
            }
        } else {
            return if (mediaList.isNullOrEmpty()) {
                0
            } else {
                mediaList!!.size
            }
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MediaViewHolder) {
            holderMap[position] = holder
            var realPos = position
            if (haveCamera) {
                realPos = position - 1
            }
            val mediaEntity = mediaList!![realPos]
            //底部文件类型图标
            if (mediaEntity.fileType == XPickerConstant.File_TYPE_VIDEO) {
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
                holder.photoIv
            )
            //图片选择
            updateItemCheck(realPos)
            holder.checkLayer.setOnClickListener {
                itemCheckListener?.invoke(realPos, mediaEntity, holder)
            }
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(realPos, false)
            }
        } else if (holder is CameraViewHolder) {
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(0, true)
            }
        }
    }

    fun updateItemCheck(position: Int, byClick: Boolean = false) {
        val mediaViewHolder =
            (holderMap[if (haveCamera) position + 1 else position] as? MediaViewHolder)
        val mediaEntity = mediaList!![position]
        if (null == mediaViewHolder) {
            return
        }
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


    class CameraViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
    }


    companion object {
        const val CAMERA_TYPE = 100
        const val MEDIA_TYPE = 101
    }
}