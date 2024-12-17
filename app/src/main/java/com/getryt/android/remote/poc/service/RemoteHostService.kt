package com.getryt.android.remote.poc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.getryt.android.remote.poc.R
import com.getryt.android.remote.poc.model.ServiceIntent
import com.getryt.android.remote.poc.repository.WebRTCRepository
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.MediaStream
import javax.inject.Inject

@AndroidEntryPoint
class RemoteHostService @Inject constructor(): Service(), WebRTCRepository.Listener {
    @Inject lateinit var webRTCRepository: WebRTCRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var sessionId: String
    private lateinit var target: String

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when(intent.action) {
                ServiceIntent.ACTION_INITIALIZE_SESSION.value -> {
                    startService()
                    this.sessionId = intent.getStringExtra("sessionId").toString()
                    this.target = intent.getStringExtra("target").toString()
                    webRTCRepository.requestSession(this.sessionId, this.target)
                    webRTCRepository.setPermissionIntent(screenPermissionIntent!!, resultCode!!)
                }
            }
        }
        return START_STICKY
    }

    private fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channelId = "getryt-remote-host-channel"
            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.getryt_foreground)
                .setContentTitle("Remote Host")
                .setContentText("Remote Host Service is running")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            val notificationChannel = NotificationChannel(
                channelId,
                "Remote Host Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
            startForeground(1, notification)
        }
    }

    private fun stopService() {
        stopSelf()
        notificationManager.cancelAll()
    }

    companion object {
        private val TAG = RemoteHostService::class.simpleName
        var screenPermissionIntent: Intent? = null
        var resultCode: Int? = null
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