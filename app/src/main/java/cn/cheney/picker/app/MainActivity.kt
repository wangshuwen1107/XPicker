package cn.cheney.picker.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.adapter.GridSpacingItemDecoration
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.CropCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.entity.CaptureType
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.entity.MineType
import cn.cheney.xpicker.util.toPx
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {


    private val demoPhotoAdapter by lazy {
        DemoPhotoAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content_rv.layoutManager = GridLayoutManager(this, 3)
        content_rv.addItemDecoration(
            GridSpacingItemDecoration(
                3,
                4.toPx(),
                true
            )
        )
        content_rv.adapter = demoPhotoAdapter
        start_camera.setOnClickListener {
            action(0)
        }
        start_picker.setOnClickListener {
            action(1)
        }
        start_crop.setOnClickListener {
            action(2)
        }
    }

    private fun action(action: Int) {
        AndPermission.with(this)
            .runtime()
            .permission(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.CAMERA,
                Permission.RECORD_AUDIO
            )
            .onGranted {
                when (action) {
                    0 -> startCamera()
                    1 -> startPicker()
                    2 -> startCrop()
                }
            }
            .onDenied {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show()
            }.start()

    }

    private fun startPicker() {
        XPicker.ofPicker()
            .mineType(MineType.TYPE_ALL)
            .maxPickerNum(3)
            .haveCameraItem(true)
            .start(this, selectedCallback = object : SelectedCallback {
                override fun onSelected(mediaList: List<MediaEntity>?) {
                    content_rv.visibility = View.VISIBLE
                    content_layer.visibility = View.GONE
                    demoPhotoAdapter.mediaList = mediaList
                }
            })
    }

    private fun startCamera() {
        XPicker.ofCamera()
            .captureMode(CaptureType.MIXED)
            .defaultBackCamera(true)
            .start(this, object : CameraSaveCallback {
                override fun onTakePhotoSuccess(photoUri: Uri) {
                    content_rv.visibility = View.GONE
                    content_layer.visibility = View.VISIBLE
                    video_iv.visibility = View.GONE
                    Glide.with(this@MainActivity)
                        .load(photoUri)
                        .into(content_iv)
                }

                override fun onTakePhotoFailed(errorCode: String) {
                }

                override fun onVideoSuccess(coverUri: Uri?, videoUri: Uri, duration: Int?) {
                    content_rv.visibility = View.GONE
                    content_layer.visibility = View.VISIBLE
                    video_iv.visibility = View.VISIBLE
                    Glide.with(this@MainActivity)
                        .load(coverUri)
                        .into(content_iv)
                }

                override fun onVideoFailed(errorCode: String) {
                }

            })
    }

    private fun startCrop() {
        XPicker.ofCrop()
            .circleCrop(true)
            .start(this, cropCallback = object : CropCallback {
                override fun onCrop(mediaEntity: MediaEntity?) {
                    if (null == mediaEntity || TextUtils.isEmpty(mediaEntity.cropPath)) {
                        return
                    }
                    content_rv.visibility = View.GONE
                    content_layer.visibility = View.VISIBLE
                    video_iv.visibility = View.GONE
                    Glide.with(this@MainActivity)
                        .load(File(mediaEntity.cropPath!!))
                        .apply(RequestOptions().circleCrop())
                        .into(content_iv)
                }
            })
    }

}
