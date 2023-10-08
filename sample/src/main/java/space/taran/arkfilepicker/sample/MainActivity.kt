package dev.arkbuilders.arkfilepicker.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import dev.arkbuilders.arkfilepicker.ArkFilePickerConfig
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import dev.arkbuilders.arkfilepicker.presentation.onArkPathPicked

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resolvePermissions()

        supportFragmentManager.onArkPathPicked(this) { path ->
            Toast.makeText(this, "Path picked $path", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btn_open).setOnClickListener {
            resolvePermissions()
            ArkFilePickerFragment
                .newInstance(getFilePickerConfig())
                .show(supportFragmentManager, null)
        }

        findViewById<MaterialButton>(R.id.btn_root_picker).setOnClickListener {
            resolvePermissions()
            RootFavPickerDialog
                .newInstance()
                .show(supportFragmentManager, null)
        }
    }

    private fun getFilePickerConfig() = ArkFilePickerConfig(
        mode = ArkFilePickerMode.FOLDER,
        titleStringId = R.string.file_picker_title,
        showRoots = true,
        rootsFirstPage = false
    )

    private fun resolvePermissions() {
        if (!isReadPermGranted()) askReadPermissions()
    }

    private fun askReadPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val packageUri =
                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    packageUri
                )
            startActivityForResult(intent, REQUEST_CODE_ALL_FILES_ACCESS)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun isReadPermGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val REQUEST_CODE_ALL_FILES_ACCESS = 0
        private const val REQUEST_CODE_PERMISSIONS = 1
    }
}