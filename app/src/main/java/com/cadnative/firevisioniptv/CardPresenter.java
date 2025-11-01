package com.cadnative.firevisioniptv;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
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
        ImageView favoriteStar = cardView.findViewById(R.id.favorite_star);

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

        // Setup favorite star
        if (favoriteStar != null) {
            FavoritesManager favManager = FavoritesManager.getInstance(cardView.getContext());
            String channelId = String.valueOf(movie.getId());
            updateFavoriteStar(favoriteStar, favManager.isFavorite(channelId));

            // Hide star by default, show only for favorites
            favoriteStar.setVisibility(favManager.isFavorite(channelId) ? View.VISIBLE : View.GONE);
        }

        // Setup long-press to show options menu
        cardView.setOnLongClickListener(v -> {
            showChannelOptionsDialog(cardView, movie, favoriteStar);
            return true;
        });

        // Setup focus handling
        cardView.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocusChange(v, focusBorder, hasFocus);
        });
    }

    private void showChannelOptionsDialog(View cardView, Movie movie, ImageView favoriteStar) {
        FavoritesManager favManager = FavoritesManager.getInstance(cardView.getContext());
        String channelId = String.valueOf(movie.getId());
        boolean isFavorite = favManager.isFavorite(channelId);

        String[] options = {
            isFavorite ? "Remove from Favorites" : "Add to Favorites",
            "Set as Auto-load Channel"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(cardView.getContext());
        builder.setTitle(movie.getTitle())
               .setItems(options, (dialog, which) -> {
                   switch (which) {
                       case 0: // Toggle Favorite
                           boolean newFavoriteState = favManager.toggleFavorite(channelId);
                           if (favoriteStar != null) {
                               updateFavoriteStar(favoriteStar, newFavoriteState);
                               favoriteStar.setVisibility(newFavoriteState ? View.VISIBLE : View.GONE);
                           }
                           String favMessage = newFavoriteState ? "Added to Favorites" : "Removed from Favorites";
                           android.widget.Toast.makeText(cardView.getContext(), favMessage, android.widget.Toast.LENGTH_SHORT).show();
                           break;

                       case 1: // Set as Auto-load
                           SettingsActivity.setAutoloadChannel(cardView.getContext(), channelId, movie.getTitle());
                           android.widget.Toast.makeText(cardView.getContext(),
                               "Set as auto-load channel: " + movie.getTitle(),
                               android.widget.Toast.LENGTH_SHORT).show();
                           break;
                   }
               });
        builder.show();
    }

    private void updateFavoriteStar(ImageView starView, boolean isFavorite) {
        if (isFavorite) {
            starView.setImageResource(R.drawable.ic_star_small);
        } else {
            starView.setImageResource(R.drawable.ic_star_outline_small);
        }
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
