package cn.cheney.picker.app

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.xpicker.XPicker
import cn.cheney.xpicker.callback.CameraSaveCallback
import cn.cheney.xpicker.callback.CropCallback
import cn.cheney.xpicker.callback.SelectedCallback
import cn.cheney.xpicker.entity.CaptureType
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.entity.MineType
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
        start_crop.setOnClickListener {
            action(2)
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
                    var result = ""
                    mediaList?.forEach {
                        result += "localPath=${it.localPath} \n localCompressPath =${it.compressLocalPath} \n"
                    }
                    content_tv.text = result
                }
            })
    }

    private fun startCamera() {
        XPicker.ofCamera()
            .captureMode(CaptureType.MIXED)
            .start(this, object : CameraSaveCallback {
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

    private fun startCrop() {
        XPicker.ofCrop()
            .start(this, cropCallback = object : CropCallback {
                override fun onCrop(mediaList: MediaEntity?) {
                    var result = ""
                    result += "localPath=${mediaList?.localPath} \n cropUri =${mediaList?.cropUri} \n"
                    content_tv.text = result
                }

            })
    }

}
