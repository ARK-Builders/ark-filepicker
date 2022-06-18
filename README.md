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
