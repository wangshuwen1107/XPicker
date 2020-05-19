package cn.cheney.lib_picker.camera

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import cn.cheney.lib_picker.R
import cn.cheney.lib_picker.XPicker
import cn.cheney.lib_picker.callback.CaptureListener
import kotlinx.android.synthetic.main.xpicker_activity_camera.*

class XCameraActivity : AppCompatActivity() {

    private var lensFacing: CameraX.LensFacing = CameraX.LensFacing.BACK

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
        xpicker_camera_capture_layer.setListener(object : CaptureListener() {
            override fun cancel() {
                super.cancel()
                xpicker_camera_switch_iv.visibility = View.VISIBLE
                xpicker_camera_photo_preview_iv.visibility = View.GONE
                xpicker_camera_capture_layer.reset()
            }

            override fun ok() {
                super.ok()
                TODO()
            }

            override fun takePictures() {
                super.takePictures()
                xpicker_camera_switch_iv.visibility = View.GONE
                cameraEngine.takePhoto {
                    if (it == null) {
                        Toast.makeText(this@XCameraActivity, "拍照出错~", Toast.LENGTH_SHORT)
                            .show()
                        xpicker_camera_capture_layer.reset()
                    } else {
                        xpicker_camera_photo_preview_iv.visibility = View.VISIBLE
                        XPicker.onImageLoad(it, xpicker_camera_photo_preview_iv)
                        xpicker_camera_capture_layer.done()
                    }
                }
            }

            override fun recordStart() {
                super.recordStart()
                cameraEngine.startRecord { videoUri, coverUri, duration->

                }
            }

            override fun recordShort(time: Long) {
                super.recordShort(time)
                Toast.makeText(this@XCameraActivity, "拍摄时间太短了~", Toast.LENGTH_SHORT)
                    .show()
                xpicker_camera_capture_layer.reset()
            }

            override fun recordEnd(time: Long) {
                super.recordEnd(time)
                cameraEngine.stopRecord()
            }

        })
        xpicker_camera_switch_iv.setOnClickListener {
            lensFacing = if (lensFacing == CameraX.LensFacing.BACK) {
                CameraX.LensFacing.FRONT
            } else {
                CameraX.LensFacing.BACK
            }
            cameraEngine.initAndPreviewCamera(lensFacing, xpicker_camera_preview)
        }
    }


}



