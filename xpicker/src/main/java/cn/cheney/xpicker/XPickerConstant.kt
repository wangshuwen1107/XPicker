package cn.cheney.xpicker

import androidx.annotation.StringDef

class XPickerConstant {

    companion object {
        const val REQUEST_KEY = "pickerRequest"
        const val PREVIEW_DATA_KEY = "previewList"
        const val PREVIEW_INDEX_KEY = "previewIndex"
        const val PREVIEW_CURRENT_MAX_NUM_KEY = "previewMaxNum"
        const val PREVIEW_ORIGINAL_KEY = "original"

        //FileType
        const val FILE_TYPE_VIDEO = 2
        const val FILE_TYPE_IMAGE = 3
        const val GIF = "image/gif"

        //compress
        const val COMPRESS_DIR_TAG = "luban"
        const val CROP_DIR_TAG = "crop"
    }
}
