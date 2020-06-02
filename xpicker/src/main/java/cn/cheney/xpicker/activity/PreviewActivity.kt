package cn.cheney.xpicker.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.XPickerConstant.Companion.PREVIEW_CURRENT_MAX_NUM_KEY
import cn.cheney.xpicker.XPickerConstant.Companion.PREVIEW_DATA_KEY
import cn.cheney.xpicker.XPickerConstant.Companion.PREVIEW_INDEX_KEY
import cn.cheney.xpicker.adapter.PreviewPageAdapter
import cn.cheney.xpicker.adapter.PreviewSelectAdapter
import cn.cheney.xpicker.callback.PreviewSelectedCallback
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.util.Logger
import cn.cheney.xpicker.view.photoview.PhotoView
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.android.synthetic.main.xpicker_activity_preview.*
import java.io.File

@SuppressLint("SetTextI18n")
class PreviewActivity : AppCompatActivity() {

    private var previewMediaList: List<MediaEntity>? = null
    private var selectList: ArrayList<MediaEntity> = arrayListOf()
    private var index: Int = 0
    private var maxPickerNum: Int = 0
    private var currentSelectMaxNum: Int = 0

    private lateinit var mediaAdapter: PreviewPageAdapter

    private val selectAdapter: PreviewSelectAdapter = PreviewSelectAdapter()

    private var showStatusBar = true

