package cn.cheney.xpicker

class XPickerConstant {

    companion object {
        const val REQUEST_KEY = "pickerRequest"
        const val PREVIEW_DATA_KEY = "previewList"
        const val PREVIEW_INDEX_KEY = "previewIndex"
        const val PREVIEW_CURRENT_MAX_NUM_KEY = "previewMaxNum"
        const val PREVIEW_ORIGINAL_KEY = "original"
        //拍照类型
        const val ONLY_CAPTURE = "ONLY_CAPTURE"
        const val ONLY_RECORDER = "ONLY_RECORDER"
        const val MIXED = "MIXED"

        //拍照 or 选择器 or 头像裁剪
        const val CAMERA = "CAMERA"
        const val PICKER = "PICKER"
        const val CROP = "CROP"
        //MineType
        const val TYPE_ALL = 0
        const val TYPE_ALL_WITHOUT_GIF = 1
        const val TYPE_VIDEO = 2
        const val TYPE_IMAGE = 3
        const val TYPE_IMAGE_WITHOUT_GIF = 4
        //mineType
        const val GIF = "image/gif"
        //compress
        const val  COMPRESS_DIR_TAG="luban"
        const val  CROP_DIR_TAG="crop"
    }
}