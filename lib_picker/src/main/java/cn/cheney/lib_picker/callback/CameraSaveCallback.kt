package cn.cheney.lib_picker.callback

import android.net.Uri

interface CameraSaveCallback {

    fun onTakePhotoSuccess(photoUri: Uri)

    fun onTakePhotoFailed(errorCode: String)

    fun onVideoSuccess(coverUri: Uri?, videoUri: Uri, duration: Int?)

    fun onVideoFailed(errorCode: String)


}