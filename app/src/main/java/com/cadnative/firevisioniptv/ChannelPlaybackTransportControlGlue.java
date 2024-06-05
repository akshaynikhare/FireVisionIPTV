package com.cadnative.firevisioniptv;

import android.content.Context;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

public class ChannelPlaybackTransportControlGlue<T extends androidx.leanback.media.PlayerAdapter> extends PlaybackTransportControlGlue<T> {
    private Action mSkipNextAction;
    private Action mSkipPreviousAction;

    private PlaybackVideoFragment mFragment;

    public ChannelPlaybackTransportControlGlue(Context context, T impl, PlaybackVideoFragment fragment) {
        super(context, impl);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        mFragment = fragment;
    }
    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        super.onCreatePrimaryActions(primaryActionsAdapter);
        primaryActionsAdapter.add(mSkipPreviousAction);
        primaryActionsAdapter.add(mSkipNextAction);
    }


    public void next() {
        mFragment.nextChannel();
    }

    public void previous() {
        mFragment.previousChannel();
    }


    @Override
    public void onActionClicked(Action action) {
        if (action == mSkipNextAction) {
            next();
        } else if (action == mSkipPreviousAction) {
            previous();
        } else {
            super.onActionClicked(action);
        }
    }
}