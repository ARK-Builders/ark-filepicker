# ARK-FilePicker
FilePicker component, shared between several ARK apps

## Importing the library
**Github packages with credentials is a workaround since JCenter is shutdown**

Add the following script to project's `build.gradle`:

```groovy
allprojects {
    repositories{
        maven {
            name = "GitHubPackages"
            url = "https://maven.pkg.github.com/ARK-Builders/ark-filepicker"
            credentials {
                username = "token"
                password = "\u0037\u0066\u0066\u0036\u0030\u0039\u0033\u0066\u0032\u0037\u0033\u0036\u0033\u0037\u0064\u0036\u0037\u0066\u0038\u0030\u0034\u0039\u0062\u0030\u0039\u0038\u0039\u0038\u0066\u0034\u0066\u0034\u0031\u0064\u0062\u0033\u0064\u0033\u0038\u0065"
            }
        }
    }
}
```

And add `arklib-android` dependency to app module's `build.gradle`:
```groovy
implementation 'dev.arkbuilders:arkfilepicker:0.1.0'
```

## Usage

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
