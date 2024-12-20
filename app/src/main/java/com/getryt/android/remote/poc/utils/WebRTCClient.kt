package com.getryt.android.remote.poc.utils

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.getryt.android.remote.poc.model.DataModel
import com.getryt.android.remote.poc.model.DataModelType
import com.google.gson.Gson
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.JavaI420Buffer
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.Observer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.VideoFrame
import org.webrtc.VideoTrack
import javax.inject.Inject

class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    var listener: Listener? = null
    private lateinit var sessionId: String
    private lateinit var target: String
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
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var localVideoTrack: VideoTrack? = null
    private var localStream: MediaStream? = null

    init { initializePeerConnectionFactory(context) }

    fun initializeWebRTCClient(sessionId: String, target: String, observer: Observer) {
        this.sessionId = sessionId
        this.target = target
        this.observer = observer
        peerConnection = createPeerConnection(observer)
        requestSession()
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

    fun setPermissionIntent(intent: Intent, resultCode: Int) {
        this.permissionIntent = intent
        this.resultCode = resultCode
    }

    private fun requestSession() {
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.RequestSession,
                sessionId = sessionId,
                target = target
            )
        )
    }

    fun handleOffer(data: DataModel) {
        Log.d(TAG, "handleOffer: REMOTE_DESC")
        Log.d(TAG, "handleOffer: ${data.data}")
        peerConnection?.setRemoteDescription(RemoteSdpObserver(), SessionDescription(
            SessionDescription.Type.OFFER,
            data.data.toString()
        ))
        peerConnection?.createAnswer(object : RemoteSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(this, desc)
                listener?.onTransferEventToSocket(
                    DataModel(
                        type = DataModelType.Answer,
                        sessionId = sessionId,
                        target = target,
                        data = desc?.description
                    )
                )
            }
            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "SDP creation failed: $p0")
            }
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "Failed to set SDP: $p0")
            }
        }, mediaConstraint)
    }

    fun startScreenCapturing() {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode!!, permissionIntent!!)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d(TAG, "onStop: Clear session")
            }
        }, null)
        val (screenWidthPixels, screenHeightPixels, densityDpi) = getDisplayMetrics()
        sendDisplayMetrics(screenWidthPixels, screenHeightPixels, densityDpi)
        val imageReader = getImageReader(screenWidthPixels, screenHeightPixels)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidthPixels,
            screenHeightPixels,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let { data ->
                sendImageToWebRTC(data)
                data.close()
            }
        }, null)
        val videoSource = peerConnectionFactory.createVideoSource(false)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)
        localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream?.addTrack(localVideoTrack)
        peerConnection?.addTrack(localVideoTrack, listOf(localStream?.id ?: "stream"))
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

    private fun getImageReader(screenWidthPixels: Int, screenHeightPixels: Int): ImageReader {
        return ImageReader.newInstance(
            screenWidthPixels,
            screenHeightPixels,
            PixelFormat.RGBA_8888,
            2
        )
    }

    private fun sendImageToWebRTC(image: Image) {
        val planes = image.planes
        val yPlane = planes[0].buffer
        val uPlane = planes[1].buffer
        val vPlane = planes[2].buffer
        val width = image.width
        val height = image.height
        val i420Y = ByteArray(yPlane.remaining())
        val i420U = ByteArray(uPlane.remaining())
        val i420V = ByteArray(vPlane.remaining())
        yPlane.get(i420Y)
        uPlane.get(i420U)
        vPlane.get(i420V)
        val i420Buffer = JavaI420Buffer.allocate(width, height)
        System.arraycopy(i420Y, 0, i420Buffer.dataY, 0, i420Y.size)
        System.arraycopy(i420U, 0, i420Buffer.dataU, 0, i420U.size)
        System.arraycopy(i420V, 0, i420Buffer.dataV, 0, i420V.size)
        val videoFrame = VideoFrame(i420Buffer, 0, System.nanoTime())
        localVideoTrack?.let { track ->
            track.addSink { videoFrame }
        }
        i420Buffer.release()
        image.close()
    }

    private fun sendDisplayMetrics(width: Int, height: Int, density: Int) {
        val displayMetrics = HashMap<String, Any>()
        displayMetrics["desc"] = "DISPLAY_METRICS"
        displayMetrics["width"] = width
        displayMetrics["height"] = height
        displayMetrics["density"] = density
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.SessionMeta,
                sessionId = sessionId,
                target = target,
                data = displayMetrics
            )
        )
    }

    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }

    companion object {
        private val TAG = WebRTCClient::class.simpleName
    }
}