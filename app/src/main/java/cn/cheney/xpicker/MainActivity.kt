package cn.cheney.xpicker

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.XPickerConstant
import cn.cheney.lib_picker.XPickerRequest
import cn.cheney.lib_picker.callback.CameraSaveCallback
import com.yanzhenjie.permission.AndPermission
import kotlinx.android.synthetic.main.activity_main.*

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
            start(this@MainActivity)
        }
    }

    private fun startCamera() {
        XPickerRequest().apply {
            captureMode = XPickerConstant.MIXED
            maxRecordTime = 5000
            minRecordTime = 2000
            start(this@MainActivity, object : CameraSaveCallback {
                override fun onTakePhotoSuccess(photoUri: Uri) {
                    Log.i(XPicker.TAG, "onTakePhotoSuccess uri=$photoUri")
                }

                override fun onTakePhotoFailed(errorCode: String) {
                    Log.e(XPicker.TAG, "onTakePhotoFailed errorCode=$errorCode")
                }

                override fun onVideoSuccess(coverUri: Uri?, videoUri: Uri, duration: Int?) {
                    Log.i(
                        XPicker.TAG,
                        "onVideoSuccess coverUrl=$coverUri ,videoUri=$videoUri ,duration=$duration"
                    )
                }

                override fun onVideoFailed(errorCode: String) {
                    Log.e(XPicker.TAG, "onVideoFailed errorCode=$errorCode")
                }

            })
        }
    }
}
