![Image](./art/guide.png)

## XPicker


|            Picker             |            Preview            |
| :---------------------------: | :---------------------------: |
| <img src="./art/demo1.jpeg"/> | <img src="./art/demo2.jpeg"/> |


### XPicker is  a great way  to selector local image and video  for Android

- 支持筛选图片(PNG,JPEG),动图GIF,视频
- 支持图片和视频的拍摄
- 支持图片的裁剪选择


### 最新版本

|  模块 | camera2  |  xpicker |
| ------------ | ------------ | ------------|
| 最新版本 |![Maven Central](https://img.shields.io/maven-central/v/io.github.wangshuwen1107/camera2) | ![Maven Central](https://img.shields.io/maven-central/v/io.github.wangshuwen1107/xpicker)|

### Download

```gradle
repositories {
   mavenCentral()
}

dependencies {
    implementation "io.github.wangshuwen1107:camera2:$latest_version"
    implementation "io.github.wangshuwen1107:xpicker:$latest_version"
}
```

### USE

#### 1. AndroidManifest配置

由于picker内置视频预览功能，且改功能是跳转到系统or三方APP播放视频，需要配置FlieProvider

```xml
<provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths_public">
            </meta-data>
  </provider>
```

在app/res/xml配置file_paths_public.xml

```xml
<paths>
    <external-path
        name="xpicker_external_name"
        path="." />
</paths>
```

#### 2. configuration imageLoad

```kotlin
XPicker.imageLoadListener = { imageUri, iv, mineType ->
            Glide.with(this@MyApp)
                .load(imageUri)
                .into(iv)
      }
```

#### 3.request permission

```text
Manifest.permission.WRITE_EXTERNAL_STORAGE
 Manifest.permission.CAMERA
 Manifest.permission.RECORD_AUDIO
 Manifest.permission.READ_EXTERNAL_STORAGE
```

#### 4.Action

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

### Thanks

- [Luban](https://github.com/Curzibn/Luban) provide easy compress lib
- [uCrop](https://github.com/Yalantis/uCrop) provide an  image cropping experience
- [immersionbar](https://github.com/gyf-dev/ImmersionBar) provide an  statusbar experience

### License

```
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
```