package cn.cheney.lib_picker.picker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.XPickerConstant
import cn.cheney.lib_picker.adapter.GridSpacingItemDecoration
import cn.cheney.lib_picker.adapter.PhotoAdapter
import cn.cheney.lib_picker.entity.MediaFolder
import cn.cheney.lib_picker.util.toPx
import kotlinx.android.synthetic.main.xpicker_activity_picker.*

class PickerActivity : AppCompatActivity() {

    private var maxNum = 0

    private val photoAdapter by lazy {
        PhotoAdapter()
    }

    private lateinit var mediaLoader: MediaLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_picker)
        initView()
        initListener()
        mediaLoader = MediaLoader(this, XPickerConstant.TYPE_IMAGE, true)
        mediaLoader.loadAllMedia {
            if (null == it || it.isEmpty()) {
                return@loadAllMedia
            }
            setFolderData(it[0])
        }
    }


    private fun initView() {
        picker_photo_rv.adapter = photoAdapter
        picker_photo_rv.layoutManager = GridLayoutManager(this, XPicker.spanCount)
        picker_photo_rv.addItemDecoration(
            GridSpacingItemDecoration(
                XPicker.spanCount,
                2.toPx(),
                true
            )
        )
    }

    private fun initListener() {
        photoAdapter.itemClickListener = { position, mediaEntity, holder ->
            mediaEntity.selected = !mediaEntity.selected
            if (mediaEntity.selected) {
                mediaEntity.selectedNum = ++maxNum
            } else {
                //比selectedNum 小的 全减1
                if (mediaEntity.selectedNum < maxNum) {
                    getDownItem(mediaEntity.selectedNum)
                }
                maxNum--
                mediaEntity.selectedNum = 0
            }
            photoAdapter.updateItemCheck(position)
        }
        picker_dir_layer.setOnClickListener {

        }

    }

    /**
     * 比目标数量小 自动减1
     */
    private fun getDownItem(target: Int) {
        if (null == photoAdapter.mediaList) {
            return
        }
        for ((index, mediaEntity) in photoAdapter.mediaList!!.withIndex()) {
            if (mediaEntity.selectedNum in (target) until (maxNum + 1) && mediaEntity.selected) {
                mediaEntity.selectedNum = mediaEntity.selectedNum - 1
                photoAdapter.updateItemCheck(index)
            }
        }
    }


    private fun setFolderData(mediaFolder: MediaFolder) {
        picker_photo_dir_name_tv.text = mediaFolder.name
        photoAdapter.mediaList = mediaFolder.mediaList
    }


}