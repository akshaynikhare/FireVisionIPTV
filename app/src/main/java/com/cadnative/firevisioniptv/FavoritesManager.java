package com.cadnative.firevisioniptv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manager class for handling favorite channels
 */
public class FavoritesManager {
    private static final String PREFS_NAME = "FireVisionFavorites";
    private static final String FAVORITES_KEY = "favorite_channels";

    private static FavoritesManager instance;
    private SharedPreferences prefs;
    private Set<String> favorites;

    private FavoritesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFavorites();
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    private void loadFavorites() {
        favorites = new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
    }

    private void saveFavorites() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(FAVORITES_KEY, favorites);
        editor.apply();
    }

    /**
     * Add a channel to favorites
     */
    public void addFavorite(String channelId) {
        favorites.add(channelId);
        saveFavorites();
    }

    /**
     * Remove a channel from favorites
     */
    public void removeFavorite(String channelId) {
        favorites.remove(channelId);
        saveFavorites();
    }

    /**
     * Toggle favorite status of a channel
     */
    public boolean toggleFavorite(String channelId) {
        if (isFavorite(channelId)) {
            removeFavorite(channelId);
            return false;
        } else {
            addFavorite(channelId);
            return true;
        }
    }

    /**
     * Check if a channel is a favorite
     */
    public boolean isFavorite(String channelId) {
        return favorites.contains(channelId);
    }

    /**
     * Get all favorite channel IDs
     */
    public Set<String> getAllFavorites() {
        return new HashSet<>(favorites);
    }

    /**
     * Clear all favorites
     */
    public void clearAllFavorites() {
        favorites.clear();
        saveFavorites();
    }
}
