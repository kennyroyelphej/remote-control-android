package com.getryt.android.remote.poc.model

enum class DataModelType {
    SignIn,
    RequestSession,
    Offer,
    Answer,
    IceCandidates,
    StartSession,
    SessionMeta,
    EndSession
}

data class DataModel(
    val type: DataModelType? = null,
    val sessionId: String,
    val target: String?=null,
    val data: Any?=null
)

data class RTCIceCandidateInit(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val usernameFragment: String? = null
)

enum class ServiceIntent(val value: String) {
    ACTION_INITIALIZE_SESSION("INITIALIZE_SESSION"),
    ACTION_END_SESSION("END_SESSION"),
}