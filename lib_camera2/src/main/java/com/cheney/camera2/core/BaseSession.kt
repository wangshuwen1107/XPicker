package com.cheney.camera2.core

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult

open class BaseSession {
    /**
     * 发送连续预览请求
     */
    fun setRepeatingPreview(
        previewBuilder: CaptureRequest.Builder,
        session: CameraCaptureSession
    ): Boolean {
        return try {
            session.stopRepeating()
            session.abortCaptures()
            session.setRepeatingRequest(
                previewBuilder.build(),
                null,
                CameraThreadManager.cameraHandler
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
    }

}