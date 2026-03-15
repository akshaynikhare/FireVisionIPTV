package com.cadnative.firevisioniptv

import android.content.Context
import android.media.tv.TvInputManager
import android.media.tv.TvInputService
import android.net.Uri
import android.view.Surface

class FireVisionTvInputService : TvInputService() {

    override fun onCreateSession(inputId: String): Session {
        return FireVisionSession(this)
    }

    private inner class FireVisionSession(context: Context) : TvInputService.Session(context) {
        override fun onRelease() {}

        override fun onSetSurface(surface: Surface?): Boolean = false

        override fun onSetStreamVolume(volume: Float) {}

        override fun onTune(channelUri: Uri): Boolean {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN)
            return false
        }

        override fun onSetCaptionEnabled(enabled: Boolean) {}
    }
}
