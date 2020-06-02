package cn.cheney.xpicker

class XPickerConstant {

    companion object {
        const val REQUEST_KEY = "pickerRequest"
        const val PREVIEW_DATA_KEY = "previewList"
        const val PREVIEW_INDEX_KEY = "previewIndex"
        const val PREVIEW_CURRENT_MAX_NUM_KEY = "previewMaxNum"
        //拍照类型
        const val ONLY_CAPTURE = "ONLY_CAPTURE"
        const val ONLY_RECORDER = "ONLY_RECORDER"
        const val MIXED = "MIXED"
        //拍照 or 选择器
        const val CAMERA = "CAMERA"
        const val PICKER = "PICKER"

        //MediaType
        const val TYPE_ALL = 0
        const val TYPE_VIDEO = 1
        const val TYPE_IMAGE = 2

    }
}