package com.cadnative.firevisioniptv;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.fragment.app.FragmentActivity;

/**
 * Loads {@link PlaybackVideoFragment} and handles key events for channel navigation.
 * Now supports overlay channel browser for better UX.
 */
public class PlaybackActivity extends FragmentActivity {

    private PlaybackVideoFragment mPlaybackVideoFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First, let the fragment handle the key event (for overlay)
        if (mPlaybackVideoFragment != null && mPlaybackVideoFragment.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}