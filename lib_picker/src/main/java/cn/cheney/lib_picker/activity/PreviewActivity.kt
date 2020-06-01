package cn.cheney.lib_picker.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPickerConstant.Companion.PREVIEW_DATA_KEY
import cn.cheney.lib_picker.XPickerConstant.Companion.PREVIEW_INDEX_KEY
import cn.cheney.lib_picker.adapter.PreviewPageAdapter
import cn.cheney.lib_picker.entity.MediaEntity
import kotlinx.android.synthetic.main.xpicker_activity_preview.*

class PreviewActivity : AppCompatActivity() {

    private var previewMediaList: List<MediaEntity>? = null

    private var index: Int = 0

    private  lateinit  var mediaAdapter:PreviewPageAdapter

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
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
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
        preview_num_tv.text = "${index + 1}/${previewMediaList!!.size}"

        val viewList = arrayListOf<View>()
        previewMediaList!!.forEach {
            viewList.add()
        }
        mediaAdapter = PreviewPageAdapter()
    }


}