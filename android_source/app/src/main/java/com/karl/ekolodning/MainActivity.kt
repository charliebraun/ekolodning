package com.karl.ekolodning

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var logTextView: TextView
    private lateinit var logScrollView: ScrollView

    private var logUri: Uri? = null
    private var logFile: File? = null
    private var outputStream: OutputStream? = null

    private var loggingActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logTextView = findViewById(R.id.logTextView)
        logScrollView = findViewById(R.id.logScrollView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<Button>(R.id.startButton).setOnClickListener {
            startLogging()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopLogging()
        }
    }

    private fun appendToLogView(text: String) {
        this@MainActivity.runOnUiThread {
            logTextView.append(text)
            logScrollView.post {
                logScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun startLogging() {
        if (loggingActive) return

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        try {
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "series_${formatter.format(Date())}.log"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                val collection = MediaStore.Files.getContentUri("external")
                logUri = this@MainActivity.contentResolver.insert(collection, values)
                outputStream = logUri?.let { this@MainActivity.contentResolver.openOutputStream(it, "wa") }
            } else {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!docsDir.exists()) docsDir.mkdirs()
                logFile = File(docsDir, filename)
                outputStream = FileOutputStream(logFile, true)
            }

            val startMsg = "GPS series started: ${Date()}\n"
            writeToStream(startMsg)
            
            logTextView.text = "" // Clear previous logs
            appendToLogView(startMsg)

            loggingActive = true
            startGps()
            
        } catch (e: Exception) {
            Log.e("GPS_LOGGER", "Error starting log", e)
            appendToLogView("Error starting log: ${e.message}\n")
        }
    }

    private fun checkPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val storage = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineLocation && storage
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
    }

    private fun startGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    if (!loggingActive) continue

                    val line = String.format(
                        Locale.US,
                        "Time: %s, Long: %.6f, Lat: %.6f, Acc: %.1fm\n",
                        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(location.time)),
                        location.longitude,
                        location.latitude,
                        location.accuracy
                    )

                    Log.d("GPS_LOGGER", line)
                    writeToStream(line)
                    appendToLogView(line)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun writeToStream(text: String) {
        try {
            outputStream?.write(text.toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("GPS_LOGGER", "Write error", e)
        }
    }

    private fun stopLogging() {
        if (!loggingActive) return
        loggingActive = false

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        val stopMsg = "GPS series stopped: ${Date()}\n"
        writeToStream(stopMsg)
        appendToLogView(stopMsg)

        try {
            outputStream?.close()
        } catch (e: Exception) {
            Log.e("GPS_LOGGER", "Close error", e)
        }
        outputStream = null
        
        val path = logFile?.absolutePath ?: logUri?.toString() ?: "unknown"
        Log.d("GPS_LOGGER", "Logging stopped. Saved to: $path")
        appendToLogView("Saved to: $path\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (loggingActive) {
            stopLogging()
        }
    }
}
