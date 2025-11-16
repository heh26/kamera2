package com.example.ipcamrecorder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple camera configuration screen:
 * - Add camera (ip, rtsp url, onvif endpoint, username/password)
 * - Choose SD-card folder (SAF)
 * - Test connection (attempt to play a short preview using ExoPlayer)
 *
 * For brevity this is a skeleton; in a real app persist config via DataStore or Room.
 */

class CameraConfigActivity : AppCompatActivity() {
    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            // Persist URI permission if needed
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.simple_list_item_1)

        val btnChooseSd: Button = Button(this).apply { text = "Choose SD-folder" }
        btnChooseSd.setOnClickListener {
            openDocumentTree.launch(null)
        }
        setContentView(btnChooseSd)
    }
}
