package com.cheney.camera2.callback

abstract class CaptureUIListener {
    open fun takePictures() {}

    open fun recordShort(time: Long) {}

    open fun recordStart() {}

    open fun recordEnd(time: Long) {}

    open fun recordZoom(zoom: Float) {}

    open fun recordTime(time: Long) {}

    open fun ok() {}

    open fun cancel() {}
}