package space.taran.arkfilepicker.sample

import android.app.Application
import space.taran.arkfilepicker.ArkFilePicker

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        ArkFilePicker.init(this)
    }
}