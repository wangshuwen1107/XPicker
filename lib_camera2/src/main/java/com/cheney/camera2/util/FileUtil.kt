package com.cheney.camera2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {

    const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val PHOTO_EXTENSION = ".jpg"
    const val VIDEO_EXTENSION = ".mp4"

    fun getVideoAndDuration(videoPath: String): Pair<File?, Int>? {
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
        saveBitmapFile(
            bitmap,
            File(replace)
        )
        return Pair(File(replace), duration?.toInt() ?: 0)
    }


    fun saveImage(context: Context, image: Image, orientation: Int, mirrored: Boolean): File? {
        val outputFile = createFile(context.externalMediaDirs.first(), FILENAME, PHOTO_EXTENSION)
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix()
        matrix.postRotate(orientation * 1.0f, bitmap.width * 1.0f / 2, bitmap.height * 1.0f / 2)
        if (mirrored) {
            matrix.postScale(-1f, 1f)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        image.close()
        if (saveBitmapFile(bitmap, outputFile)) {
            return outputFile
        }
        return null
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

    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.CHINA)
                .format(System.currentTimeMillis()) + extension
        )


}