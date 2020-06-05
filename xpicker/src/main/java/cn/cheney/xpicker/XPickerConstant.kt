package cn.cheney.xpicker

import androidx.annotation.StringDef

class XPickerConstant {

    companion object {
        const val REQUEST_KEY = "pickerRequest"
        const val REQUEST_BUNDLE_KEY = "pickerBundle"
        const val PREVIEW_DATA_KEY = "previewList"
        const val PREVIEW_INDEX_KEY = "previewIndex"
        const val PREVIEW_CURRENT_MAX_NUM_KEY = "previewMaxNum"
        const val PREVIEW_ORIGINAL_KEY = "original"

        //FileProvider
        const val FILE_PROVIDER = "cn.cheney.xpicker.fileprovider"

        //FileType
        const val FILE_TYPE_VIDEO = 1
        const val FILE_TYPE_IMAGE = 2

        //dir
        const val COMPRESS_DIR_TAG = "luban"
        const val CROP_DIR_TAG = "crop"

        //RealMineType
        const val GIF = "image/gif"
        const val JPEG = "image/jpeg"
        const val PNG = "image/png"
        const val WEBP = "image/webp"
        const val MP4 = "video/mp4"


    }
}
