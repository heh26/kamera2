package com.example.ipcamrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.ipcamrecorder.ptz.OnvifClient
import com.example.ipcamrecorder.ui.JoystickView
import com.example.ipcamrecorder.storage.HomePositionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

import com.example.ipcamrecorder.storage.SafStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private var recordingFile: File? = null
    private var safTreeUri: Uri? = null
    private var onvif: OnvifClient? = null
    private var homePan = 0.0
    private var homeTilt = 0.0
    private var homeZoom = 0.0

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var homeStore: HomePositionStore

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        // handle runtime permissions gracefully
    }

    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            safTreeUri = it
            findViewById<TextView>(R.id.tv_sd_folder).text = "SD: selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.preview_texture)
        val btnConnect: Button = findViewById(R.id.btn_connect)
        val btnRecord: Button = findViewById(R.id.btn_record)
        val btnConfig: Button = findViewById(R.id.btn_config)

        val btnPtzUp: Button = findViewById(R.id.btn_ptz_up)
        val btnPtzDown: Button = findViewById(R.id.btn_ptz_down)
        val btnPtzLeft: Button = findViewById(R.id.btn_ptz_left)
        val btnPtzRight: Button = findViewById(R.id.btn_ptz_right)
        val btnPtzHome: Button = findViewById(R.id.btn_ptz_home)
        val seekZoom: SeekBar = findViewById(R.id.seek_zoom)

        if (!hasPermissions()) {
            requestPermissions.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }

        btnConnect.setOnClickListener {
            // For demo: we won't attach ExoPlayer to TextureView; in production attach player surface
            // just show TextureView as placeholder; in real app you would use PlayerView/Surface
        }

        btnConfig.setOnClickListener {
            // choose SD folder
            openDocumentTree.launch(null)
        }

        btnRecord.setOnClickListener {
            if (recordingFile == null) {
                val out = createOutputFile()
                recordingFile = out
                // If SAF selected, create file on SD card and record into it (FFmpeg output path -> content URI not directly supported)
                // Simpler approach: record to app external files then copy to SAF folder after closing recording (implemented below)
                startRecordingToFile("rtsp://192.168.1.100:554/stream", out.absolutePath)
                btnRecord.text = "Stop Record"
            } else {
                stopRecording()
                btnRecord.text = "Start Record"
                // if SAF folder chosen, copy to SAF
                safTreeUri?.let { tree ->
                    recordingFile?.let { file ->
                        val newUri = SafStorage.createFileInTree(contentResolver, tree, file.name, "video/mp4")
                        newUri?.let { uri ->
                            contentResolver.openOutputStream(uri)?.use { outStream ->
                                file.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                        }
                    }
                }
                recordingFile = null
            }
        }

        // PTZ buttons: send small relative moves via Onvif or HTTP fallback
        btnPtzUp.setOnClickListener { sendRelativePtz(0.0, 0.2) }
        btnPtzDown.setOnClickListener { sendRelativePtz(0.0, -0.2) }
        btnPtzLeft.setOnClickListener { sendRelativePtz(-0.2, 0.0) }
        btnPtzRight.setOnClickListener { sendRelativePtz(0.2, 0.0) }
        btnPtzHome.setOnClickListener { sendGoHome() }

        // Zoom slider (100..400% mapped from seek progress 100..400)
        seekZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoomFactor = progress / 100.0f
                applyDigitalZoom(zoomFactor.toDouble())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Initialize ONVIF client placeholder (replace with actual endpoint + credentials from config screen)
        onvif = OnvifClient("http://192.168.1.100:80/onvif/device_service", "admin", "admin")

        // initialize HomePositionStore and load stored home position
        homeStore = HomePositionStore(this)
        scope.launch {
            val home = homeStore.homePositionFlow.first()
            homePan = home.pan
            homeTilt = home.tilt
            homeZoom = home.zoom
        }

        // wire joystick
        val joystick: JoystickView? = findViewById(R.id.joystick)
        joystick?.setOnMoveListener { x, y, mag ->
            // x, y normalized - send relative PTZ moves scaled by magnitude
            if (mag > 0.05f) {
                sendRelativePtz(x.toDouble() * 0.15, y.toDouble() * 0.15)
            } else {
                // small deadzone
            }
        }

        // long-press HOME button to save current pan/tilt/zoom as HOME
        findViewById<Button>(R.id.btn_ptz_home).setOnLongClickListener {
            // persist current known homePan/homeTilt/homeZoom (could be read from camera GetStatus in real device)
            scope.launch {
                homeStore.saveHome(com.example.ipcamrecorder.storage.HomePosition(homePan, homeTilt, homeZoom))
            }
            true
        }
    }

    private fun applyDigitalZoom(zoom: Double) {
        // apply scale and translate to center the zoom on TextureView
        val w = textureView.width.toFloat()
        val h = textureView.height.toFloat()
        val matrix = Matrix()
        val scale = zoom.toFloat().coerceAtLeast(1.0f)
        val dx = (w - w * scale) / 2f
        val dy = (h - h * scale) / 2f
        matrix.setScale(scale, scale, w / 2f, h / 2f)
        matrix.postTranslate(dx, dy)
        textureView.setTransform(matrix)
        textureView.invalidate()
    }

    private fun sendRelativePtz(x: Double, y: Double) {
        // try ONVIF first, otherwise you can call HTTP fallback commands per camera model
        try {
            onvif?.continuousMove(x, y, 0.0)
        } catch (e: Exception) {
            // HTTP fallback - implement per camera model (e.g., http://ip/ptz?move=left)
        }
    }

    private fun sendGoHome() {
        // perform absolute move to homePan/homeTilt/homeZoom
        try {
            onvif?.absoluteMove(homePan, homeTilt, homeZoom)
        } catch (e: Exception) {
            // HTTP fallback
        }
    }

    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun createOutputFile(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val folder = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "recording_$ts.mp4")
    }

    private fun startRecordingToFile(rtspUrl: String, outPath: String) {
        val cmd = arrayOf("-rtsp_transport", "tcp", "-i", rtspUrl, "-c", "copy", "-f", "mp4", outPath)
        FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
            // handle session logs and completion
        }
    }

    private fun stopRecording() {
        FFmpegKit.cancel()
    }
}
