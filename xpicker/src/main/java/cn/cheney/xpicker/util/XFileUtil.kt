package cn.cheney.xpicker.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object XFileUtil {

    const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val PHOTO_EXTENSION = ".jpg"
    const val VIDEO_EXTENSION = ".mp4"

    /** Helper function used to create a timestamped file */
    fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.CHINA)
                .format(System.currentTimeMillis()) + extension
        )

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

    fun saveBitmapFile(bitmap: Bitmap, file: File): Boolean {
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

    fun saveBytes(bytes: ByteArray, file: File) {
        var bos: OutputStream? = null
        try {
            bos = FileOutputStream(file)
            bos.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                bos?.close()
            } catch (e: IOException) {

            }
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
}