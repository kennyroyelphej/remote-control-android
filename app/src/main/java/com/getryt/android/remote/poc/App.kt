package com.getryt.android.remote.poc

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, applicationContext.getString(R.string.app_name) + " App Initialized")
    }

    companion object {
        private val TAG = App::class.simpleName
    }
}