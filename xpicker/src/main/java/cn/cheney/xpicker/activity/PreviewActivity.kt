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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager.widget.ViewPager
import cn.cheney.xpicker.R
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.XPickerConstant.Companion.PREVIEW_DATA_KEY
import cn.cheney.xpicker.XPickerConstant.Companion.PREVIEW_INDEX_KEY
import cn.cheney.xpicker.adapter.PreviewPageAdapter
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.view.photoview.PhotoView
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.android.synthetic.main.xpicker_activity_preview.*
import java.io.File

@SuppressLint("SetTextI18n")
class PreviewActivity : AppCompatActivity() {

    private var previewMediaList: List<MediaEntity>? = null

    private var index: Int = 0

    private lateinit var mediaAdapter: PreviewPageAdapter

    private var showStatusBar = true

    private lateinit var mImmersionBar: ImmersionBar
    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_preview)
        previewMediaList = intent.getParcelableArrayListExtra(PREVIEW_DATA_KEY)
        index = intent.getIntExtra(PREVIEW_INDEX_KEY, 0)
        if (previewMediaList.isNullOrEmpty()) {
            finish()
            return
        }
        initView()
        initListener()
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
            photoView.setOnPhotoTapListener { _, _, _ ->
                showStatusBar = !showStatusBar
                preview_title_bar.visibility = if (showStatusBar) View.VISIBLE else View.GONE
                preview_bottom_bar.visibility = if (showStatusBar) View.VISIBLE else View.GONE
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
        updateSelect(previewMediaList!![index].selected)
    }


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


    @SuppressLint("SetTextI18n")
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
                updateSelect(previewMediaList!![index].selected)
            }

        })
        preview_select_layer.setOnClickListener {
            previewMediaList!![index].selected = !previewMediaList!![index].selected
            updateSelect(previewMediaList!![index].selected)
        }

    }


    private fun updateSelect(select: Boolean) {
        preview_select_iv.isSelected = select
        if (select) {
            preview_select_iv.setImageResource(R.drawable.preview_selected)
        } else {
            preview_select_iv.setImageDrawable(null)
        }
        updateSelectNum()
    }

    private fun updateSelectNum() {
        val selectedList = previewMediaList!!.filter {
            it.selected
        }
        if (selectedList.isNullOrEmpty()) {
            preview_done_tv.isEnabled = false
            preview_done_tv.text = getString(R.string.picker_done)
            preview_done_tv.setTextColor(Color.parseColor("#C8C7C7"))
        } else {
            preview_done_tv.isEnabled = true
            preview_done_tv.text = "${getString(R.string.picker_done)} (${selectedList.size})"
            preview_done_tv.setTextColor(Color.WHITE)
        }
    }

    private fun setStatusBarVis(show: Boolean) {
        if (show) {
            mImmersionBar.hideBar(BarHide.FLAG_SHOW_BAR).init()
        } else {
            mImmersionBar.hideBar(BarHide.FLAG_HIDE_STATUS_BAR).init()
        }
    }

}