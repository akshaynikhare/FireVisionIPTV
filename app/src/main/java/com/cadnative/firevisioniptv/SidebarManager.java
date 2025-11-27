package com.cadnative.firevisioniptv;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Helper class to manage Netflix-style sidebar navigation across all activities
 * Reduces code duplication and ensures consistent navigation behavior
 */
public class SidebarManager {
    
    public enum SidebarItem {
        HOME,
        SEARCH,
        CATEGORIES,
        LANGUAGES,
        FAVORITES,
        SETTINGS
    }

    private final Activity activity;
    private final View sidebarHome;
    private final View sidebarSearch;
    private final View sidebarCategories;
    private final View sidebarLanguages;
    private final View sidebarFavorites;
    private final View sidebarSettings;
    private SidebarItem currentItem;

    /**
     * Initialize sidebar manager with an activity
     * @param activity The activity containing the sidebar
     */
    public SidebarManager(Activity activity) {
        this.activity = activity;
        
        // Find sidebar views
        this.sidebarHome = activity.findViewById(R.id.sidebar_home);
        this.sidebarSearch = activity.findViewById(R.id.sidebar_search);
        this.sidebarCategories = activity.findViewById(R.id.sidebar_categories);
        this.sidebarLanguages = activity.findViewById(R.id.sidebar_languages);
        this.sidebarFavorites = activity.findViewById(R.id.sidebar_favorites);
        this.sidebarSettings = activity.findViewById(R.id.sidebar_settings);
    }

    /**
     * Setup sidebar with click listeners and navigation
     * @param currentItem The currently active sidebar item
     */
    public void setup(SidebarItem currentItem) {
        this.currentItem = currentItem;
        
        // Setup click listeners based on activity type
        if (activity instanceof MainActivity) {
            setupForMainActivity();
        } else if (activity instanceof SettingsActivity) {
            setupForSettingsActivity();
        }

        // Setup focus listeners for all items
        setupFocusListeners();
        
        // Select current item
        selectSidebarItem(currentItem);
        
        // Request focus on current item
        requestFocus(currentItem);
    }

    /**
     * Setup sidebar for MainActivity
     */
    private void setupForMainActivity() {
        MainActivity mainActivity = (MainActivity) activity;
        
        // Use both click and key listeners for better TV experience
        setupMainActivityItem(sidebarHome, SidebarItem.HOME, () -> {
            selectSidebarItem(SidebarItem.HOME);
            mainActivity.showHomeFragment();
        });

        setupMainActivityItem(sidebarSearch, SidebarItem.SEARCH, () -> {
            selectSidebarItem(SidebarItem.SEARCH);
            mainActivity.showSearchFragment();
        });

        setupMainActivityItem(sidebarCategories, SidebarItem.CATEGORIES, () -> {
            selectSidebarItem(SidebarItem.CATEGORIES);
            mainActivity.showCategoriesFragment();
        });

        setupMainActivityItem(sidebarLanguages, SidebarItem.LANGUAGES, () -> {
            selectSidebarItem(SidebarItem.LANGUAGES);
            mainActivity.showLanguagesFragment();
        });

        setupMainActivityItem(sidebarFavorites, SidebarItem.FAVORITES, () -> {
            selectSidebarItem(SidebarItem.FAVORITES);
            mainActivity.showFavoritesFragment();
        });

        setupMainActivityItem(sidebarSettings, SidebarItem.SETTINGS, () -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
        });
    }

    /**
     * Setup a sidebar item with proper key and click handling for TV
     */
    private void setupMainActivityItem(View view, SidebarItem item, Runnable action) {
        if (view == null) return;
        
        // Handle click events
        view.setOnClickListener(v -> action.run());
        
        // Handle D-pad center key press for TV
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || 
                    keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    action.run();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Setup sidebar for SettingsActivity
     */
    private void setupForSettingsActivity() {
        Runnable goToMain = () -> {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            activity.finish();
        };

        setupSettingsActivityItem(sidebarHome, goToMain);
        setupSettingsActivityItem(sidebarSearch, goToMain);
        setupSettingsActivityItem(sidebarCategories, goToMain);
        setupSettingsActivityItem(sidebarLanguages, goToMain);
        setupSettingsActivityItem(sidebarFavorites, goToMain);
        setupSettingsActivityItem(sidebarSettings, () -> {
            // Already on settings
        });
    }

    /**
     * Setup a sidebar item for SettingsActivity
     */
    private void setupSettingsActivityItem(View view, Runnable action) {
        if (view == null) return;
        
        view.setOnClickListener(v -> action.run());
        
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || 
                    keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    action.run();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Setup focus change listeners for visual feedback
     */
    private void setupFocusListeners() {
        setupFocusListener(sidebarHome, SidebarItem.HOME);
        setupFocusListener(sidebarSearch, SidebarItem.SEARCH);
        setupFocusListener(sidebarCategories, SidebarItem.CATEGORIES);
        setupFocusListener(sidebarLanguages, SidebarItem.LANGUAGES);
        setupFocusListener(sidebarFavorites, SidebarItem.FAVORITES);
        setupFocusListener(sidebarSettings, SidebarItem.SETTINGS);
    }

    /**
     * Setup focus listener for a single sidebar item
     */
    private void setupFocusListener(View view, SidebarItem item) {
        if (view != null) {
            view.setOnFocusChangeListener((v, hasFocus) -> {
                // Keep current item selected even when focus changes
                // This ensures the active section stays highlighted
            });
        }
    }

    /**
     * Select a sidebar item visually
     */
    public void selectSidebarItem(SidebarItem item) {
        this.currentItem = item;
        
        // Deselect all
        if (sidebarHome != null) sidebarHome.setSelected(false);
        if (sidebarSearch != null) sidebarSearch.setSelected(false);
        if (sidebarCategories != null) sidebarCategories.setSelected(false);
        if (sidebarLanguages != null) sidebarLanguages.setSelected(false);
        if (sidebarFavorites != null) sidebarFavorites.setSelected(false);
        if (sidebarSettings != null) sidebarSettings.setSelected(false);

        // Select the specified item
        View selectedView = getViewForItem(item);
        if (selectedView != null) {
            selectedView.setSelected(true);
        }
    }

    /**
     * Request focus on a sidebar item
     */
    public void requestFocus(SidebarItem item) {
        View view = getViewForItem(item);
        if (view != null) {
            view.post(() -> view.requestFocus());
        }
    }

    /**
     * Get the view corresponding to a sidebar item
     */
    private View getViewForItem(SidebarItem item) {
        switch (item) {
            case HOME:
                return sidebarHome;
            case SEARCH:
                return sidebarSearch;
            case CATEGORIES:
                return sidebarCategories;
            case LANGUAGES:
                return sidebarLanguages;
            case FAVORITES:
                return sidebarFavorites;
            case SETTINGS:
                return sidebarSettings;
            default:
                return null;
        }
    }

    /**
     * Get the current selected sidebar item
     */
    public SidebarItem getCurrentItem() {
        return currentItem;
    }
}
