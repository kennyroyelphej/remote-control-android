package com.getryt.android.remote.poc.repository

import android.content.Intent
import android.util.Log
import com.getryt.android.remote.poc.model.DataModel
import com.getryt.android.remote.poc.model.DataModelType
import com.getryt.android.remote.poc.model.RTCIceCandidateInit
import com.getryt.android.remote.poc.utils.RemotePeerObserver
import com.getryt.android.remote.poc.utils.SocketClient
import com.getryt.android.remote.poc.utils.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import javax.inject.Inject

class WebRTCRepository @Inject constructor(
    private val gson: Gson,
    private val socketClient: SocketClient,
    private val webRTCClient: WebRTCClient,
) : SocketClient.Listener, WebRTCClient.Listener {
    private lateinit var sessionId: String
    private lateinit var target: String
    var listener: Listener? = null

    fun requestSession(sessionId: String, target: String) {
        this.sessionId = sessionId
        this.target = target
        socketClient.listener = this
        webRTCClient.listener = this
        socketClient.initializeSocketClient(sessionId)
        webRTCClient.initializeWebRTCClient(sessionId, target, object : RemotePeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                p0?.let {
                    webRTCClient.sendIceCandidate(p0, this@WebRTCRepository.target)
                }
            }
        })
    }

    fun setPermissionIntent(intent: Intent, resultCode: Int) {
        webRTCClient.setPermissionIntent(intent, resultCode)
    }

    override fun onNewMessageReceived(data: DataModel) {
        Log.d(TAG, "onNewMessageReceived: $data")
        when (data.type) {
            DataModelType.SignIn -> {}
            DataModelType.Offer -> {}
            DataModelType.Answer -> {
                webRTCClient.addRemoteAnswer(SessionDescription(
                    SessionDescription.Type.ANSWER,
                    data.data.toString()
                ))
            }
            DataModelType.IceCandidates -> {
                val parsedCandidate = try {
                    gson.fromJson(data.data.toString(), RTCIceCandidateInit::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                parsedCandidate?.let {
                    webRTCClient.addIceCandidate(IceCandidate(
                        parsedCandidate.sdpMid,
                        parsedCandidate.sdpMLineIndex,
                        parsedCandidate.candidate
                    ))
                }
            }
            DataModelType.StartStreaming -> {}
            DataModelType.EndCall -> {}
            null -> {
                Log.e(TAG, "onNewMessageReceived: INVALID TYPE ERROR")
            }
        }
    }

    override fun onTransferEventToSocket(data: DataModel) {
        socketClient.sendMessage(data)
    }

    interface Listener {
        fun onConnectionRequestReceived(target: String)
        fun onConnectionConnected()
        fun onCallEndReceived()
        fun onRemoteStreamAdded(stream: MediaStream)
    }

    companion object {
        private val TAG = WebRTCRepository::class.simpleName
    }
}