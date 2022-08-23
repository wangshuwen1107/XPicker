package com.cheney.camera2.callback

import java.io.File

abstract class TakePhotoCallback {
    open fun onSuccess(file: File) {}
    open fun onFailed(errorCode: Int, errorMsg: String) {}
}