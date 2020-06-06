![Image](./art/guide.png)

## XPicker

[ ![Download](https://api.bintray.com/packages/wenwen/maven/xpicker/images/download.svg) ](https://bintray.com/wenwen/maven/xpicker/_latestVersion)

|            Picker             |            Preview            |             Crop              |
| :---------------------------: | :---------------------------: | :---------------------------: |
| <img src="./art/demo1.jpeg"/> | <img src="./art/demo2.jpeg"/> | <img src="./art/demo3.jpeg"/> |

### XPicker is  a great way  to selector local image and video  for Android

- Please use AndrodX Lib 
- Select images including Image Video GIF 
- No delay of the mediaLoader
- TakePhoto and record video by custom camera 

### Download

```gradle
repositories {
    jcenter()
}

dependencies {
    implementation 'com.zhihu.android:matisse:$latest_version'
}
```

### USE

#### 1. configuration imageLoad

```kotlin
XPicker.imageLoadListener = { imageUri, iv, mineType ->
            Glide.with(this@MyApp)
                .load(imageUri)
                .into(iv)
      }
```

#### 2.request permission

```text
 Manifest.permission.WRITE_EXTERNAL_STORAGE
 Manifest.permission.CAMERA
 Manifest.permission.RECORD_AUDIO
 Manifest.permission.READ_EXTERNAL_STORAGE
```

#### 3.Action

```kotlin
 XPicker.ofCamera()
        .captureMode(CaptureType.MIXED)
        .start(this,CameraSaveCallback)

  XPicker.ofCrop()
         .circleCrop(true)
         .start(this,CropCallback)

  XPicker.ofPicker()
         .mineType(MineType.TYPE_ALL)
         .start(this,SelectedCallback)

```

### ProGuard

```text
 //statusBar Lib
 -keep class com.gyf.immersionbar.* {*;}
 -dontwarn com.gyf.immersionbar.**
 //UCrop
 -dontwarn com.yalantis.ucrop**
 -keep class com.yalantis.ucrop** { *; }
 -keep interface com.yalantis.ucrop** { *; }
```

### Thanks
[Luban](https://github.com/Curzibn/Luban) provide easy compress lib

[uCrop](https://github.com/Yalantis/uCrop) provide an  image cropping experience

[immersionbar](https://github.com/gyf-dev/ImmersionBar) provide an  statusbar experience

### License

    Copyright 2020 WangShuwen.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.