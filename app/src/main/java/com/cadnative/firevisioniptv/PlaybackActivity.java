package com.cadnative.firevisioniptv;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.fragment.app.FragmentActivity;

/**
 * Loads {@link PlaybackVideoFragment} and handles key events for channel navigation.
 */
public class PlaybackActivity extends FragmentActivity {

    private PlaybackVideoFragment mPlaybackVideoFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mPlaybackVideoFragment = new PlaybackVideoFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, mPlaybackVideoFragment)
                    .commit();
        } else {
            mPlaybackVideoFragment = (PlaybackVideoFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPlaybackVideoFragment != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mPlaybackVideoFragment.nextChannel();
                    return true;
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mPlaybackVideoFragment.previousChannel();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}