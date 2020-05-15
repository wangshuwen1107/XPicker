package cn.cheney.xpicker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.cheney.lib_picker.camera.XCameraActivity
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
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .onGranted {
                startActivity(Intent(this@MainActivity, XCameraActivity::class.java))
            }
            .onDenied {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show()
            }.start()

    }
}
