package com.cadnative.firevisioniptv;

import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import java.util.List;

public class PlaybackVideoFragment extends VideoSupportFragment {

    private ChannelPlaybackTransportControlGlue<MediaPlayerAdapter> mTransportControlGlue;
    private List<Movie> mChannels; // List of channels (movies in this case)
    private int mCurrentChannelIndex;
    private ChannelOverlayFragment mOverlayFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Movie movie = (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);

        // Get the list of channels (movies) and find the current one
        mChannels = MovieList.list;
        mCurrentChannelIndex = mChannels.indexOf(movie);

        VideoSupportFragmentGlueHost glueHost = new VideoSupportFragmentGlueHost(PlaybackVideoFragment.this);

        MediaPlayerAdapter playerAdapter = new MediaPlayerAdapter(getContext());
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE);

        mTransportControlGlue = new ChannelPlaybackTransportControlGlue<>(getContext(), playerAdapter, this);
        mTransportControlGlue.setHost(glueHost);
        updateChannelInfo(movie);

        // Keep the screen on while playing the video
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.setAttributes(params);

        // Initialize overlay fragment
        setupOverlay();
    }

    private void setupOverlay() {
        mOverlayFragment = ChannelOverlayFragment.newInstance(mCurrentChannelIndex);
        mOverlayFragment.setOnChannelSelectedListener(new ChannelOverlayFragment.OnChannelSelectedListener() {
            @Override
            public void onChannelSelected(Movie channel, int position) {
                mCurrentChannelIndex = position;
                updateChannelInfo(channel);
            }

            @Override
            public void onOverlayDismissed() {
                // Overlay dismissed, resume normal playback controls
            }

            @Override
            public void onBackPressedWhenVisible() {
                // Second BACK press - go back to channel list
                getActivity().finish();
            }
        });

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.overlay_fragment_container, mOverlayFragment)
                .commit();
    }

    private void updateChannelInfo(Movie movie) {
        mTransportControlGlue.setTitle(movie.getTitle());
        mTransportControlGlue.setSubtitle(movie.getDescription());
        mTransportControlGlue.getPlayerAdapter().setDataSource(Uri.parse(movie.getVideoUrl()));
        mTransportControlGlue.playWhenPrepared();
    }

    public void nextChannel() {
        mCurrentChannelIndex = (mCurrentChannelIndex + 1) % mChannels.size();
        updateChannelInfo(mChannels.get(mCurrentChannelIndex));

        // Update overlay if it exists
        if (mOverlayFragment != null) {
            mOverlayFragment.updateCurrentChannel(mCurrentChannelIndex);
        }
    }

    public void previousChannel() {
        mCurrentChannelIndex = (mCurrentChannelIndex - 1 + mChannels.size()) % mChannels.size();
        updateChannelInfo(mChannels.get(mCurrentChannelIndex));

        // Update overlay if it exists
        if (mOverlayFragment != null) {
            mOverlayFragment.updateCurrentChannel(mCurrentChannelIndex);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // First, check if overlay should handle the key event
        if (mOverlayFragment != null && mOverlayFragment.handleKeyEvent(keyCode, event)) {
            return true;
        }

        // If overlay is visible, don't handle navigation keys (overlay will handle them)
        if (mOverlayFragment != null && mOverlayFragment.isOverlayVisible()) {
            return false;
        }

        // Handle channel navigation when overlay is not visible
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Show overlay on BACK button
                if (mOverlayFragment != null) {
                    mOverlayFragment.show();
                    return true;
                }
                return false;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                nextChannel();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                previousChannel();
                return true;

            default:
                return false;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mTransportControlGlue != null) {
            mTransportControlGlue.pause();
        }

        // Allow the device to sleep when the fragment is paused
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.setAttributes(params);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTransportControlGlue != null) {
            mTransportControlGlue = null;
        }

        // Allow the device to sleep when the fragment is destroyed
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.setAttributes(params);
    }

    // Additional helper methods for PlaybackActivity
    public void togglePlayPause() {
        if (mTransportControlGlue != null) {
            if (mTransportControlGlue.isPlaying()) {
                mTransportControlGlue.pause();
            } else {
                mTransportControlGlue.play();
            }
        }
    }

    public void play() {
        if (mTransportControlGlue != null) {
            mTransportControlGlue.play();
        }
    }

    public void pause() {
        if (mTransportControlGlue != null) {
            mTransportControlGlue.pause();
        }
    }

    public void showOverlay() {
        if (mOverlayFragment != null) {
            mOverlayFragment.show();
        }
    }
}