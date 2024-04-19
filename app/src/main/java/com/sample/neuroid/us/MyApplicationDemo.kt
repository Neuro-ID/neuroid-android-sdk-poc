package com.sample.neuroid.us

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroIDImpl
import com.neuroid.tracker.extensions.setVerifyIntegrationHealth
import com.sample.neuroid.us.domain.config.ConfigHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplicationDemo : MultiDexApplication() {

    @Inject
    lateinit var configHelper: ConfigHelper

    override fun onCreate() {
        super.onCreate()
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )

        // tied to form id: form_dream102
        NeuroIDImpl.Builder(
            this,
            "key_live_suj4CX90v0un2k1ufGrbItT5",
            NeuroIDImpl.DEVELOPMENT).build()
        NeuroIDImpl.getInstance()?.setEnvironmentProduction(true)
        NeuroIDImpl.getInstance()?.setSiteId(configHelper.formId)
        NeuroIDImpl.getInstance()?.setVerifyIntegrationHealth(true)
        NeuroIDImpl.getInstance()?.setUserID(configHelper.userId)
        NeuroIDImpl.getInstance()?.setRegisteredUserID("ahsdkghasdjkghdklasglasd")
        NeuroIDImpl.getInstance()?.startSession("testSession")
    }
}