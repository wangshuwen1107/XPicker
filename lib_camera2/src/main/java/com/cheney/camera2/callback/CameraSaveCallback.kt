package com.cheney.camera2.callback

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

interface CameraSaveCallback {

    fun onTakePhotoSuccess(photoFile: File)

    fun onTakePhotoFailed(errorCode: String)

    fun onVideoSuccess(cover: Bitmap?, videoFile: File, duration: Int?)

    fun onVideoFailed(errorCode: String)


}