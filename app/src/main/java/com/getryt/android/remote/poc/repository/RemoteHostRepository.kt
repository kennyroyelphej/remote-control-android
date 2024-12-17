package com.getryt.android.remote.poc.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import com.getryt.android.remote.poc.model.ServiceIntent
import com.getryt.android.remote.poc.service.RemoteHostService
import javax.inject.Inject

class RemoteHostRepository @Inject constructor(
    private val context: Context
) {
    private fun issueCommand(action: ServiceIntent, data: HashMap<String, String>) {
        val thread = Thread {
            val startIntent = Intent(context, RemoteHostService::class.java)
            startIntent.action = action.value
            data.forEach { (key, value) -> startIntent.putExtra(key, value) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
        thread.start()
    }

    fun initializeSession(sessionId: String, target: String) {
        val data = HashMap<String, String>()
        data["sessionId"] = sessionId
        data["target"] = target
        issueCommand(
            ServiceIntent.ACTION_INITIALIZE_SESSION,
            data
        )
    }
}