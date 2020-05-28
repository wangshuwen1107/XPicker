package cn.cheney.lib_picker.util

import android.util.Log
import java.util.*

class Logger {

    companion object {
        private const val TAG = "XPicker - %1\$s.%2\$s(L:%3\$d)"

        fun v(vararg messages: String) {
            val message: String = concatMessage(*messages)
            val maxLogChars = 1364
            var i = 0
            while (i < message.length) {
                var end = i + maxLogChars
                if (end > message.length) {
                    end = message.length
                }
                Log.v(generateTag(), message.substring(i, end).trim { it <= ' ' })
                i += maxLogChars
            }
        }

        fun d(vararg messages: String) {
            val message: String = concatMessage(*messages)
            val maxLogChars = 1364
            var i = 0
            while (i < message.length) {
                var end = i + maxLogChars
                if (end > message.length) {
                    end = message.length
                }
                Log.d(generateTag(), message.substring(i, end).trim { it <= ' ' })
                i += maxLogChars
            }
        }

        fun i(vararg messages: String) {
            val message: String = concatMessage(*messages)
            val maxLogChars = 1364
            var i = 0
            while (i < message.length) {
                var end = i + maxLogChars
                if (end > message.length) {
                    end = message.length
                }
                Log.i(generateTag(), message.substring(i, end).trim { it <= ' ' })
                i += maxLogChars
            }
        }

        fun w(vararg messages: String) {
            val message: String = concatMessage(*messages)
            val maxLogChars = 1364
            var i = 0
            while (i < message.length) {
                var end = i + maxLogChars
                if (end > message.length) {
                    end = message.length
                }
                Log.w(generateTag(), message.substring(i, end).trim { it <= ' ' })
                i += maxLogChars
            }
        }

        fun e(vararg messages: String) {
            val message: String = concatMessage(*messages)
            val maxLogChars = 1364
            var i = 0
            while (i < message.length) {
                var end = i + maxLogChars
                if (end > message.length) {
                    end = message.length
                }
                Log.e(generateTag(), message.substring(i, end).trim { it <= ' ' })
                i += maxLogChars
            }
        }

        private fun generateTag(): String? {
            val caller =
                Thread.currentThread().stackTrace[4]
            var callerClazzName = caller.className
            callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1)
            return String.format(
                Locale.getDefault(),
                TAG,
                callerClazzName,
                caller.methodName,
                caller.lineNumber
            )
        }

        private fun concatMessage(vararg messages: String): String {
            return if (messages.isNotEmpty()) {
                val sb = StringBuilder()
                for (message in messages) {
                    sb.append(message)
                }
                sb.toString()
            } else {
                ""
            }
        }
    }
}