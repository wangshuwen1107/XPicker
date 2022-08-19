package cn.cheney.xpicker.entity

enum class CameraError(val errorCode:Int,val errorMsg:String) {

    TAKE_PHOTO_ERROR(-2000,"拍摄出错"),
    TAKE_PHOTO_INIT_ERROR(-2001,"初始化失败")
}