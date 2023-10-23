package cn.cheney.xpicker.core

import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.fragment.app.FragmentActivity
import androidx.loader.content.CursorLoader
import cn.cheney.xpicker.entity.MediaEntity
import cn.cheney.xpicker.entity.MediaFolder
import cn.cheney.xpicker.entity.MineType
import cn.cheney.xpicker.util.Logger
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MediaLoader(var activity: FragmentActivity, var filterType: Int = 0) {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val DURATION = "duration"
        private const val VIDEO = "video"
        private const val IMAGE = "image"

        private const val SELECTION_ALL = (MediaStore.Files.FileColumns.MEDIA_TYPE
                + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE
                + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                + " AND "
                + MediaStore.Files.FileColumns.SIZE + ">0")

        private const val SELECTION_ALL_WITHOUT_GIF =
            ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " AND "
                    + MediaStore.Images.Media.MIME_TYPE + "!= 'image/gif')"
                    + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        private const val SELECTION_IMAGE = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)

        private const val SELECTION_IMAGE_WITHOUT_GIF =
            (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " AND "
                    + MediaStore.Images.Media.MIME_TYPE + "!='image/gif'")

        private const val SELECTION_VIDEO = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
    }

    private val PROJECTION_ALL = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        DURATION,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT
    )

    private val IMAGE_PROJECTION = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.SIZE
    )

    private val VIDEO_PROJECTION = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.MIME_TYPE,
        DURATION,
    )


    private fun onLoadComplete(startTime: Long, data: Cursor?, imageLoadListener: LocalMediaLoadListener) {
        val imageFolders: ArrayList<MediaFolder> = arrayListOf()
        try {
            val allImageFolder = MediaFolder()
            val latelyImages: ArrayList<MediaEntity> = ArrayList()
            if (data != null) {
                val count = data.count
                if (count > 0) {
                    data.moveToFirst()
                    do {
                        val id = data.getLongOrNull(data.getColumnIndex(PROJECTION_ALL[0]))
                        val path = data.getString(data.getColumnIndex(IMAGE_PROJECTION[1]))
                        if (TextUtils.isEmpty(path) || !File(path).exists()) {
                            continue
                        }
                        val mimeType = data.getString(data.getColumnIndex(IMAGE_PROJECTION[6]))
                        val eqImg = mimeType.startsWith(IMAGE)
                        val width = if (eqImg) {
                            data.getIntOrNull(data.getColumnIndex(IMAGE_PROJECTION[4])) ?: 0
                        } else {
                            0
                        }
                        val height = if (eqImg) {
                            data.getIntOrNull(data.getColumnIndex(IMAGE_PROJECTION[5])) ?: 0
                        } else {
                            0
                        }
                        var fileType = 0
                        var duration = 0L
                        if (mimeType.startsWith(IMAGE)) {
                            fileType = MediaEntity.FILE_TYPE_IMAGE
                        } else if (mimeType.startsWith(VIDEO)) {
                            fileType = MediaEntity.FILE_TYPE_VIDEO
                            duration = data.getLongOrNull(data.getColumnIndex(VIDEO_PROJECTION[7])) ?: 0L
                        }
                        val file = File(path)
                        //过滤文件小于0
                        if (!file.exists() || file.length() <= 0) {
                            continue
                        }
                        //过滤时间<700ms的视频
                        if (fileType == MediaEntity.FILE_TYPE_VIDEO && duration < 700) {
                            continue
                        }
                        val mediaEntity = MediaEntity(path)
                        mediaEntity.fileType = fileType
                        mediaEntity.duration = duration
                        mediaEntity.width = width
                        mediaEntity.height = height
                        mediaEntity.mineType = mimeType
                        val folder = getImageFolder(path, imageFolders) ?: continue
                        val mediaList = folder.mediaList
                        mediaList.add(mediaEntity)
                        folder.imageNum = folder.imageNum + 1
                        latelyImages.add(mediaEntity)
                        val imageNum = allImageFolder.imageNum
                        allImageFolder.imageNum = imageNum + 1
                    } while (data.moveToNext())

                    if (latelyImages.size > 0) {
                        sortFolder(imageFolders)
                        imageFolders.add(0, allImageFolder)
                        allImageFolder.firstImagePath = latelyImages[0].localPath
                        allImageFolder.firstImageMineType = latelyImages[0].mineType
                        val title = if (filterType == MediaEntity.FILE_TYPE_VIDEO) "所有音频" else "相机胶卷"
                        allImageFolder.name = title
                        allImageFolder.mediaList = latelyImages
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            handler.post {
                Logger.d("扫描耗时=${System.currentTimeMillis() - startTime}")
                imageLoadListener.loadComplete(imageFolders)
            }
        }
    }

    fun loadAllMedia(imageLoadListener: LocalMediaLoadListener) {
        var loader: CursorLoader? = null
        when (filterType) {
            MineType.TYPE_ALL.type ->
                loader = CursorLoader(
                    activity, MediaStore.Files.getContentUri("external"),
                    PROJECTION_ALL,
                    SELECTION_ALL,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )
            MineType.TYPE_ALL_WITHOUT_GIF.type ->
                loader = CursorLoader(
                    activity, MediaStore.Files.getContentUri("external"),
                    PROJECTION_ALL,
                    SELECTION_ALL_WITHOUT_GIF,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )
            MineType.TYPE_IMAGE.type ->
                loader = CursorLoader(
                    activity, MediaStore.Files.getContentUri("external"),
                    IMAGE_PROJECTION,
                    SELECTION_IMAGE,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )
            MineType.TYPE_IMAGE_WITHOUT_GIF.type ->
                loader = CursorLoader(
                    activity, MediaStore.Files.getContentUri("external"),
                    IMAGE_PROJECTION,
                    SELECTION_IMAGE_WITHOUT_GIF,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )
            MineType.TYPE_VIDEO.type -> loader =
                CursorLoader(
                    activity, MediaStore.Files.getContentUri("external"),
                    VIDEO_PROJECTION,
                    SELECTION_VIDEO,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )
        }
        val startTime = System.currentTimeMillis()
        loader!!.registerListener(filterType) { _, data ->
            thread { onLoadComplete(startTime, data, imageLoadListener) }
        }

        loader.startLoading()
    }

    private fun sortFolder(imageFolders: List<MediaFolder>) {
        Collections.sort(imageFolders, object : Comparator<MediaFolder?> {
            override fun compare(o1: MediaFolder?, o2: MediaFolder?): Int {
                if (null == o1 || null == o2) {
                    return 0
                }
                if (o1.mediaList.isEmpty() || o2.mediaList.isEmpty()) {
                    return 0
                }
                return o1.imageNum.compareTo(o2.imageNum)
            }
        })
    }

    private fun getImageFolder(path: String, imageFolders: ArrayList<MediaFolder>): MediaFolder? {
        val imageFile = File(path)
        val folderFile = imageFile.parentFile ?: return null
        //已存在的文件夹里面包含着 自己所属的文件夹
        for (folder in imageFolders) {
            if (folder.name == folderFile.name) {
                return folder
            }
        }
        //创新新的文件夹
        val newFolder = MediaFolder()
        newFolder.name = folderFile.name
        newFolder.path = folderFile.absolutePath
        newFolder.firstImagePath = path
        imageFolders.add(newFolder)
        return newFolder
    }

    interface LocalMediaLoadListener {
        fun loadComplete(folders: List<MediaFolder>?)
    }


}