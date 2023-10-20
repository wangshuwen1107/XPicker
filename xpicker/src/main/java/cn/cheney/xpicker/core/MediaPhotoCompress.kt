package cn.cheney.xpicker.core

import android.content.Context
import android.os.Environment
import cn.cheney.xpicker.XPickerConstant
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.util.Logger
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File

class MediaPhotoCompress {

    private var mediaList = arrayListOf<MediaEntity>()

    companion object {
        //压缩子目录
        private const val COMPRESS_DIR_TAG = "compress"
    }

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
            if (it.fileType == MediaEntity.FILE_TYPE_IMAGE && MediaEntity.GIF != it.mineType) {
                this.mediaList.add(it)
            }
        }
        if (this.mediaList.isNullOrEmpty()) {
            callback()
            return
        }
        compressSingle(context, 0, callback)
    }


    private fun compressSingle(context: Context, index: Int, callback: () -> Unit) {
        Luban.with(context)
            .load(mediaList[index].localPath)
            .setTargetDir(getCompressDir(context).absolutePath)
            .setRenameListener {
                File(it!!).name
            }
            .setCompressListener(object : OnCompressListener {
                override fun onSuccess(file: File?) {
                    Logger.d("Compress Success ${file?.absolutePath}")
                    if (null != file && file.exists()) {
                        mediaList[index].compressPath = file.absolutePath
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
        val publicPicturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val applicationId = context.applicationInfo.processName
        val pickerDirName = applicationId.replace(".", "_")
        val selfPickerDir = File(publicPicturesDir, pickerDirName)
        if (!selfPickerDir.exists()) {
            selfPickerDir.mkdir()
        }
        val selfCompressDir = File(selfPickerDir, COMPRESS_DIR_TAG)
        if (!selfCompressDir.exists()) {
            selfCompressDir.mkdirs()
        }
        return selfCompressDir
    }

}