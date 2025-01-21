package com.getryt.android.remote.poc.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.getryt.android.remote.poc.databinding.ActivityMainBinding
import com.getryt.android.remote.poc.repository.RemoteHostRepository
import com.getryt.android.remote.poc.repository.WebRTCRepository
import com.getryt.android.remote.poc.service.RemoteHostService
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.MediaStream
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), WebRTCRepository.Listener {
    @Inject lateinit var remoteHostRepository: RemoteHostRepository
    private lateinit var binding: ActivityMainBinding
    private val screenCaptureLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                RemoteHostService.screenPermissionIntent = result.data
                RemoteHostService.resultCode = result.resultCode
                remoteHostRepository.initializeSession(sessionId, target)
            }
        }
    private var notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    private var sessionId: String = "a91c981f-2ee8-4cdf-b497-389f3b11e398"
    private var target: String = "a91c981f-2ee8-4cdf-b497-389f3b11e399"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setViewStateListeners()
        askNotificationPermission()
        askCapturePermission()
    }

    private fun setViewStateListeners() {
        binding.run {
            btnStartSession.setOnClickListener {
                val mediaProjectionManager: MediaProjectionManager =
                    this@MainActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                screenCaptureLauncher.launch(captureIntent)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun askCapturePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
                ),
                0
            )
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    override fun onConnectionRequestReceived(target: String) {
        TODO("Not yet implemented")
    }

    override fun onConnectionConnected() {
        TODO("Not yet implemented")
    }

    override fun onCallEndReceived() {
        TODO("Not yet implemented")
    }

    override fun onRemoteStreamAdded(stream: MediaStream) {
        TODO("Not yet implemented")
    }
}