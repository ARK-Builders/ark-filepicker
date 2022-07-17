# ARK-FilePicker
FilePicker component, shared between several ARK apps

# Usage

```kotlin
// Define config
val config = ArkFilePickerConfig(
    mode = ArkFilePickerMode.FOLDER,
    initialPath = somePath,
    titleStringId = R.string.file_picker_title,
    ..
)

// Add listeners via Fragment Result API
supportFragmentManager.onArkPathPicked(lifecycleOwner = this) { path ->
    ..
}
supportFragmentManager.onArkFolderChange(lifecycleOwner = this) { path ->
    ..
}

// Show
ArkFilePickerFragment
    .newInstance(config)
    .show(supportFragmentManager, null)
```

# Install
Add the jitpack.io repository in the project build.gradle or settings.gradle(v7+)
```kotlin
allprojects {
    ..
    repositories {
        ..
        maven { url 'https://jitpack.io' }
    }
}

or 

dependencyResolutionManagement {
    ..
    repositories {
        ..
        maven { url 'https://jitpack.io' }
    }
}
```

And then add the file picker dependency to your module
```kotlin
implementation 'com.github.ark-builders:ark-filepicker:main-SNAPSHOT'
```
