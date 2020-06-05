package cn.cheney.picker.app

import android.app.Application
import cn.cheney.xpicker.XPicker
import com.bumptech.glide.Glide

class MyApp : Application() {


    override fun onCreate() {
        super.onCreate()

        XPicker.imageLoadListener = { imageUri, iv, mineType ->
            Glide.with(this@MyApp)
                .load(imageUri)
                .into(iv)
        }
    }
}