package cn.cheney.xpicker

import android.app.Application
import cn.cheney.lib_picker.XPicker
import com.bumptech.glide.Glide

class MyApp : Application() {


    override fun onCreate() {
        super.onCreate()

        XPicker.imageLoadListener = { imageUri, iv ->
            Glide.with(this@MyApp)
                .load(imageUri)
                .into(iv)
        }
    }
}