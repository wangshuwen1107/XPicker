package cn.cheney.xpicker.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    fun saveBitmapFile(bitmap: Bitmap, file: File) {
        try {
            val bos =
                BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
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
}