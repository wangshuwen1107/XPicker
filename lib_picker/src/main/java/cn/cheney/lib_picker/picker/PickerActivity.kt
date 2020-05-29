package cn.cheney.lib_picker.picker

import android.annotation.SuppressLint
import android.graphics.Color
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
import cn.cheney.lib_picker.XPickerRequest
import cn.cheney.lib_picker.adapter.GridSpacingItemDecoration
import cn.cheney.lib_picker.adapter.PhotoAdapter
import cn.cheney.lib_picker.entity.MediaEntity
import cn.cheney.lib_picker.entity.MediaFolder
import cn.cheney.lib_picker.util.Logger
import cn.cheney.lib_picker.util.toPx
import kotlinx.android.synthetic.main.xpicker_activity_picker.*

class PickerActivity : AppCompatActivity() {

    private var maxNum = 0

    private val photoAdapter by lazy {
        PhotoAdapter()
    }

    private var xPickerRequest: XPickerRequest? = null
    private lateinit var mediaLoader: MediaLoader
    private lateinit var animationRotateShow: Animation
    private lateinit var animationRotateHide: Animation
    var folderListPop: FolderListPop? = null

    /**
     * 全部文件夹集合
     */
    private var folderList: List<MediaFolder>? = null
    /**
     * 当前文件夹名称
     */
    private var currentChooseFolderName: String? = null
    /**
     * 选择的文件集合
     */
    private var chooseMediaList: MutableList<MediaEntity> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_picker)
        xPickerRequest = intent.getParcelableExtra(XPickerConstant.REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        animationRotateShow = AnimationUtils.loadAnimation(this, R.anim.xpicker_folder_arrow_show)
        animationRotateHide = AnimationUtils.loadAnimation(this, R.anim.xpicker_folder_arrow_hide)
        initView()
        initListener()
        mediaLoader = MediaLoader(this, xPickerRequest!!.browseType, xPickerRequest!!.supportGif)
        mediaLoader.loadAllMedia { newMediaFolderList ->
            if (newMediaFolderList.isNullOrEmpty()) {
                return@loadAllMedia
            }
            Logger.i("loadAllMedia  size = ${newMediaFolderList.size}")
            picker_dir_layer.visibility = View.VISIBLE
            //1.清空选择List
            chooseMediaList.clear()
            //2.更新缓存
            updateNewFolderListByCache(newMediaFolderList)
            //更新最新List
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
        photoAdapter.itemCheckListener = { position, mediaEntity, holder ->
            if (mediaEntity.selected || maxNum < xPickerRequest!!.maxPickerNum) {
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
                addToChooseList(mediaEntity, mediaEntity.selected)
                photoAdapter.updateItemCheck(position)
            }
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
        picker_back_iv.setOnClickListener {
            finish()
        }
    }

    /**
     * 选择/反选 mediaEntity
     */
    @SuppressLint("SetTextI18n")
    private fun addToChooseList(mediaEntity: MediaEntity, isChoose: Boolean) {
        //增加
        if (!chooseMediaList.any {
                it.localPath == mediaEntity.localPath
            } && isChoose) {
            chooseMediaList.add(mediaEntity)
        }
        //删除
        if (!isChoose) {
            val filterList = chooseMediaList.filter {
                it.localPath == mediaEntity.localPath
            }
            if (!filterList.isNullOrEmpty()) {
                chooseMediaList.remove(filterList[0])
            }
        }
        runOnUiThread {
            photoAdapter.hasLimit = chooseMediaList.size >= xPickerRequest!!.maxPickerNum
            if (chooseMediaList.isEmpty()) {
                picker_done_tv.isEnabled = false
                picker_done_tv.text = getString(R.string.picker_done)
                picker_done_tv.setTextColor(Color.parseColor("#C8C7C7"))

                picker_preview_tv.text= getString(R.string.picker_preview)
                picker_preview_tv.setTextColor(Color.parseColor("#C8C7C7"))
            } else {
                picker_done_tv.isEnabled = true
                picker_done_tv.text = "${getString(R.string.picker_done)} (${chooseMediaList.size})"
                picker_done_tv.setTextColor(Color.WHITE)

                picker_preview_tv.text="${getString(R.string.picker_preview)} (${chooseMediaList.size})"
                picker_preview_tv.setTextColor(Color.WHITE)
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
                        addToChooseList(newMedia, newMedia.selected)
                        break
                    }
                }
            }
        }
    }

}