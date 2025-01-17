package com.sample.neuroid.us

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroID
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
                .build(),
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )

        NeuroID.Builder(this, "key_live_MwC5DQNYzRsRhnnYjvz1fJtp",
            isAdvancedDevice = true, serverEnvironment = NeuroID.PRODSCRIPT_DEVCOLLECTION
        ).build()
        // NeuroID.getInstance()?.start()
        // NeuroID.getInstance()?.setEnvironmentProduction(true)
        // NeuroID.getInstance()?.setSiteId(configHelper.formId)
        NeuroID.getInstance()?.startAppFlow("form_parks912", null)
        // NeuroID.getInstance()?.startAppFlow("form_skein469", "testSession")
        // NeuroID.getInstance()?.startSession("hasdklghs")
        NeuroID.getInstance()?.identify("fgdsgasdgsdg")
    }
}
