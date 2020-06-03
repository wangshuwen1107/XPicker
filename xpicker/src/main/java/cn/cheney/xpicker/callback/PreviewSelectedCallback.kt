package cn.cheney.xpicker.callback

import cn.cheney.xpicker.entity.MediaEntity

interface PreviewSelectedCallback {

    fun onSelected(mediaList: List<MediaEntity>?, isOrigin: Boolean)

    fun onCancel(mediaList: List<MediaEntity>, isOrigin: Boolean)

}