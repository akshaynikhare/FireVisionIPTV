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

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

/**
 * Presenter for channel items in the overlay grid
 */
public class ChannelOverlayPresenter extends Presenter {
    private static final String TAG = "ChannelOverlayPresenter";
    private static final int CARD_WIDTH = 240;
    private static final int CARD_HEIGHT = 135;
    private static final int CARD_CORNER_RADIUS = 12;
    private static final float SCALE_SELECTED = 1.12f;
    private static final float SCALE_DEFAULT = 1.0f;
    private static final int ANIMATION_DURATION = 200;

    private Drawable mDefaultImage;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_overlay, parent, false);

        mDefaultImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_channel_placeholder);

        ViewHolder viewHolder = new ViewHolder(view);
        setupFocusHandling(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Movie channel = (Movie) item;
        View cardView = viewHolder.view;
        ImageView channelImage = cardView.findViewById(R.id.channel_image);
        TextView channelName = cardView.findViewById(R.id.channel_name);
        View focusOverlay = cardView.findViewById(R.id.focus_overlay);

        // Set channel name
        if (channelName != null) {
            channelName.setText(channel.getTitle());
        }

        // Load channel image with Glide
        if (channelImage != null) {
            if (channel.getCardImageUrl() != null && !channel.getCardImageUrl().isEmpty()) {
                RequestOptions requestOptions = new RequestOptions()
                        .centerCrop()
                        .transform(new RoundedCorners(CARD_CORNER_RADIUS))
                        .error(PlaceholderHelper.createTextPlaceholder(cardView.getContext(), channel.getTitle()));

                Glide.with(cardView.getContext())
                        .load(channel.getCardImageUrl())
                        .apply(requestOptions)
                        .into(channelImage);
            } else {
                // No image URL, use text placeholder directly
                channelImage.setImageDrawable(PlaceholderHelper.createTextPlaceholder(cardView.getContext(), channel.getTitle()));
            }
        }

        // Set rounded corners on the card
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(CARD_CORNER_RADIUS);
        background.setColor(ContextCompat.getColor(cardView.getContext(),
                R.color.overlay_card_background));
        cardView.setBackground(background);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Clean up
        ImageView channelImage = viewHolder.view.findViewById(R.id.channel_image);
        if (channelImage != null) {
            channelImage.setImageDrawable(null);
        }
    }

    private void setupFocusHandling(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        View focusOverlay = view.findViewById(R.id.focus_overlay);

        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                animateFocusChange(v, focusOverlay, hasFocus);
            }
        });
    }

    private void animateFocusChange(View cardView, View focusOverlay, boolean hasFocus) {
        float targetScale = hasFocus ? SCALE_SELECTED : SCALE_DEFAULT;
        float targetAlpha = hasFocus ? 1.0f : 0.0f;

        // Scale animation
        AnimatorSet scaleAnimatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardView, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardView, "scaleY", targetScale);
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
        ObjectAnimator elevationAnimator = ObjectAnimator.ofFloat(cardView, "elevation", targetElevation);
        elevationAnimator.setDuration(ANIMATION_DURATION);
        elevationAnimator.start();
    }
}
