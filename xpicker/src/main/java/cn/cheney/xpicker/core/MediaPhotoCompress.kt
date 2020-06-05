package cn.cheney.xpicker.core

import android.content.Context
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.util.Logger
import cn.cheney.xpicker.util.getUir
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File

class MediaPhotoCompress {

    private var mediaList = arrayListOf<MediaEntity>()

    fun compressImg(
        context: Context,
        mediaList: List<MediaEntity>? = null,
        callback: () -> Unit
    ) {
        if (null == mediaList) {
            callback.invoke()
            return
        }
        mediaList.forEach {
            if (null != it.localUri
                && it.fileType == XPickerConstant.FILE_TYPE_IMAGE
                && XPickerConstant.GIF != it.mineType
            ) {
                this.mediaList.add(it)
            }
        }
        compressSingle(context, 0, callback)
    }


    private fun compressSingle(context: Context, index: Int, callback: () -> Unit) {
        Luban.with(context)
            .load(mediaList[index].localUri!!)
            .setTargetDir(getCompressDir(context).absolutePath)
            .setRenameListener {
                File(it!!).name
            }
            .setCompressListener(object : OnCompressListener {
                override fun onSuccess(file: File?) {
                    Logger.d("Compress Success ${file?.absolutePath}")
                    if (null != file && file.exists()) {
                        mediaList[index].compressLocalUri = file.getUir(context)
                    }
                    if (index == mediaList.size - 1) {
                        callback.invoke()
                    } else {
                        compressSingle(context, index + 1, callback)
                    }
                }

                override fun onError(e: Throwable?) {
                    Logger.e("Compress onError ${e?.message}")
                    if (index == mediaList.size - 1) {
                        callback.invoke()
                    } else {
                        compressSingle(context, index + 1, callback)
                    }
                }

                override fun onStart() {}

            })
            .launch()
    }


    private fun getCompressDir(context: Context): File {
        val compressDir = File(
            context.externalMediaDirs.first().absolutePath
                    + File.separator + XPickerConstant.COMPRESS_DIR_TAG
        )
        if (!compressDir.exists()) {
            compressDir.mkdirs()
        }
        return compressDir
    }

}