package space.taran.arkfilepicker.sample

import android.app.Application
import space.taran.arkfilepicker.folders.FoldersRepo

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        FoldersRepo.init(this)
    }
}