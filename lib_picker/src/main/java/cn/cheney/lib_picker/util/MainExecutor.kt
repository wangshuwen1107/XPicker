package cn.cheney.lib_picker.util

import android.os.Handler
import android.os.Looper

class MainExecutor : ThreadExecutor(Handler(Looper.getMainLooper())) {

    override fun execute(runnable: Runnable) {
        handler.post(runnable)
    }
}