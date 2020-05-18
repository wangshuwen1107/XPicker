package cn.cheney.lib_picker.util

import android.os.Handler
import java.util.concurrent.Executor

open class ThreadExecutor(protected val handler: Handler) : Executor {

    override fun execute(runnable: Runnable) {
        handler.post(runnable)
    }
}