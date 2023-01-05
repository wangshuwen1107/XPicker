package com.cheney.camera2.core

import android.content.Context
import android.media.MediaRecorder
import android.util.Size
import android.view.Surface
import com.cheney.camera2.callback.VideoRecordCallback
import com.cheney.camera2.util.FileUtil
import java.io.File
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

class XVideoRecorder constructor(private var mContext: Context) {

    private var mMediaRecorder: MediaRecorder? = null

    private var recorderFile: File? = null

    private var isRecording = AtomicBoolean(false)

    companion object {
        private const val DEFAULT_BIT_RATE = 3 * 1024 * 1024
        private const val DEFAULT_FRAME_RATE = 30
    }

    fun setUp(videoSize: Size, rotation: Int) {
        if (null == mMediaRecorder) {
            mMediaRecorder = MediaRecorder()
        }
        try {
            mMediaRecorder?.apply {
                recorderFile = FileUtil.createFile(
                    mContext.externalMediaDirs.first(),
                    FileUtil.FILENAME,
                    FileUtil.VIDEO_EXTENSION
                )
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(recorderFile!!.path)
                setVideoEncodingBitRate(DEFAULT_BIT_RATE)
                setVideoFrameRate(DEFAULT_FRAME_RATE)
                setVideoSize(videoSize.width, videoSize.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOrientationHint(rotation)
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start() {
        try {
            if (isRecording.get()) {
                return
            }
            mMediaRecorder?.start()
            isRecording.set(true)
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording.set(false)
            deleteFile()
        }
    }

    fun stop(callback: VideoRecordCallback?) {
        try {
            mMediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            callbackResult(callback)
            isRecording.set(false)
            mMediaRecorder = null
        }
    }

    fun getRecorderSurface(): Surface? {
        var surface: Surface? = null
        try {
            surface = mMediaRecorder?.surface
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return surface
    }

    private fun callbackResult(callback: VideoRecordCallback?) {
        if (recorderFile?.exists() == true && isRecording.get()) {
            callback?.onSuccess(recorderFile!!)
        } else {
            deleteFile()
            callback?.onFailed()
        }
        recorderFile = null
    }

    private fun deleteFile() {
        recorderFile?.delete()
        recorderFile = null
    }
}