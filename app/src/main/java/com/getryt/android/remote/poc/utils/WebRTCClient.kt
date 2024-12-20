package com.getryt.android.remote.poc.utils

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.getryt.android.remote.poc.model.DataModel
import com.getryt.android.remote.poc.model.DataModelType
import com.google.gson.Gson
import org.webrtc.CapturerObserver
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.Observer
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoFrame
import org.webrtc.VideoTrack
import javax.inject.Inject

class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    var listener: Listener? = null
    private lateinit var sessionId: String
    private lateinit var observer: Observer
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("69129037dd365448de9ce440")
            .setPassword("I0l8v+eSlzpedOs7")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("69129037dd365448de9ce440")
            .setPassword("I0l8v+eSlzpedOs7")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("69129037dd365448de9ce440")
            .setPassword("I0l8v+eSlzpedOs7")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername("69129037dd365448de9ce440")
            .setPassword("I0l8v+eSlzpedOs7")
            .createIceServer()
    )
    private var permissionIntent: Intent? = null
    private var resultCode: Int? = null
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnection: PeerConnection? = null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localVideoTrack: VideoTrack? = null
    private var localStream: MediaStream? = null

    init { initializePeerConnectionFactory(context) }

    fun initializeWebRTCClient(sessionId: String, target: String, observer: Observer) {
        this.sessionId = sessionId
        this.observer = observer
        peerConnection = createPeerConnection(observer)
        sendOffer(target)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder().setVideoDecoderFactory(
            DefaultVideoDecoderFactory(eglBaseContext)
        ).setVideoEncoderFactory(
            DefaultVideoEncoderFactory(
                eglBaseContext, true, true
            )
        ).setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }).createPeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnection(observer: Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(
            iceServer, observer
        )
    }

    fun setPermissionIntent(intent: Intent, resultCode: Int) {
        this.permissionIntent = intent
        this.resultCode = resultCode
    }

    private fun sendOffer(target: String) {
        peerConnection?.createOffer(object : RemoteSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(this, desc)
                listener?.onTransferEventToSocket(
                    DataModel(
                        type = DataModelType.Offer,
                        sessionId = sessionId,
                        target = target,
                        data = desc?.description
                    )
                )
            }
        }, mediaConstraint)
    }

    fun addRemoteAnswer(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(RemoteSdpObserver(), sessionDescription)
        startScreenCapturing()
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(candidate: IceCandidate, target: String){
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidates,
                sessionId = sessionId,
                target = target,
                data = gson.toJson(candidate)
            )
        )
    }

    private fun startScreenCapturing() {
        Handler(Looper.getMainLooper()).post {
            val eglBase = EglBase.create()
            surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase.eglBaseContext)
            if (surfaceTextureHelper == null) {
                Log.e("ScreenCapture", "Failed to create SurfaceTextureHelper")
                return@post
            }
            screenCapturer = ScreenCapturerAndroid(
                permissionIntent,
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        Log.d(TAG, "MediaProjection stopped")
                        screenCapturer?.stopCapture()
                        surfaceTextureHelper?.stopListening()
                        surfaceTextureHelper?.dispose()
                        surfaceTextureHelper = null
                        screenCapturer = null
                    }
                }
            )
            val (screenWidthPixels, screenHeightPixels, densityDpi) = getDisplayMetrics()
            val videoSource = peerConnectionFactory.createVideoSource(screenCapturer!!.isScreencast)
            Log.d(TAG, "Video source created: $videoSource")
            screenCapturer?.initialize(surfaceTextureHelper, context, object : CapturerObserver {
                override fun onCapturerStarted(p0: Boolean) {
                    Log.d(TAG, "Screen capturer started: $p0")
                }
                override fun onCapturerStopped() {
                    Log.d(TAG, "Screen capturer stopped.")
                }
                override fun onFrameCaptured(p0: VideoFrame?) {
                    Log.d(TAG, "Frame captured: width=${p0?.buffer?.width}, height=${p0?.buffer?.height}")
                }
            })
            screenCapturer?.startCapture(screenWidthPixels, screenHeightPixels, 30)
            localVideoTrack = peerConnectionFactory.createVideoTrack("SCREEN_VIDEO_TRACK", videoSource)
            Log.d(TAG, "Local video track created: ${localVideoTrack?.id()}")
            localStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")
            Log.d(TAG, "Local media stream created: ${localStream?.id}")
            localStream?.addTrack(localVideoTrack)
            Log.d(TAG, "local track added")
            peerConnection?.addStream(localStream)
            Log.d(TAG, "local stream added")
            debugPeerConnectionStats()
        }
    }

    private fun getDisplayMetrics(): Triple<Int, Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            Triple(bounds.width(), bounds.height(), displayMetrics.densityDpi)
        } else {
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            Triple(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi)
        }
    }

    private fun debugPeerConnectionStats() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                peerConnection?.getStats { report ->
                    Log.d(TAG, "debugPeerConnectionStats: $report")
                    for (stat in report.statsMap.values) {
                        if (stat.type == "outbound-rtp" && stat.members["kind"] == "video") {
                            val bitrate = stat.members["bytesSent"]?.toString()?.toLongOrNull()
                            val packetsSent = stat.members["packetsSent"]?.toString()?.toLongOrNull()
                            Log.d("WebRTC Stats", "Video Track Bitrate: $bitrate bytes")
                            Log.d("WebRTC Stats", "Packets Sent: $packetsSent")
                        }
                    }
                }
                Handler(Looper.getMainLooper()).postDelayed(this, 5000)
            }
        }, 5000)
    }

    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }

    companion object {
        private val TAG = WebRTCClient::class.simpleName
    }
}