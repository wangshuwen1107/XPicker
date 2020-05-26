//package cn.cheney.lib_picker.picker;
//
//import android.database.Cursor;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.text.TextUtils;
//
//import androidx.fragment.app.FragmentActivity;
//import androidx.loader.app.LoaderManager;
//import androidx.loader.content.CursorLoader;
//import androidx.loader.content.Loader;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
//import cn.cheney.lib_picker.R;
//import cn.cheney.lib_picker.XPickerConstant;
//import cn.cheney.lib_picker.entity.MediaEntity;
//import cn.cheney.lib_picker.entity.MediaFolder;
//
//public class MediaLoader {
//
//    private static final String DURATION = "duration";
//    private static final int AUDIO_DURATION = 500;// 过滤掉小于500毫秒的录音
//    private int type = XPickerConstant.TYPE_IMAGE;
//    private FragmentActivity activity;
//    private boolean isGif;
//    private long videoS = 0;
//
//    /**
//     * 查询全部图片和视频，并且过滤掉已损坏图片和视频
//     */
//    private static final String SELECTION_ALL =
//            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
//                    + " OR "
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
//                    + " AND "
//                    + MediaStore.Images.Media.MIME_TYPE + "!=?"
//                    + " AND "
//                    + MediaStore.Images.Media.MIME_TYPE + "!=?"
//                    + " AND "
//                    + MediaStore.Files.FileColumns.SIZE + ">0";
//
//
//    private static final String[] SELECTION_ALL_ARGS = {
//            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
//            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
//            "image/gif",
//            "image/GIF"
//    };
//
//    private static final String[] PROJECTION_ALL = {
//            MediaStore.Files.FileColumns._ID,
//            MediaStore.MediaColumns.DATA,
//            MediaStore.MediaColumns.DATE_ADDED,
//            MediaStore.MediaColumns.DISPLAY_NAME,
//            MediaStore.MediaColumns.SIZE,
//            DURATION,
//            MediaStore.MediaColumns.MIME_TYPE,
//            MediaStore.MediaColumns.WIDTH,
//            MediaStore.MediaColumns.HEIGHT,
//    };
//
//    /**
//     * 获取全部图片和视频，但过滤掉gif图片
//     */
//    private static final String SELECTION_ALL_WITHOUT_GIF =
//            "(" + MediaStore.Images.Media.MIME_TYPE + "=?"
//                    + " OR "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?"
//                    + " OR "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?"
//                    + " OR "
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
//                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
//
//    private static final String[] SELECTION_ALL_WITHOUT_GIF_ARGS = {
//            "image/jpeg",
//            "image/png",
//            "image/webp",
//            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
//    };
//
//    /**
//     * 图片
//     */
//    private final static String[] IMAGE_PROJECTION = {
//            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.DATA,
//            MediaStore.Images.Media.DISPLAY_NAME,
//            MediaStore.Images.Media.DATE_ADDED,
//            MediaStore.Images.Media.WIDTH,
//            MediaStore.Images.Media.HEIGHT,
//            MediaStore.Images.Media.MIME_TYPE,
//            MediaStore.Images.Media.SIZE,
//    };
//
//    /**
//     * 视频
//     */
//    private final static String[] VIDEO_PROJECTION = {
//            MediaStore.Video.Media._ID,
//            MediaStore.Video.Media.DATA,
//            MediaStore.Video.Media.DISPLAY_NAME,
//            MediaStore.Video.Media.DATE_ADDED,
//            MediaStore.Video.Media.WIDTH,
//            MediaStore.Video.Media.HEIGHT,
//            MediaStore.Video.Media.MIME_TYPE,
//            MediaStore.Video.Media.DURATION,
//    };
//
//    /**
//     * 音频
//     */
//    private final static String[] AUDIO_PROJECTION = {
//            MediaStore.Audio.Media._ID,
//            MediaStore.Audio.Media.DATA,
//            MediaStore.Audio.Media.DISPLAY_NAME,
//            MediaStore.Audio.Media.DATE_ADDED,
//            MediaStore.Audio.Media.IS_MUSIC,
//            MediaStore.Audio.Media.IS_PODCAST,
//            MediaStore.Audio.Media.MIME_TYPE,
//            MediaStore.Audio.Media.DURATION,
//    };
//
//    /**
//     * 只查询图片条件
//     */
//    private final static String CONDITION_GIF =
//            "(" + MediaStore.Images.Media.MIME_TYPE + "=? or "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?" + " or "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?" + " or "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?)" + " AND "
//                    + MediaStore.MediaColumns.WIDTH + ">0";
//
//    private final static String[] SELECT_GIF = {
//            "image/jpeg",
//            "image/png",
//            "image/gif",
//            "image/webp"
//    };
//
//    /**
//     * 获取全部图片
//     */
//    private final static String IMAGE_SELECTION =
//            "(" + MediaStore.Images.Media.MIME_TYPE + "=? or "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?" + " or "
//                    + MediaStore.Images.Media.MIME_TYPE + "=?)" + " AND "
//                    + MediaStore.MediaColumns.WIDTH + ">0";
//
//    /**
//     * 获取全部图片
//     */
//    private final static String[] IMAGE_SELECTION_ARGS = {
//            "image/jpeg",
//            "image/png",
//            "image/webp"
//    };
//
//    private static final String SELECTION_IMAGE =
//            MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//
//    /**
//     * 获取全部视频
//     */
//    private final static String VIDEO_SELECTION =
//            "(" + MediaStore.Video.Media.MIME_TYPE + "=?)" + " AND "
//                    + MediaStore.MediaColumns.WIDTH + ">0";
//
//    private static final String SELECTION_VIDEO =
//            MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
//    /**
//     * 获取全部视频
//     */
//    private final static String[] VIDEO_SELECTION_ARGS = {
//            "video/mp4"
//    };
//
//    private static final String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
//
//    public MediaLoader(FragmentActivity activity, int type, boolean isGif, long videoS) {
//        this.activity = activity;
//        this.type = type;
//        this.isGif = isGif;
//        this.videoS = videoS;
//    }
//
//    public void loadAllMedia(final LocalMediaLoadListener imageLoadListener) {
//        LoaderManager.getInstance(activity).initLoader(type, null,
//                new LoaderManager.LoaderCallbacks<Cursor>() {
//                    @Override
//                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//                        CursorLoader cursorLoader = null;
//                        switch (id) {
//                            case XPickerConstant.TYPE_ALL:
//                                cursorLoader = new CursorLoader(
//                                        activity, MediaStore.Files.getContentUri("external"),
//                                        PROJECTION_ALL,
//                                        SELECTION_ALL,
//                                        SELECTION_ALL_ARGS,
//                                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
//                                break;
//                            case XPickerConstant.TYPE_IMAGE:
//                                cursorLoader = new CursorLoader(
//                                        activity, MediaStore.Files.getContentUri("external"),
//                                        IMAGE_PROJECTION,
//                                        isGif ? CONDITION_GIF : SELECTION_IMAGE,
//                                        null,
//                                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
//                                break;
//                            case XPickerConstant.TYPE_VIDEO:
//                                cursorLoader = new CursorLoader(
//                                        activity, MediaStore.Files.getContentUri("external"),
//                                        VIDEO_PROJECTION,
//                                        SELECTION_VIDEO,
//                                        null,
//                                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
//                                break;
//                        }
//                        return cursorLoader;
//                    }
//
//                    @Override
//                    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//                        List<MediaFolder> imageFolders = new ArrayList<>();
//                        try {
//                            MediaFolder allImageFolder = new MediaFolder();
//                            List<MediaEntity> latelyImages = new ArrayList<>();
//                            if (data != null) {
//                                int count = data.getCount();
//                                if (count > 0) {
//                                    data.moveToFirst();
//                                    do {
//                                        String path = data.getString
//                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
//                                        // 如原图路径不存在或者路径存在但文件不存在,就结束当前循环
//                                        if (TextUtils.isEmpty(path) || !new File(path).exists()
//                                        ) {
//                                            continue;
//                                        }
//                                        String mimeType = data.getString
//                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
//                                        boolean eqImg = mimeType.startsWith(XPickerConstant.IMAGE);
//                                        int duration = eqImg ? 0 : data.getInt
//                                                (data.getColumnIndexOrThrow(VIDEO_PROJECTION[7]));
//                                        int w = eqImg ? data.getInt
//                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[4])) : 0;
//                                        int h = eqImg ? data.getInt
//                                                (data.getColumnIndexOrThrow(IMAGE_PROJECTION[5])) : 0;
//
//                                        int fileType = 0;
//                                        if (mimeType.startsWith(XPickerConstant.AUDIO)) {
//                                            fileType = MimeType.ofAudio();
//                                        } else if (mimeType.startsWith(XPickerConstant.IMAGE)) {
//                                            fileType = MimeType.ofImage();
//                                        } else if (mimeType.startsWith(XPickerConstant.VIDEO)) {
//                                            fileType = MimeType.ofVideo();
//                                        }
//
//                                        MediaEntity image = MediaEntity.newBuilder()
//                                                .localPath(path)
//                                                .duration(duration)
//                                                .fileType(fileType)
//                                                .mimeType(mimeType)
//                                                .width(w)
//                                                .height(h)
//                                                .build();
//
//                                        MediaFolder folder = getImageFolder(path, imageFolders);
//                                        List<MediaEntity> images = folder.getImages();
//                                        images.add(image);
//                                        folder.setImageNum(folder.getImageNum() + 1);
//                                        latelyImages.add(image);
//                                        int imageNum = allImageFolder.getImageNum();
//                                        allImageFolder.setImageNum(imageNum + 1);
//                                    } while (data.moveToNext());
//
//                                    if (latelyImages.size() > 0) {
//                                        sortFolder(imageFolders);
//                                        imageFolders.add(0, allImageFolder);
//                                        allImageFolder.setFirstImagePath
//                                                (latelyImages.get(0).getLocalPath());
//                                        String title = type == MimeType.ofAudio() ?
//                                                activity.getString(R.string.picture_all_audio)
//                                                : activity.getString(R.string.picture_camera_roll);
//                                        allImageFolder.setName(title);
//                                        allImageFolder.setImages(latelyImages);
//                                    }
//                                }
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } finally {
//                            imageLoadListener.loadComplete(imageFolders);
//                        }
//                    }
//
//                    @Override
//                    public void onLoaderReset(Loader<Cursor> loader) {
//                    }
//                });
//    }
//
//    private void sortFolder(List<MediaFolder> imageFolders) {
//        // 文件夹按图片数量排序
//        Collections.sort(imageFolders, new Comparator<MediaFolder>() {
//            @Override
//            public int compare(MediaFolder lhs, MediaFolder rhs) {
//                if (lhs.getImages() == null || rhs.getImages() == null) {
//                    return 0;
//                }
//                int lsize = lhs.getImageNum();
//                int rsize = rhs.getImageNum();
//                return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
//            }
//        });
//    }
//
//    private MediaFolder getImageFolder(String path, List<MediaFolder> imageFolders) {
//        File imageFile = new File(path);
//        File folderFile = imageFile.getParentFile();
//
//        for (MediaFolder folder : imageFolders) {
//            if (folder.getName().equals(folderFile.getName())) {
//                return folder;
//            }
//        }
//        MediaFolder newFolder = new MediaFolder();
//        newFolder.setName(folderFile.getName());
//        newFolder.setPath(folderFile.getAbsolutePath());
//        newFolder.setFirstImagePath(path);
//        imageFolders.add(newFolder);
//        return newFolder;
//    }
//
//    public interface LocalMediaLoadListener {
//        void loadComplete(List<MediaFolder> folders);
//    }
//}
