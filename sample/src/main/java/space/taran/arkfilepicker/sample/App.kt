package dev.arkbuilders.arkfilepicker.sample

import android.app.Application
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        FoldersRepo.init(this)
    }
}