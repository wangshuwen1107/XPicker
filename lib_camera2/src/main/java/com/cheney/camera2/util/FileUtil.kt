package com.cheney.camera2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.Image
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


object FileUtil {

    private const val FILENAME_FORMAT = "yyyy_MM_dd_HH_mm_ss"
    private const val PHOTO_EXTENSION = ".jpg"
    private const val VIDEO_EXTENSION = ".mp4"

    fun getVideoAndDuration(videoPath: String): Pair<Bitmap?, Int>? {
        if (TextUtils.isEmpty(videoPath)) {
            return null
        }
        if (!File(videoPath).exists()) {
            return null
        }
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(videoPath)
        val duration =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val bitmap = mmr.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        val replace = videoPath.replace(".mp4", ".jpg")
        if (null == bitmap) {
            return Pair(null, duration?.toInt() ?: 0)
        }
//        saveBitmapFile(
//            bitmap,
//            File(replace)
//        )
        return Pair(bitmap, duration?.toInt() ?: 0)
    }


    fun saveImage(context: Context, image: Image, mirrored: Boolean): File {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
        image.close()
        //先写文件便于后面取图片的角度
        val file = createJpgFile(context)
        writeFile(file, bytes)
        val orientation = getJpegOrientation(file)
        //图像做转换
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix()
        matrix.postRotate(orientation * 1.0f)
        if (mirrored) {
            matrix.postScale(-1f, 1f)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        //将正确的图像存入文件
        saveBitmapFile(bitmap, file)
        return file
    }

    private fun writeFile(path: File, bytes: ByteArray) {
        try {
            val out = FileOutputStream(path) //指定写到哪个路径中
            val fileChannel = out.channel
            fileChannel.write(ByteBuffer.wrap(bytes)) //将字节流写入文件中
            fileChannel.force(true) //强制刷新
            fileChannel.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun scanPhotoAlbum(context: Context, dataFile: File?) {
        if (dataFile == null) {
            return
        }
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            dataFile.absolutePath.substring(
                dataFile.absolutePath.lastIndexOf(".") + 1
            )
        )
        MediaScannerConnection.scanFile(
            context,
            arrayOf(dataFile.absolutePath),
            arrayOf(mimeType),
            null
        )
    }

    private fun saveBitmapFile(bitmap: Bitmap, file: File): Boolean {
        var bos: OutputStream? = null
        return try {
            bos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } finally {
            try {
                bos?.close()
            } catch (e: IOException) {

            }
        }
    }

    fun createJpgFile(context: Context): File {
        val createFile = createFile(context, FILENAME_FORMAT, PHOTO_EXTENSION)
        createFile.createNewFile()
        return createFile
    }

    fun createVideoFile(context: Context): File {
        val createFile = createFile(context, FILENAME_FORMAT, VIDEO_EXTENSION)
        createFile.createNewFile()
        return createFile
    }

    fun createFile(context: Context, format: String, extension: String): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val applicationId = context.applicationInfo.processName
        val pickerDirName = applicationId.replace(".","_")
        val pickerDir = File(picturesDir, pickerDirName)
        if (!pickerDir.exists()) {
            pickerDir.mkdir()
        }
        return File(pickerDir, SimpleDateFormat(format, Locale.CHINA)
                .format(System.currentTimeMillis()) + extension)
    }


    private fun getJpegOrientation(jpeg: File): Int {
        var degree = 0
        try {
            val exifInterface = ExifInterface(jpeg.absolutePath)
            val orientation: Int = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }

}