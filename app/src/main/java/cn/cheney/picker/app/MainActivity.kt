package cn.cheney.picker.app

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.XPickerRequest
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.entity.MediaEntity
import com.yanzhenjie.permission.AndPermission
import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_camera.setOnClickListener {
            action(0)
        }
        start_picker.setOnClickListener {
            action(1)
        }
    }

    private fun action(action: Int) {
        AndPermission.with(this)
            .runtime()
            .permission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .onGranted {
                when (action) {
                    0 -> {
                        startCamera()
                    }
                    1 -> {
                        startPicker()
                    }
                }
            }
            .onDenied {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show()
            }.start()

    }

    private fun startPicker() {
        XPickerRequest().apply {
            actionType = XPickerConstant.PICKER
            browseType = XPickerConstant.TYPE_ALL
            supportGif = true
            haveCameraItem = true
            maxPickerNum = 5
            start(this@MainActivity, mediaSelectedCallback = object : SelectedCallback {
                override fun onSelected(mediaList: List<MediaEntity>?) {
                    var result = ""
                    mediaList?.forEach {
                        result += "localPath=${it.localPath} \n localCompressPath =${it.compressLocalPath} \n"
                    }
                    content_tv.text = result
                }

            })
        }
    }

    private fun startCamera() {
        XPickerRequest().apply {
            captureMode = XPickerConstant.MIXED
            maxRecordTime = 5000
            minRecordTime = 2000
            start(this@MainActivity, object : CameraSaveCallback {
                override fun onTakePhotoSuccess(photoUri: Uri) {
                    content_tv.text = "TakePhoto uri=$photoUri"
                }

                override fun onTakePhotoFailed(errorCode: String) {
                    content_tv.text = "TakePhoto  errorCode=$errorCode"
                }

                override fun onVideoSuccess(coverUri: Uri?, videoUri: Uri, duration: Int?) {
                    content_tv.text = "Video \n" +
                            " coverUrl=$coverUri \n " +
                            " videoUri=$videoUri \n" +
                            " duration=$duration"
                }

                override fun onVideoFailed(errorCode: String) {
                    content_tv.text = "Video errorCode=$errorCode"
                }

            })
        }
    }
}
