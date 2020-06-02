package cn.cheney.xpicker.callback

import cn.cheney.xpicker.entity.MediaEntity

interface SelectedCallback {
    fun onSelected(mediaList: List<MediaEntity>?)
}