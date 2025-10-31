package com.cadnative.firevisioniptv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Overlay fragment for browsing channels during playback
 * Appears when BACK button is pressed, allows navigation with LEFT/RIGHT
 */
public class ChannelOverlayFragment extends Fragment {
    private static final String TAG = "ChannelOverlayFragment";
    private static final int AUTO_HIDE_DELAY_MS = 5000;
    private static final int ANIMATION_DURATION_MS = 250;

    private View mOverlayContainer;
    private HorizontalGridView mChannelGrid;
    private TextView mCategoryTitle;
    private ChannelOverlayAdapter mAdapter;
    private List<Movie> mChannels;
    private int mCurrentChannelIndex;
    private OnChannelSelectedListener mListener;
    private Handler mAutoHideHandler;
    private Runnable mAutoHideRunnable;
    private boolean mIsOverlayVisible = false;

    public interface OnChannelSelectedListener {
        void onChannelSelected(Movie channel, int position);
        void onOverlayDismissed();
        void onBackPressedWhenVisible(); // New callback for going back to channel list
    }

    public static ChannelOverlayFragment newInstance(int currentChannelIndex) {
        ChannelOverlayFragment fragment = new ChannelOverlayFragment();
        Bundle args = new Bundle();
        args.putInt("current_index", currentChannelIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentChannelIndex = getArguments().getInt("current_index", 0);
        }
        mChannels = MovieList.list;
        mAutoHideHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel_overlay, container, false);

        mOverlayContainer = root.findViewById(R.id.overlay_container);
        mChannelGrid = root.findViewById(R.id.channel_grid);
        mCategoryTitle = root.findViewById(R.id.category_title);

        setupChannelGrid();
        setupAutoHide();

        // Initially hidden
        mOverlayContainer.setAlpha(0f);
        mOverlayContainer.setVisibility(View.GONE);

        return root;
    }

    private void setupChannelGrid() {
        mAdapter = new ChannelOverlayAdapter(mChannels, new ChannelOverlayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Movie channel, int position) {
                if (mListener != null) {
                    mListener.onChannelSelected(channel, position);
                    hide();
                }
            }
        });
        mChannelGrid.setAdapter(mAdapter);

        // Set the selected position to current channel
        mChannelGrid.setSelectedPosition(mCurrentChannelIndex);

        // Handle channel selection
        mChannelGrid.setOnChildViewHolderSelectedListener(
                new OnChildViewHolderSelectedListener() {
                    @Override
                    public void onChildViewHolderSelected(@NonNull RecyclerView parent,
                                                          @Nullable RecyclerView.ViewHolder child,
                                                          int position, int subposition) {
                        if (position >= 0 && position < mChannels.size()) {
                            Movie selectedChannel = mChannels.get(position);
                            updateCategoryTitle(selectedChannel);
                            resetAutoHideTimer();
                        }
                    }
                });

        // Initial category title
        if (mCurrentChannelIndex >= 0 && mCurrentChannelIndex < mChannels.size()) {
            updateCategoryTitle(mChannels.get(mCurrentChannelIndex));
        }
    }

    private void updateCategoryTitle(Movie channel) {
        if (mCategoryTitle != null && channel != null) {
            String category = channel.getGroup();
            if (category == null || category.isEmpty()) {
                category = "Channels";
            }
            mCategoryTitle.setText(category);
        }
    }

    private void setupAutoHide() {
        mAutoHideRunnable = new Runnable() {
            @Override
            public void run() {
                hide();
            }
        };
    }

    public void show() {
        if (mIsOverlayVisible) return;

        mIsOverlayVisible = true;
        mOverlayContainer.setVisibility(View.VISIBLE);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mOverlayContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(ANIMATION_DURATION_MS);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();

        // Request focus on the grid
        mChannelGrid.requestFocus();

        resetAutoHideTimer();
    }

    public void hide() {
        if (!mIsOverlayVisible) return;

        cancelAutoHideTimer();

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mOverlayContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(ANIMATION_DURATION_MS);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOverlayContainer.setVisibility(View.GONE);
                mIsOverlayVisible = false;
                if (mListener != null) {
                    mListener.onOverlayDismissed();
                }
            }
        });
        fadeOut.start();
    }

    public void toggle() {
        if (mIsOverlayVisible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isOverlayVisible() {
        return mIsOverlayVisible;
    }

    private void resetAutoHideTimer() {
        cancelAutoHideTimer();
        mAutoHideHandler.postDelayed(mAutoHideRunnable, AUTO_HIDE_DELAY_MS);
    }

    private void cancelAutoHideTimer() {
        mAutoHideHandler.removeCallbacks(mAutoHideRunnable);
    }

    public boolean handleKeyEvent(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (!mIsOverlayVisible) {
            // If overlay is hidden, only respond to BACK to show it
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                show();
                return true;
            }
            return false;
        }

        // Overlay is visible, handle navigation
        resetAutoHideTimer();

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Second BACK press when overlay is visible - go back to channel list
                if (mListener != null) {
                    mListener.onBackPressedWhenVisible();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                // Let the grid handle left navigation
                return false;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_CHANNEL_UP:
                // Let the grid handle right navigation
                return false;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // Select current channel
                int position = mChannelGrid.getSelectedPosition();
                if (position >= 0 && position < mChannels.size() && mListener != null) {
                    Movie selectedChannel = mChannels.get(position);
                    mListener.onChannelSelected(selectedChannel, position);
                    hide();
                }
                return true;

            default:
                return false;
        }
    }

    public void setOnChannelSelectedListener(OnChannelSelectedListener listener) {
        mListener = listener;
    }

    public void updateCurrentChannel(int index) {
        mCurrentChannelIndex = index;
        if (mChannelGrid != null && index >= 0 && index < mChannels.size()) {
            mChannelGrid.setSelectedPosition(index);
            updateCategoryTitle(mChannels.get(index));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelAutoHideTimer();
    }
}
