package com.cadnative.firevisioniptv;

import android.content.Context;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.view.Surface;

public class FireVisionTvInputService extends TvInputService {
    @Override
    public Session onCreateSession(String inputId) {
        return new FireVisionSession(this);
    }

    private class FireVisionSession extends TvInputService.Session {
        public FireVisionSession(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            return false;
        }

        @Override
        public void onSetStreamVolume(float volume) {
        }

        @Override
        public boolean onTune(Uri channelUri) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            return false;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // Handle captioning
        }
    }
}
