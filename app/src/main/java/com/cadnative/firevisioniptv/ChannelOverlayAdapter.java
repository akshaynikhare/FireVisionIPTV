package com.cadnative.firevisioniptv;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

/**
 * RecyclerView adapter for the channel overlay grid
 */
public class ChannelOverlayAdapter extends RecyclerView.Adapter<ChannelOverlayAdapter.ViewHolder> {
    private static final String TAG = "ChannelOverlayAdapter";
    private static final int CARD_CORNER_RADIUS = 12;
    private static final float SCALE_SELECTED = 1.12f;
    private static final float SCALE_DEFAULT = 1.0f;
    private static final int ANIMATION_DURATION = 200;

    private List<Movie> mChannels;
    private OnItemClickListener mListener;
    private Drawable mDefaultImage;

    public interface OnItemClickListener {
        void onItemClick(Movie channel, int position);
    }

    public ChannelOverlayAdapter(List<Movie> channels, OnItemClickListener listener) {
        this.mChannels = channels;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_overlay, parent, false);

        mDefaultImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.movie);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie channel = mChannels.get(position);
        holder.bind(channel, position);
    }

    @Override
    public int getItemCount() {
        return mChannels != null ? mChannels.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView channelImage;
        private TextView channelName;
        private View focusOverlay;
        private View cardContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView;
            channelImage = itemView.findViewById(R.id.channel_image);
            channelName = itemView.findViewById(R.id.channel_name);
            focusOverlay = itemView.findViewById(R.id.focus_overlay);

            setupFocusHandling();
        }

        public void bind(Movie channel, int position) {
            // Set channel name
            if (channelName != null) {
                channelName.setText(channel.getTitle());
            }

            // Load channel icon/logo with Glide - fitCenter for clear icon display with padding
            if (channelImage != null) {
                RequestOptions requestOptions = new RequestOptions()
                        .fitCenter()
                        .error(mDefaultImage);

                Glide.with(itemView.getContext())
                        .load(channel.getCardImageUrl())
                        .apply(requestOptions)
                        .into(channelImage);
            }

            // Set up click listener
            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onItemClick(channel, position);
                }
            });
        }

        private void setupFocusHandling() {
            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                animateFocusChange(hasFocus);
            });
        }

        private void animateFocusChange(boolean hasFocus) {
            float targetScale = hasFocus ? SCALE_SELECTED : SCALE_DEFAULT;
            float targetAlpha = hasFocus ? 1.0f : 0.0f;

            // Scale animation
            AnimatorSet scaleAnimatorSet = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardContainer, "scaleX", targetScale);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardContainer, "scaleY", targetScale);
            scaleX.setDuration(ANIMATION_DURATION);
            scaleY.setDuration(ANIMATION_DURATION);
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleAnimatorSet.playTogether(scaleX, scaleY);
            scaleAnimatorSet.start();

            // Focus overlay animation
            if (focusOverlay != null) {
                ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(focusOverlay, "alpha", targetAlpha);
                alphaAnimator.setDuration(ANIMATION_DURATION);
                alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                alphaAnimator.start();
            }

            // Elevation animation for depth
            float targetElevation = hasFocus ? 12f : 4f;
            ObjectAnimator elevationAnimator = ObjectAnimator.ofFloat(cardContainer, "elevation", targetElevation);
            elevationAnimator.setDuration(ANIMATION_DURATION);
            elevationAnimator.start();
        }
    }
}
