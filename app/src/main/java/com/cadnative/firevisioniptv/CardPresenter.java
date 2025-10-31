package com.cadnative.firevisioniptv;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

/**
 * Modern CardPresenter with custom layout
 * 80% icon space, 20% name space, wider than tall
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static final float SCALE_SELECTED = 1.10f;
    private static final float SCALE_DEFAULT = 1.0f;
    private static final int ANIMATION_DURATION = 200;

    private Drawable mDefaultCardImage;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");

        mDefaultCardImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.movie);

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_card, parent, false);

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        View cardView = viewHolder.view;

        Log.d(TAG, "onBindViewHolder");

        ImageView channelImage = cardView.findViewById(R.id.channel_image);
        TextView channelName = cardView.findViewById(R.id.channel_name);
        View focusBorder = cardView.findViewById(R.id.focus_border);

        // Set channel name
        if (channelName != null) {
            channelName.setText(movie.getTitle());
        }

        // Load channel icon/logo with Glide
        if (channelImage != null && movie.getCardImageUrl() != null) {
            RequestOptions requestOptions = new RequestOptions()
                    .fitCenter()
                    .error(mDefaultCardImage);

            Glide.with(cardView.getContext())
                    .load(movie.getCardImageUrl())
                    .apply(requestOptions)
                    .into(channelImage);
        }

        // Setup focus handling
        cardView.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocusChange(v, focusBorder, hasFocus);
        });
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");

        View cardView = viewHolder.view;
        ImageView channelImage = cardView.findViewById(R.id.channel_image);

        // Remove references to images for garbage collection
        if (channelImage != null) {
            channelImage.setImageDrawable(null);
        }

        // Remove focus listener
        cardView.setOnFocusChangeListener(null);
    }

    private void animateFocusChange(View cardView, View focusBorder, boolean hasFocus) {
        float targetScale = hasFocus ? SCALE_SELECTED : SCALE_DEFAULT;
        float targetAlpha = hasFocus ? 1.0f : 0.0f;
        float targetElevation = hasFocus ? 12f : 6f;

        // Scale animation
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardView, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardView, "scaleY", targetScale);
        scaleX.setDuration(ANIMATION_DURATION);
        scaleY.setDuration(ANIMATION_DURATION);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();

        // Focus border animation
        if (focusBorder != null) {
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(focusBorder, "alpha", targetAlpha);
            alphaAnimator.setDuration(ANIMATION_DURATION);
            alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaAnimator.start();
        }

        // Elevation animation
        CardView cardViewWidget = cardView.findViewById(R.id.card_view);
        if (cardViewWidget != null) {
            ObjectAnimator elevationAnimator = ObjectAnimator.ofFloat(cardViewWidget, "cardElevation", targetElevation);
            elevationAnimator.setDuration(ANIMATION_DURATION);
            elevationAnimator.start();
        }
    }
}
