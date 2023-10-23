package cn.cheney.picker.app

import android.app.Application
import android.widget.ImageView
import cn.cheney.xpicker.ImageLoadListener
import cn.cheney.xpicker.XPicker
import com.bumptech.glide.Glide
import java.io.File

class MyApp : Application() {


    override fun onCreate() {
        super.onCreate()

        XPicker.imageLoadListener = object : ImageLoadListener {
            override fun onFileLoad(file: File, iv: ImageView, mineType: String?) {
                Glide.with(this@MyApp)
                    .load(file)
                    .into(iv)
            }

        }
    }
}