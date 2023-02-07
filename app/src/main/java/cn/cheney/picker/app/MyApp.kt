package cn.cheney.picker.app

import android.app.Application
import cn.cheney.xpicker.XPicker
import com.bumptech.glide.Glide

class MyApp : Application() {


    override fun onCreate() {
        super.onCreate()

        XPicker.imageLoadListener = { file, iv, mineType ->
            Glide.with(this@MyApp)
                .load(file)
                .into(iv)
        }
    }
}