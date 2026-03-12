package com.cadnative.firevisioniptv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for FireVision IPTV.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class FireVisionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private lateinit var instance: FireVisionApplication

        @JvmStatic
        fun getAppContext() = instance.applicationContext
    }
}
