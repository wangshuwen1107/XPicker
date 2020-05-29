package cn.cheney.lib_picker.picker;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.cheney.lib_picker.XPickerConstant;
import cn.cheney.lib_picker.entity.MediaEntity;
import cn.cheney.lib_picker.entity.MediaFolder;

public class MediaLoader {

    private static final String DURATION = "duration";
    private static final String VIDEO = "video";
    private static final String IMAGE = "image";

    private int type;
    private FragmentActivity activity;
    private boolean isGif;

    private static final String[] PROJECTION_ALL = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            DURATION,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
    };

    /**
     * 图片
     */
    private final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
    };

    /**
     * 视频
     */
    private final static String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
    };

    /**
     * 查询全部图片和视频
     */
    private static final String SELECTION_ALL =
            MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE
                    + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    + " AND "
                    + MediaStore.Files.FileColumns.SIZE + ">0";

    /**
     * 获取全部图片和视频，但过滤掉gif图片
     */
    private static final String SELECTION_ALL_WITHOUT_GIF =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " AND "
                    + MediaStore.Images.Media.MIME_TYPE + "!= 'image/gif')"
                    + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    /**
     * 获取全部图片
     */
    private static final String SELECTION_IMAGE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


    /**
     * 获取全部图片[除Gif]
     */
    private static final String SELECTION_IMAGE_WITHOUT_GIF =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + "AND"
                    + MediaStore.Images.Media.MIME_TYPE + "!='image/gif'";

    /**
     * 获取全部视频
     */
    private static final String SELECTION_VIDEO =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;


    public MediaLoader(FragmentActivity activity, int type, boolean isGif) {
        this.activity = activity;
        this.type = type;
        this.isGif = isGif;
    }

    public void loadAllMedia(final LocalMediaLoadListener imageLoadListener) {
        LoaderManager.getInstance(activity).initLoader(type, null,
                new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        CursorLoader cursorLoader = null;
                        switch (id) {
                            case XPickerConstant.TYPE_ALL:
                                if (isGif) {
                                    cursorLoader = new CursorLoader(
                                            activity, MediaStore.Files.getContentUri("external"),
                                            PROJECTION_ALL,
                                            SELECTION_ALL,
                                            null,
                                            MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
                                } else {
                                    cursorLoader = new CursorLoader(
                                            activity, MediaStore.Files.getContentUri("external"),
                                            PROJECTION_ALL,
                                            SELECTION_ALL_WITHOUT_GIF,
                                            null,
                                            MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
                                }
                                break;
                            case XPickerConstant.TYPE_IMAGE:
                                if (isGif) {
                                    cursorLoader = new CursorLoader(
                                            activity, MediaStore.Files.getContentUri("external"),
                                            IMAGE_PROJECTION,
                                            SELECTION_IMAGE,
                                            null,
                                            MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
                                } else {
                                    cursorLoader = new CursorLoader(
                                            activity, MediaStore.Files.getContentUri("external"),
                                            IMAGE_PROJECTION,
                                            SELECTION_IMAGE_WITHOUT_GIF,
                                            null,
                                            MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
                                }
                                break;
                            case XPickerConstant.TYPE_VIDEO:
                                cursorLoader = new CursorLoader(
                                        activity, MediaStore.Files.getContentUri("external"),
                                        VIDEO_PROJECTION,
                                        SELECTION_VIDEO,
                                        null,
                                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
                                break;
                        }
                        return cursorLoader;
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                        List<MediaFolder> imageFolders = new ArrayList<>();
                        try {
                            MediaFolder allImageFolder = new MediaFolder();
                            List<MediaEntity> latelyImages = new ArrayList<>();
                            if (data != null) {
                                int count = data.getCount();
                                if (count > 0) {
                                    data.moveToFirst();
                                    do {
                                        String path = data.getString
                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                                        // 如原图路径不存在或者路径存在但文件不存在,就结束当前循环
                                        if (TextUtils.isEmpty(path) || !new File(path).exists()) {
                                            continue;
                                        }
                                        String mimeType = data.getString
                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
                                        boolean eqImg = mimeType.startsWith(IMAGE);
                                        int duration = eqImg ? 0 : data.getInt
                                                (data.getColumnIndexOrThrow(VIDEO_PROJECTION[7]));
                                        int w = eqImg ? data.getInt
                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[4])) : 0;
                                        int h = eqImg ? data.getInt
                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[5])) : 0;

                                        int fileType = 0;
                                        if (mimeType.startsWith(IMAGE)) {
                                            fileType = XPickerConstant.TYPE_IMAGE;
                                        } else if (mimeType.startsWith(VIDEO)) {
                                            fileType = XPickerConstant.TYPE_VIDEO;
                                        }

                                        MediaEntity mediaEntity = new MediaEntity();
                                        mediaEntity.setLocalPath(path);
                                        mediaEntity.setFileType(fileType);
                                        mediaEntity.setDuration(duration);
                                        mediaEntity.setWidth(w);
                                        mediaEntity.setHeight(h);
                                        mediaEntity.setMineType(mimeType);

                                        MediaFolder folder = getImageFolder(path, imageFolders);
                                        List<MediaEntity> mediaList = folder.getMediaList();
                                        mediaList.add(mediaEntity);
                                        folder.setImageNum(folder.getImageNum() + 1);

                                        latelyImages.add(mediaEntity);
                                        int imageNum = allImageFolder.getImageNum();
                                        allImageFolder.setImageNum(imageNum + 1);
                                    } while (data.moveToNext());

                                    if (latelyImages.size() > 0) {
                                        sortFolder(imageFolders);
                                        imageFolders.add(0, allImageFolder);
                                        allImageFolder.setFirstImagePath
                                                (latelyImages.get(0).getLocalPath());
                                        String title = type == XPickerConstant.TYPE_VIDEO ?
                                                "所有音频"
                                                : "相机胶卷";
                                        allImageFolder.setName(title);
                                        allImageFolder.setMediaList(latelyImages);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            imageLoadListener.loadComplete(imageFolders);
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {
                    }
                });
    }

    private void sortFolder(List<MediaFolder> imageFolders) {
        // 文件夹按图片数量排序
        Collections.sort(imageFolders, new Comparator<MediaFolder>() {
            @Override
            public int compare(MediaFolder lhs, MediaFolder rhs) {
                if (lhs.getMediaList().isEmpty() || rhs.getMediaList().isEmpty()) {
                    return 0;
                }
                int lsize = lhs.getImageNum();
                int rsize = rhs.getImageNum();
                return Integer.compare(rsize, lsize);
            }
        });
    }

    private MediaFolder getImageFolder(String path, List<MediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();
        for (MediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        MediaFolder newFolder = new MediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    public interface LocalMediaLoadListener {
        void loadComplete(List<MediaFolder> folders);
    }
}
