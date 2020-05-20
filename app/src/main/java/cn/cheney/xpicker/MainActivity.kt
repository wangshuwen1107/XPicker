package cn.cheney.xpicker

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.lib_picker.CAPTURE_AND_RECORDER
import cn.cheney.lib_picker.XPickerRequest
import com.yanzhenjie.permission.AndPermission
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_camera.setOnClickListener {
            goToCamera()
        }
    }

    private fun goToCamera() {
        AndPermission.with(this)
            .runtime()
            .permission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .onGranted {
                XPickerRequest().apply {
                    cameraType = CAPTURE_AND_RECORDER
                    maxRecordTime = 5000
                    minRecordTime = 2000
                    start(this@MainActivity)
                }
            }
            .onDenied {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show()
            }.start()

    }
}
