package com.getryt.android.remote.poc.utils

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class RemoteSdpObserver: SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}

    override fun onSetSuccess() {}

    override fun onCreateFailure(p0: String?) {}

    override fun onSetFailure(p0: String?) {}
}