package com.cadnative.firevisioniptv;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;

/**
 * Loads {@link PlaybackVideoFragment} and handles key events for channel navigation.
 * Now supports overlay channel browser for better UX.
 * Enhanced with wake lock to prevent screensaver during playback.
 */
public class PlaybackActivity extends FragmentActivity {

    private PlaybackVideoFragment mPlaybackVideoFragment;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        // CRITICAL: Prevent screensaver during playback
        // Method 1: Window flag (primary method)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Method 2: Wake lock (backup method)
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
            "FireVision:PlaybackWakeLock"
        );

        if (savedInstanceState == null) {
            mPlaybackVideoFragment = new PlaybackVideoFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.playback_fragment_container, mPlaybackVideoFragment)
                    .commit();
        } else {
            mPlaybackVideoFragment = (PlaybackVideoFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.playback_fragment_container);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Acquire wake lock when activity resumes
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        // Re-add flag in case it was cleared
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Release wake lock when activity pauses
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        // Clear flag when paused
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure wake lock is released
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First, let the fragment handle the key event (for overlay)
        if (mPlaybackVideoFragment != null && mPlaybackVideoFragment.onKeyDown(keyCode, event)) {
            return true;
        }

        // Enhanced Fire TV remote key handling
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // Toggle play/pause
                if (mPlaybackVideoFragment != null) {
                    mPlaybackVideoFragment.togglePlayPause();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (mPlaybackVideoFragment != null) {
                    mPlaybackVideoFragment.play();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mPlaybackVideoFragment != null) {
                    mPlaybackVideoFragment.pause();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MENU:
                // Show channel overlay or settings
                if (mPlaybackVideoFragment != null) {
                    mPlaybackVideoFragment.showOverlay();
                    return true;
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }
}