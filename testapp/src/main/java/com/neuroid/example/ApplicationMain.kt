package com.neuroid.example

import android.app.Application
import com.neuroid.tracker.NeuroID

class ApplicationMain : Application() {
    companion object {
        val dataManager = DataManager()
        var isApplicationSubmitted = false
        var isLoggedIn = false

        var registeredSessionId = 1
        var registeredUserID = 1
        var registeredUserName = "regUser-"
        var sessionName = "sessionName-"
    }

    fun resetAll() {
        // userEnteredId = ""
        isApplicationSubmitted = false
        isLoggedIn = false
    }

    override fun onCreate() {
        super.onCreate()
        NeuroID.Builder(
            this,
            "key_live_MwC5DQNYzRsRhnnYjvz1fJtp",
            isAdvancedDevice = true,
            serverEnvironment = NeuroID.TEST
        ).build()
        NeuroID.getInstance()?.setVerifyIntegrationHealth(true)
    }

    fun setUserID(userId: String) {
        NeuroID.getInstance()?.identify(userId)
    }

    fun stopTracking() {
        NeuroID.getInstance()?.stopSession()
    }

    override fun onTerminate() {
        NeuroID.getInstance()?.stop()
        super.onTerminate()
    }

}
