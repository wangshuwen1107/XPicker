package com.cheney.camera2.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object CameraThreadManager {

    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    val cameraHandler = Handler(cameraThread.looper)

    val mainHandler = Handler(Looper.getMainLooper())

}