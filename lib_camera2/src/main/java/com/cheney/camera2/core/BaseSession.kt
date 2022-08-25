package com.cheney.camera2.core

import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.view.Surface

abstract class BaseSession {

    abstract fun getCameraDevice(): CameraDevice?


    /**
     * 构建连续预览请求
     */
    fun setRepeatingPreview(
        previewBuilder: CaptureRequest.Builder,
        session: CameraCaptureSession
    ) {
        try {
            session.stopRepeating()
            session.abortCaptures()
            session.setRepeatingRequest(
                previewBuilder.build(),
                null,
                CameraThreadManager.cameraHandler
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 构建聚焦请求
     */
    fun setFocusRequest(builder: CaptureRequest.Builder, afRect: Rect, aeRect: Rect) {
        builder.apply {
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            //聚焦区域
            set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(afRect, 1000)))
            //曝光区域
            set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(MeteringRectangle(aeRect, 1000)))
        }
    }

    /**
     * 发送capture
     */
    fun capture(
        builder: CaptureRequest.Builder,
        session: CameraCaptureSession,
        completed: ((Boolean) -> Unit)?
    ) {
        try {
            session.capture(
                builder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        completed?.invoke(true)
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                        completed?.invoke(false)
                    }

                }, CameraThreadManager.cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            completed?.invoke(false)
        }
    }

    /**
     * 构建照相request builder
     */
    fun createCaptureRequest(
        surface: Surface,
        orientation: Int
    ): CaptureRequest.Builder? {
        var builder: CaptureRequest.Builder? = null
        try {
            builder = getCameraDevice()?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        builder?.apply {
            set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            set(CaptureRequest.JPEG_ORIENTATION, orientation)
            addTarget(surface)
        }
        return builder
    }

    /**
     * 构建预览request builder
     */
    fun createPreviewRequest(surface: Surface): CaptureRequest.Builder? {
        var builder: CaptureRequest.Builder? = null
        try {
            builder = getCameraDevice()?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        builder?.apply {
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
            set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
            )
            addTarget(surface)
        }
        return builder
    }


    fun createVideoRequest(
        preview: Surface,
        recorder: Surface
    ): CaptureRequest.Builder? {
        var builder: CaptureRequest.Builder? = null
        try {
            builder = getCameraDevice()?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        builder?.apply {
            addTarget(preview)
            addTarget(recorder)
        }
        return builder

    }


}