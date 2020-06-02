package cn.cheney.xpicker.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.XPickerRequest
import cn.cheney.xpicker.adapter.GridSpacingItemDecoration
import cn.cheney.xpicker.adapter.PhotoAdapter
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.PreviewSelectedCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.core.MediaLoader
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.entity.MediaFolder
import cn.cheney.xpicker.util.Logger
import cn.cheney.xpicker.util.toPx
import cn.cheney.xpicker.view.FolderListPop
import kotlinx.android.synthetic.main.xpicker_activity_picker.*
import kotlin.concurrent.thread

class PickerActivity : AppCompatActivity() {

    private val photoAdapter by lazy {
        PhotoAdapter(this)
    }

    private var xPickerRequest: XPickerRequest? = null
    private lateinit var mediaLoader: MediaLoader
    private lateinit var animationRotateShow: Animation
    private lateinit var animationRotateHide: Animation
    private var folderListPop: FolderListPop? = null

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
    private var chooseMediaList: ArrayList<MediaEntity> = arrayListOf()
    /**
     * 当前显示文件夹
     */
    private var currentFolder: MediaFolder? = null
    /**
     * 标记是否进入预览界面
     */
    private var ignoreUpdate = false
    /**
     * 当前最大的选择数字
     */
    private var maxNum = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_picker)
        xPickerRequest = intent.getParcelableExtra(XPickerConstant.REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        animationRotateShow = AnimationUtils.loadAnimation(this, R.anim.picker_folder_arrow_show)
        animationRotateHide = AnimationUtils.loadAnimation(this, R.anim.picker_folder_arrow_hide)
        initView()
        initListener()

        mediaLoader = MediaLoader(
            this,
            xPickerRequest!!.browseType,
            xPickerRequest!!.supportGif
        )
        loadData()
    }


    private fun loadData() {
        mediaLoader.loadAllMedia(object :
            MediaLoader.LocalMediaLoadListener {
            override fun loadComplete(folders: List<MediaFolder>?) {
                if (folders.isNullOrEmpty()) {
                    return
                }
                if (ignoreUpdate) {
                    ignoreUpdate = false
                    return
                }
                Logger.i("loadAllMedia  size = ${folders[0].mediaList.size}")
                picker_dir_layer.visibility = View.VISIBLE
                thread {
                    //1.清空选择List
                    chooseMediaList.clear()
                    //2.更新缓存
                    updateNewFolderListByCache(folders)
                    //3.更新最新List
                    folderList = folders
                    if (!TextUtils.isEmpty(currentChooseFolderName)) {
                        chooseFolder(currentChooseFolderName!!)
                    } else {
                        chooseFolder(folders[0].name)
                    }
                }
            }

        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        callback(true)
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
        photoAdapter.haveCamera = xPickerRequest?.haveCameraItem ?: false
    }

    private fun initListener() {
        photoAdapter.itemCheckListener = { position, mediaEntity, holder ->
            if (mediaEntity.selected || maxNum < xPickerRequest!!.maxPickerNum) {
                mediaEntity.selected = !mediaEntity.selected
                if (mediaEntity.selected) {
                    mediaEntity.selectedNum = ++maxNum
                } else {
                    if (mediaEntity.selectedNum < maxNum) {
                        autoDownSomeItem(mediaEntity.selectedNum)
                    }
                    maxNum--
                    mediaEntity.selectedNum = 0
                }
                addToChooseList(mediaEntity, mediaEntity.selected)
                photoAdapter.updateItemCheck(position, true)
            }
        }

        photoAdapter.itemClickListener = { position, isCamera ->
            if (isCamera) {
                xPickerRequest!!.actionType = XPickerConstant.CAMERA
                xPickerRequest!!.start(
                    this@PickerActivity,
                    cameraSaveCallback = object : CameraSaveCallback {
                        override fun onTakePhotoSuccess(photoUri: Uri) {
                            loadData()
                        }

                        override fun onTakePhotoFailed(errorCode: String) {
                        }

                        override fun onVideoSuccess(coverUri: Uri?, videoUri: Uri, duration: Int?) {
                            loadData()
                        }

                        override fun onVideoFailed(errorCode: String) {
                        }

                    })
            } else {
                if (!currentFolder?.mediaList.isNullOrEmpty()) {
                    goToPreview(currentFolder!!.mediaList, position)
                }
            }
        }

        picker_dir_layer.setOnClickListener {
            if (folderList.isNullOrEmpty()) {
                return@setOnClickListener
            }
            picker_arrow_down_iv.startAnimation(animationRotateShow)
            folderListPop = FolderListPop(
                this,
                folderList!!,
                currentChooseFolderName!!
            )
            folderListPop!!.showAsDropDown(title_layer)
            folderListPop!!.folderDismissListener = { folderName ->
                picker_arrow_down_iv.startAnimation(animationRotateHide)
                chooseFolder(folderName)
            }
        }
        picker_back_iv.setOnClickListener {
            callback(true)
        }
        picker_done_tv.setOnClickListener {
            callback(false)
        }
        picker_preview_tv.setOnClickListener {
            goToPreview(chooseMediaList)
        }
    }


    private fun goToPreview(mediaList: ArrayList<MediaEntity>, index: Int = 0) {
        ignoreUpdate = true
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra(XPickerConstant.PREVIEW_INDEX_KEY, index)
            putParcelableArrayListExtra(XPickerConstant.PREVIEW_DATA_KEY, mediaList)
            putExtra(XPickerConstant.PREVIEW_CURRENT_MAX_NUM_KEY, xPickerRequest!!.maxPickerNum)
        }
        startActivity(intent)
        PreviewActivity.selectedCallback = object : PreviewSelectedCallback {
            override fun onSelected(mediaList: List<MediaEntity>?) {
                callback(false, assign = true, assignList = mediaList)
            }

            override fun onCancel(mediaList: List<MediaEntity>) {
                updateMediaEntity(mediaList)
            }
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

                picker_preview_tv.text = getString(R.string.picker_preview)
                picker_preview_tv.setTextColor(Color.parseColor("#C8C7C7"))
            } else {
                picker_done_tv.isEnabled = true
                picker_done_tv.text = "${getString(R.string.picker_done)} (${chooseMediaList.size})"
                picker_done_tv.setTextColor(Color.WHITE)

                picker_preview_tv.text =
                    "${getString(R.string.picker_preview)} (${chooseMediaList.size})"
                picker_preview_tv.setTextColor(Color.WHITE)
            }
        }
    }

    /**
     * 查找比目标值小的item 减1
     * @param target 反选的ItemNum
     */
    private fun autoDownSomeItem(target: Int) {
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


    /**
     * 选择文件夹
     * @param name 文件夹名字
     */
    private fun chooseFolder(name: String) {
        val chooseFolderList = folderList?.filter {
            it.name == name
        }
        if (!chooseFolderList.isNullOrEmpty()) {
            val chooseFolder = chooseFolderList[0]
            currentFolder = chooseFolder
            currentChooseFolderName = chooseFolder.name
            runOnUiThread {
                picker_photo_dir_name_tv.text = chooseFolder.name
                photoAdapter.mediaList = chooseFolder.mediaList
            }
        }
    }

    /**
     * 更新缓存里面的信息
     * @param newMediaFolderList 新扫描出来的文件夹列表
     */
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
                        newMedia.selectedNum = cacheMediaEntity.selectedNum
                        newMedia.selected = cacheMediaEntity.selected
                        addToChooseList(newMedia, newMedia.selected)
                        break
                    }
                }
            }
        }
    }


    private fun updateMediaEntity(mediaList: List<MediaEntity>?) {
        if (mediaList.isNullOrEmpty()) {
            return
        }
        currentFolder?.mediaList?.forEach { cacheMediaEntity ->
            mediaList.forEach newList@{ newEntity ->
                if (newEntity.localPath == cacheMediaEntity.localPath) {
                    cacheMediaEntity.selected = newEntity.selected
                    cacheMediaEntity.selectedNum = newEntity.selectedNum
                    val index = currentFolder?.mediaList?.indexOf(cacheMediaEntity)
                    if (null != index && index >= 0) {
                        photoAdapter.updateItemCheck(index)
                        addToChooseList(cacheMediaEntity, cacheMediaEntity.selected)
                    }
                    return@newList
                }
            }
        }
        //更新当前最大的选择数量
        maxNum = currentFolder?.mediaList?.maxBy {
            it.selectedNum
        }?.selectedNum ?: 0
    }


    private fun callback(
        cancel: Boolean = false,
        assign: Boolean = false,
        assignList: List<MediaEntity>? = null
    ) {
        if (!cancel) {
            if (assign) {
                mediaSelectedCallback?.onSelected(assignList)
            } else if (!chooseMediaList.isNullOrEmpty()) {
                chooseMediaList.sortBy {
                    it.selectedNum
                }
                mediaSelectedCallback?.onSelected(chooseMediaList)
            }
        }
        mediaSelectedCallback = null
        finish()
    }

    companion object {
        var mediaSelectedCallback: SelectedCallback? = null
    }
}