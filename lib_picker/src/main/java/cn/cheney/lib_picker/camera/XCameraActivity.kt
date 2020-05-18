package cn.cheney.lib_picker.camera

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.view.CaptureLayer
import kotlinx.android.synthetic.main.xpicker_activity_camera.*

class XCameraActivity : AppCompatActivity() {

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var displayId: Int? = null

    private val cameraEngine: CameraEngine by lazy {
        CameraEngine(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.xpicker_activity_camera)
        xpicker_camera_preview.post {
            displayId = xpicker_camera_preview.display.displayId
            cameraEngine.initAndPreviewCamera(lensFacing, xpicker_camera_preview)
        }
        initListener()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraEngine.release()
    }

    private fun initListener() {
        xpicker_camera_capture_layer.setListener(object : CaptureLayer.CaptureListener {

            override fun onBackClick() {
                xpicker_camera_switch_iv.visibility = View.VISIBLE
                xpicker_camera_photo_show_iv.visibility = View.GONE
                xpicker_camera_capture_layer.normal()
            }

            override fun onDoneClick() {

            }

            override fun onLongClick() {
            }

            override fun onClick() {
                //隐藏切换按钮
                xpicker_camera_switch_iv.visibility = View.GONE
                cameraEngine.takePhoto {
                    if (it == null) {
                        Toast.makeText(this@XCameraActivity, "拍照出错~", Toast.LENGTH_SHORT)
                            .show()
                        xpicker_camera_capture_layer.normal()
                    } else {
                        xpicker_camera_photo_show_iv.visibility = View.VISIBLE
                        XPicker.onImageLoad(it, xpicker_camera_photo_show_iv)
                        xpicker_camera_capture_layer.done()
                    }
                }
            }

        })
        xpicker_camera_switch_iv.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            cameraEngine.initAndPreviewCamera(lensFacing, xpicker_camera_preview)
        }
    }


}



