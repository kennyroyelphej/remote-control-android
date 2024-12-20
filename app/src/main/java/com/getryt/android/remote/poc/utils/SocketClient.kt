package com.getryt.android.remote.poc.utils

import com.getryt.android.remote.poc.model.DataModel
import com.getryt.android.remote.poc.model.DataModelType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketClient @Inject constructor(
    private val gson: Gson
) {
    var listener: Listener? = null
    private var sessionId: String? = null

    fun initializeSocketClient(sessionId: String) {
        this.sessionId = sessionId
        webSocket = object : WebSocketClient(URI("ws://192.168.1.14:3000")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessage(
                    DataModel(
                        type = DataModelType.SignIn,
                        sessionId = sessionId,
                        target = null, data = null
                    )
                )
            }
            override fun onMessage(message: String?) {
                val model = try {
                    gson.fromJson(message.toString(), DataModel::class.java)
                } catch (ex: Exception) {
                    null
                }
                model?.let { data -> listener?.onNewMessageReceived(data) }
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000)
                    initializeSocketClient(sessionId)
                }
            }
            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
            }
        }
        webSocket?.connect()
    }
    fun sendMessage(message: Any?) {
        try {
            webSocket?.send(gson.toJson(message))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    interface Listener {
        fun onNewMessageReceived(data: DataModel)
    }
    companion object {
        private var webSocket: WebSocketClient? = null
    }
}