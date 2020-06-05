package cn.cheney.xpicker.callback

import cn.cheney.xpicker.entity.MediaEntity

interface CropCallback {
    fun onCrop(mediaList: MediaEntity?)
}