    private lateinit var mImmersionBar: ImmersionBar
    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_preview)
        previewMediaList = intent.getParcelableArrayListExtra(PREVIEW_DATA_KEY)
        maxPickerNum = intent.getIntExtra(PREVIEW_CURRENT_MAX_NUM_KEY, 0)
        index = intent.getIntExtra(PREVIEW_INDEX_KEY, 0)
        if (previewMediaList.isNullOrEmpty()) {
            finish()
            return
        }
        updateSelectedListData()
        val maxNumEntity = selectList.maxBy {
            it.selectedNum
        }
        currentSelectMaxNum = maxNumEntity?.selectedNum ?: 0
        Logger.d("MaxPickerChooseNum = $maxPickerNum; currentSelectMaxNum=$currentSelectMaxNum")
        initView()
        initListener()
    }

    private fun updateSelectedListData() {
        this.selectList.clear()
        this.selectList.addAll(previewMediaList!!.filter {
            it.selected
        }.sortedBy {
            it.selectedNum
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        callback(false)
    }

    private fun initView() {
        mImmersionBar = ImmersionBar.with(this)
            .statusBarColor(R.color.XPicker_gray_main)
            .titleBarMarginTop(preview_title_bar)
        mImmersionBar.init()

        animationIn = AnimationUtils.loadAnimation(this, R.anim.picker_pop_in)
        animationOut = AnimationUtils.loadAnimation(this, R.anim.picker_pop_out)

        val viewList = arrayListOf<View>()
        previewMediaList!!.forEach { mediaEntity ->
            val rootView = View.inflate(this, R.layout.xpicker_item_preview, null)
            val photoView = rootView.findViewById<PhotoView>(R.id.preview_photoView)
            val playIv = rootView.findViewById<ImageView>(R.id.preview_play)
            XPicker.imageLoadListener?.invoke(
                Uri.fromFile(File(mediaEntity.localPath!!)),
                photoView
            )
            if (mediaEntity.fileType == XPickerConstant.TYPE_VIDEO) {
                playIv.visibility = View.VISIBLE
                playIv.setOnClickListener {
                    playVideo(mediaEntity)
                }
            } else {
                playIv.visibility = View.GONE
            }
            viewList.add(rootView)
            photoView.setOnClickListener {
                showStatusBar = !showStatusBar
                preview_bottom_bar.visibility = if (showStatusBar) View.VISIBLE else View.GONE
                if (showStatusBar) {
                    preview_title_bar.visibility = View.VISIBLE
                    preview_bottom_bar.visibility = View.VISIBLE
                    if (selectList.isNullOrEmpty()) {
                        preview_select_rv.visibility = View.GONE
                    } else {
                        preview_select_rv.visibility = View.VISIBLE
                    }
                } else {
                    preview_title_bar.visibility = View.GONE
                    preview_bottom_bar.visibility = View.GONE
                    preview_select_rv.visibility = View.GONE
                }
                if (showStatusBar) {
                    setStatusBarVis(showStatusBar)
                    preview_title_bar.startAnimation(animationIn)
                    animationIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {

                        }

                        override fun onAnimationStart(animation: Animation?) {
                        }

                    })
                } else {
                    preview_title_bar.startAnimation(animationOut)
                    animationOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            setStatusBarVis(showStatusBar)
                        }

                        override fun onAnimationStart(animation: Animation?) {
                        }

                    })
                }
            }
        }
        mediaAdapter = PreviewPageAdapter(viewList)
        preview_vp.adapter = mediaAdapter
        preview_vp.setCurrentItem(index, false)
        //预览数字 1/xx
        preview_num_tv.text = "${index + 1}/${previewMediaList!!.size}"
        //选择状态
        updateSelectBtn(previewMediaList!![index].selected)
        //底部选择预览
        preview_select_rv.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.HORIZONTAL, false
        )
        preview_select_rv.adapter = selectAdapter
    }


    private fun initListener() {
        preview_vp.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                index = position
                preview_num_tv.text = "${index + 1}/${previewMediaList!!.size}"
                updateSelectBtn(previewMediaList!![index].selected)
            }

        })
        preview_select_layer.setOnClickListener {
            val mediaEntity = previewMediaList!![index]
            if (!mediaEntity.selected && currentSelectMaxNum >= maxPickerNum) {
                Toast.makeText(
                    this,
                    getString(R.string.picker_preview_max_limit, maxPickerNum),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@setOnClickListener
            }
            mediaEntity.selected = !mediaEntity.selected
            if (mediaEntity.selected) {
                mediaEntity.selectedNum = ++currentSelectMaxNum
            } else {
                if (mediaEntity.selectedNum < currentSelectMaxNum) {
                    autoDownSomeItem(mediaEntity.selectedNum)
                }
                currentSelectMaxNum--
                mediaEntity.selectedNum = 0
            }
            //更新选择列表
            updateSelectedListData()
            //更新视图
            updateSelectBtn(previewMediaList!![index].selected)
        }
        selectAdapter.itemClickListener = { id ->
            index = previewMediaList!!.indexOf(id)
            preview_vp.setCurrentItem(index, true)
            preview_num_tv.text = "${index + 1}/${previewMediaList!!.size}"
            updateSelectBtn(previewMediaList!![index].selected)
        }
        preview_back_iv.setOnClickListener {
            callback(false)
        }
        preview_done_tv.setOnClickListener {
            callback(true)
        }
    }

    /**
     * 查找比目标值小的item 减1
     * @param target 反选的ItemNum
     */
    private fun autoDownSomeItem(target: Int) {
        for ((index, mediaEntity) in selectList.withIndex()) {
            if (mediaEntity.selectedNum in (target) until (currentSelectMaxNum + 1)
                && mediaEntity.selected
            ) {
                mediaEntity.selectedNum = mediaEntity.selectedNum - 1
            }
        }
    }

    /**
     * 调用系统播放器播放video
     */
    private fun playVideo(mediaEntity: MediaEntity) {
        if (TextUtils.isEmpty(mediaEntity.localPath)) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(mediaEntity.localPath!!)
        val uri: Uri
        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, "cn.cheney.xpicker.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "video/*")
        startActivity(intent)
    }


    /**
     * 更新选择栏列表
     */
    private fun updateSelectList() {
        if (selectList.isNullOrEmpty()) {
            preview_select_rv.visibility = View.GONE
        } else {
            preview_select_rv.visibility = View.VISIBLE
        }
        selectAdapter.currentPreviewId = previewMediaList!![index].localPath
        selectAdapter.selectList = selectList
    }

    /**
     * 更新左下角选择按钮
     */
    private fun updateSelectBtn(select: Boolean) {
        preview_select_iv.isSelected = select
        if (select) {
            preview_select_iv.setImageResource(R.drawable.preview_selected)
        } else {
            preview_select_iv.setImageDrawable(null)
        }
        updateSelectNum()
        updateSelectList()
    }


    /**
     * 更新完成按钮
     */
    private fun updateSelectNum() {
        if (selectList.isNullOrEmpty()) {
            preview_done_tv.isEnabled = false
            preview_done_tv.text = getString(R.string.picker_done)
            preview_done_tv.setTextColor(Color.parseColor("#C8C7C7"))
        } else {
            preview_done_tv.isEnabled = true
            preview_done_tv.text = "${getString(R.string.picker_done)} (${selectList.size})"
            preview_done_tv.setTextColor(Color.WHITE)
        }
        selectAdapter.selectList = selectList
    }

    /**
     * 状态栏显示隐藏
     */
    private fun setStatusBarVis(show: Boolean) {
        if (show) {
            mImmersionBar.hideBar(BarHide.FLAG_SHOW_BAR).init()
        } else {
            mImmersionBar.hideBar(BarHide.FLAG_HIDE_STATUS_BAR).init()
        }
    }


    private fun callback(done: Boolean = false) {
        if (!selectList.isNullOrEmpty()) {
            selectList.sortBy {
                it.selectedNum
            }
        }
        if (done) {
            selectedCallback?.onSelected(selectList)
        } else {
            selectedCallback?.onCancel(previewMediaList!!)
        }
        selectedCallback = null
        finish()
    }

    companion object {
        var selectedCallback: PreviewSelectedCallback? = null
    }

}