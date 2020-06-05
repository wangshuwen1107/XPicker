package cn.cheney.xpicker.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.StringDef
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.adapter.GridSpacingItemDecoration
import cn.cheney.xpicker.adapter.PhotoAdapter
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.CropCallback
import cn.cheney.xpicker.callback.PreviewSelectedCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.core.MediaLoader
import cn.cheney.xpicker.core.MediaPhotoCompress
import cn.cheney.xpicker.entity.*
import cn.cheney.xpicker.util.Logger
import cn.cheney.xpicker.util.getPrefix
import cn.cheney.xpicker.util.toPx
import cn.cheney.xpicker.view.FolderListPop
import cn.cheney.xpicker.view.LoadingDialog
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.xpicker_activity_picker.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class PickerActivity : AppCompatActivity() {

    private val photoAdapter by lazy {
        PhotoAdapter(this)
    }

    private val loadingDialog: LoadingDialog by lazy {
        LoadingDialog(this).apply {
            setCancelable(false)
        }
    }

    private var xPickerRequest: PickerRequest? = null
    private lateinit var mediaLoader: MediaLoader
    private lateinit var animationRotateShow: Animation
    private lateinit var animationRotateHide: Animation
    private var folderListPop: FolderListPop? = null
    private var cropEntity: MediaEntity? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * The of media folder
     */
    private var folderList: List<MediaFolder>? = null

    /**
     * The current media folder name
     */
    private var currentChooseFolderName: String? = null

    /**
     * The current folder media list
     */
    private var chooseMediaList: ArrayList<MediaEntity> = arrayListOf()

    /**
     * The current media folder
     */
    private var currentFolder: MediaFolder? = null

    /**
     * Whether to ignore update tag
     */
    private var ignoreUpdate = false

    /**
     * The current max select Num
     */
    private var maxNum = 0

    /**
     * Whether selected original of tags
     */
    private var isOriginal = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_picker)
        val bundle = intent.getBundleExtra(XPickerConstant.REQUEST_BUNDLE_KEY)
        xPickerRequest = bundle?.getParcelable(XPickerConstant.REQUEST_KEY)
        if (null == xPickerRequest) {
            finish()
            return
        }
        animationRotateShow = AnimationUtils.loadAnimation(this, R.anim.picker_folder_arrow_show)
        animationRotateHide = AnimationUtils.loadAnimation(this, R.anim.picker_folder_arrow_hide)
        initView()
        initListener()
        mediaLoader = MediaLoader(this, xPickerRequest!!.mineType)
        loadData()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        callback(true)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri: Uri? = UCrop.getOutput(data!!)
            cropEntity?.cropPath = resultUri?.path
            cropCallback?.onCrop(cropEntity)
            cropEntity = null
            cropCallback = null
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropCallback?.onCrop(cropEntity)
            cropEntity = null
            cropCallback = null
            finish()
        }
    }

    private fun initView() {
        photoAdapter.haveCheck = xPickerRequest!!.actionType != ActionType.CROP.type
        picker_photo_rv.adapter = photoAdapter
        picker_photo_rv.layoutManager = GridLayoutManager(this, xPickerRequest!!.spanCount)
        picker_photo_rv.addItemDecoration(
            GridSpacingItemDecoration(
                xPickerRequest!!.spanCount,
                2.toPx(),
                true
            )
        )
        photoAdapter.haveCamera = xPickerRequest?.haveCameraItem ?: false
        picker_bottom_layer.visibility =
            if (xPickerRequest!!.actionType == ActionType.CROP.type) View.GONE
            else View.VISIBLE
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
                goToCamera()
            } else {
                if (xPickerRequest!!.actionType == ActionType.CROP.type) {
                    cropEntity = currentFolder!!.mediaList[position]
                    goToCrop(cropEntity!!)
                } else {
                    if (!currentFolder?.mediaList.isNullOrEmpty()) {
                        goToPreview(currentFolder!!.mediaList, position)
                    }
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
        picker_original_check_layer.setOnClickListener {
            isOriginal = !isOriginal
            updateOriginal()
        }
    }


    private fun showEmpty() {
        if (!xPickerRequest!!.haveCameraItem) {
            picker_empty_tv.visibility = View.VISIBLE
            picker_photo_rv.visibility = View.GONE
        }
    }


    private fun loadData() {
        mediaLoader.loadAllMedia(object :
            MediaLoader.LocalMediaLoadListener {
            override fun loadComplete(folders: List<MediaFolder>?) {
                if (folders.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                if (ignoreUpdate) {
                    ignoreUpdate = false
                    return
                }
                @StringDef
                if (!folderList.isNullOrEmpty()
                    && folderList!![0].mediaList.size == folders[0].mediaList.size
                ) {
                    return
                }
                picker_empty_tv.visibility = View.GONE
                picker_photo_rv.visibility = View.VISIBLE
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


    private fun goToCamera() {
        xPickerRequest?.apply {
            XPicker.ofCamera()
                .defaultLensFacing(defaultLensFacing)
                .maxRecordTime(maxRecordTime)
                .minRecordTime(minRecordTime)
                .captureMode(CaptureType.valueOf(captureMode))
                .start(this@PickerActivity, cameraSaveCallback = object : CameraSaveCallback {
                    override fun onTakePhotoSuccess(photoUri: Uri) {
                        handler.postDelayed(Runnable {
                            loadData()
                        }, 300)
                    }

                    override fun onTakePhotoFailed(errorCode: String) {
                    }

                    override fun onVideoSuccess(
                        coverUri: Uri?,
                        videoUri: Uri,
                        duration: Int?
                    ) {
                        handler.postDelayed(Runnable {
                            loadData()
                        }, 300)
                    }

                    override fun onVideoFailed(errorCode: String) {
                    }
                })
        }
    }

    private fun updateOriginal() {
        picker_original_check_iv.isSelected = isOriginal
        if (isOriginal) {
            picker_original_check_iv.setImageResource(R.drawable.preview_selected)
        } else {
            picker_original_check_iv.setImageDrawable(null)
        }
    }


    private fun goToPreview(mediaList: ArrayList<MediaEntity>, index: Int = 0) {
        ignoreUpdate = true
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra(XPickerConstant.PREVIEW_INDEX_KEY, index)
            putParcelableArrayListExtra(XPickerConstant.PREVIEW_DATA_KEY, mediaList)
            putExtra(XPickerConstant.PREVIEW_CURRENT_MAX_NUM_KEY, xPickerRequest!!.maxPickerNum)
            putExtra(XPickerConstant.PREVIEW_ORIGINAL_KEY, isOriginal)
        }
        startActivity(intent)
        PreviewActivity.selectedCallback = object : PreviewSelectedCallback {
            override fun onSelected(mediaList: List<MediaEntity>?, isOrigin: Boolean) {
                callback(false, assign = true, assignList = mediaList)
                this@PickerActivity.isOriginal = isOriginal
                updateOriginal()
            }

            override fun onCancel(mediaList: List<MediaEntity>, isOrigin: Boolean) {
                updateMediaEntity(mediaList)
                this@PickerActivity.isOriginal = isOriginal
                updateOriginal()
            }
        }

    }

    private fun goToCrop(mediaEntity: MediaEntity) {
        val targetFile = File(mediaEntity.localPath!!)
        UCrop.of(
            Uri.fromFile(targetFile),
            Uri.fromFile(getCropDir(this, targetFile.getPrefix()))
        )
            .withAspectRatio(1f, 1f)
            .withOptions(UCrop.Options().apply {
                setCircleDimmedLayer(xPickerRequest!!.circleCrop)
            })
            .start(this)
    }

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
        maxNum = currentFolder?.mediaList?.maxBy {
            it.selectedNum
        }?.selectedNum ?: 0
    }


    private fun callback(
        cancel: Boolean = false,
        assign: Boolean = false,
        assignList: List<MediaEntity>? = null
    ) {
        if (cancel) {
            mediaSelectedCallback = null
            finish()
            return
        }
        if (assign) {
            mediaSelectedCallback?.onSelected(assignList)
            mediaSelectedCallback = null
            finish()
            return
        }
        if (!chooseMediaList.isNullOrEmpty()) {
            chooseMediaList.sortBy {
                it.selectedNum
            }
        }
        if (isOriginal) {
            mediaSelectedCallback?.onSelected(chooseMediaList)
            mediaSelectedCallback = null
            finish()
            return
        }
        loadingDialog.showLoading(getString(R.string.picker_compress_tip))
        MediaPhotoCompress().apply {
            compressImg(this@PickerActivity, chooseMediaList) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    mediaSelectedCallback?.onSelected(chooseMediaList)
                    mediaSelectedCallback = null
                    finish()
                }
            }
        }
    }

    companion object {
        var mediaSelectedCallback: SelectedCallback? = null
        var cropCallback: CropCallback? = null

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"


        private fun getCropDir(context: Context, prefix: String): File {
            val compressDir = File(
                context.externalMediaDirs.first().absolutePath
                        + File.separator + if (TextUtils.isEmpty(prefix)) PHOTO_EXTENSION else prefix
            )
            if (!compressDir.exists()) {
                compressDir.mkdirs()
            }
            return File(
                compressDir, SimpleDateFormat(FILENAME, Locale.CHINA)
                    .format(System.currentTimeMillis()) + PHOTO_EXTENSION
            )
        }
    }

}