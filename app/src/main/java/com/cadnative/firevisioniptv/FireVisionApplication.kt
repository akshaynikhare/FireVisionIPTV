package com.cadnative.firevisioniptv

import android.app.Application
import com.cadnative.firevisioniptv.worker.WorkManagerInitializer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FireVisionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        WorkManagerInitializer.scheduleChannelSync(this)
    }

    companion object {
        private lateinit var instance: FireVisionApplication

        @JvmStatic
        fun getAppContext() = instance.applicationContext
    }
}
