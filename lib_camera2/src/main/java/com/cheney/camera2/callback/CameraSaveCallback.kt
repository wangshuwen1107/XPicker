package com.cheney.camera2.callback

import android.graphics.Bitmap
import android.net.Uri

interface CameraSaveCallback {

    fun onTakePhotoSuccess(photoUri: Uri)

    fun onTakePhotoFailed(errorCode: String)

    fun onVideoSuccess(cover: Bitmap?, videoUri: Uri, duration: Int?)

    fun onVideoFailed(errorCode: String)


}