package com.cheney.camera2.callback

import java.io.File

interface VideoRecordCallback {
     fun onSuccess(file: File)
     fun onFailed()
}