package cn.cheney.lib_picker.picker

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.XPickerConstant
import cn.cheney.lib_picker.adapter.GridSpacingItemDecoration
import cn.cheney.lib_picker.adapter.PhotoAdapter
import cn.cheney.lib_picker.entity.MediaFolder
import cn.cheney.lib_picker.util.Logger
import cn.cheney.lib_picker.util.toPx
import kotlinx.android.synthetic.main.xpicker_activity_picker.*

class PickerActivity : AppCompatActivity() {

    private var maxNum = 0

    private val photoAdapter by lazy {
        PhotoAdapter()
    }

    private lateinit var mediaLoader: MediaLoader
    private lateinit var animationRotateShow: Animation
    private lateinit var animationRotateHide: Animation
    var folderListPop: FolderListPop? = null

    private var folderList: List<MediaFolder>? = null
    private var currentChooseFolderName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_picker)
        animationRotateShow = AnimationUtils.loadAnimation(this, R.anim.xpicker_folder_arrow_show)
        animationRotateHide = AnimationUtils.loadAnimation(this, R.anim.xpicker_folder_arrow_hide)
        initView()
        initListener()
        mediaLoader = MediaLoader(this, XPickerConstant.TYPE_IMAGE, true)
        mediaLoader.loadAllMedia { newMediaFolderList ->
            if (newMediaFolderList.isNullOrEmpty()) {
                return@loadAllMedia
            }
            Logger.i("loadAllMedia  size = ${newMediaFolderList.size}")
            picker_dir_layer.visibility = View.VISIBLE
            //将缓存数据注入
            updateNewFolderListByCache(newMediaFolderList)
            folderList = newMediaFolderList
            if (!TextUtils.isEmpty(currentChooseFolderName)) {
                chooseFolder(currentChooseFolderName!!)
            } else {
                chooseFolder(newMediaFolderList[0].name)
            }
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
            if (folderList.isNullOrEmpty()) {
                return@setOnClickListener
            }
            picker_arrow_down_iv.startAnimation(animationRotateShow)
            folderListPop = FolderListPop(this, folderList!!, currentChooseFolderName!!)
            folderListPop!!.showAsDropDown(title_layer)
            folderListPop!!.folderDismissListener = { folderName ->
                picker_arrow_down_iv.startAnimation(animationRotateHide)
                chooseFolder(folderName)
            }
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

    private fun chooseFolder(name: String) {
        val chooseFolderList = folderList?.filter {
            it.name == name
        }
        if (!chooseFolderList.isNullOrEmpty()) {
            val chooseFolder = chooseFolderList[0]
            currentChooseFolderName = chooseFolder.name
            picker_photo_dir_name_tv.text = chooseFolder.name
            photoAdapter.mediaList = chooseFolder.mediaList
        }
    }

    private fun updateNewFolderListByCache(newMediaFolderList: List<MediaFolder>) {
        if (folderList.isNullOrEmpty()) {
            return
        }
        folderList!!.forEach { cacheMediaFolder ->
            val targetFolders = newMediaFolderList.filter {
                it.name == cacheMediaFolder.name
            }
            if (targetFolders.isNullOrEmpty()
                || targetFolders[0].mediaList.isNullOrEmpty()
                || cacheMediaFolder.mediaList.isNullOrEmpty()
            ) {
                return@forEach
            }
            cacheMediaFolder.mediaList.forEach { cacheMediaEntity ->
                for (newMedia in targetFolders[0].mediaList) {
                    if (newMedia.localPath == cacheMediaEntity.localPath
                    ) {
                        Logger.i(
                            "Found new MediaEntity localPath=" +
                                    "${cacheMediaEntity.localPath}"
                        )
                        newMedia.selectedNum = cacheMediaEntity.selectedNum
                        newMedia.selected = cacheMediaEntity.selected
                        break
                    }
                }
            }
        }
    }

}