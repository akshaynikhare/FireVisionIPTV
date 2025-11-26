package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.cadnative.firevisioniptv.update.UpdateManager;


/*
 * Main Activity class that loads {@link MainFragment} with Netflix-style sidebar.
 */
public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private UpdateManager updateManager;

    private View sidebarHome;
    private View sidebarSearch;
    private View sidebarCategories;
    private View sidebarFavorites;
    private View sidebarSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Check if TV code is configured, if not redirect to pairing
        if (!isTvCodeConfigured()) {
            Intent pairingIntent = new Intent(this, PairingActivity.class);
            startActivity(pairingIntent);
            finish();
            return;
        }

        // Handle deep link
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            handleDeepLink(intent.getData());
        }

        // Initialize update manager
        updateManager = new UpdateManager(this);

        // Check for updates on startup (silently, no dialog if no update)
        updateManager.checkForUpdates(false);

        // Schedule channel recommendations
        // Temporarily disabled due to permission issues
        // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        //     RecommendationService.scheduleRecommendationUpdate(this);
        // }

        // Initialize sidebar
        setupSidebar();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commitNow();

            // Check for auto-load channel
            checkAndPlayAutoloadChannel();
        }

        // Set home as selected initially
        if (sidebarHome != null) {
            sidebarHome.setSelected(true);
            sidebarHome.requestFocus();
        }
    }

    private void checkAndPlayAutoloadChannel() {
        String autoloadChannelId = SettingsActivity.getAutoloadChannelId(this);
        if (autoloadChannelId != null && !autoloadChannelId.isEmpty()) {
            // Delay to let MainFragment load channels first
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                playChannelById(autoloadChannelId);
            }, 2000); // 2 second delay
        }
    }

    private void playChannelById(String channelId) {
        // This will be triggered after channels are loaded
        // The MainFragment will handle the actual playback
        Log.d(TAG, "Auto-playing channel: " + channelId);
        // Send broadcast to MainFragment to play the channel
        android.content.Intent intent = new android.content.Intent("com.cadnative.firevisioniptv.AUTOPLAY_CHANNEL");
        intent.putExtra("channel_id", channelId);
        sendBroadcast(intent);
    }

    private void setupSidebar() {
        sidebarHome = findViewById(R.id.sidebar_home);
        sidebarSearch = findViewById(R.id.sidebar_search);
        sidebarCategories = findViewById(R.id.sidebar_categories);
        sidebarFavorites = findViewById(R.id.sidebar_favorites);
        sidebarSettings = findViewById(R.id.sidebar_settings);

        // Home - Already showing MainFragment
        sidebarHome.setOnClickListener(v -> {
            selectSidebarItem(sidebarHome);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commit();
        });

        // Search
        sidebarSearch.setOnClickListener(v -> {
            selectSidebarItem(sidebarSearch);
            // Load CustomSearchFragment in the main content area
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new CustomSearchFragment())
                    .commit();
        });

        // Categories - Same as home for now
        sidebarCategories.setOnClickListener(v -> {
            selectSidebarItem(sidebarCategories);
            // Refresh MainFragment
            MainFragment mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, mainFragment)
                    .commit();
        });

        // Favorites - Filter to show only favorites
        sidebarFavorites.setOnClickListener(v -> {
            selectSidebarItem(sidebarFavorites);
            // Create MainFragment that will show favorites
            MainFragment favFragment = MainFragment.newInstanceForFavorites();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, favFragment)
                    .commit();
        });

        // Settings
        sidebarSettings.setOnClickListener(v -> {
            selectSidebarItem(sidebarSettings);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set up focus change listeners for visual feedback
        setupFocusListener(sidebarHome);
        setupFocusListener(sidebarSearch);
        setupFocusListener(sidebarCategories);
        setupFocusListener(sidebarFavorites);
        setupFocusListener(sidebarSettings);
    }

    private void setupFocusListener(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            // Don't change selection state on focus - let click handlers manage it
            // Just update visual feedback if needed
        });
    }

    private void selectSidebarItem(View selectedItem) {
        // Deselect all
        sidebarHome.setSelected(false);
        sidebarSearch.setSelected(false);
        sidebarCategories.setSelected(false);
        sidebarFavorites.setSelected(false);
        sidebarSettings.setSelected(false);

        // Select the clicked item
        selectedItem.setSelected(true);
    }

    private void handleDeepLink(android.net.Uri data) {
        if (data.getHost().equals("play") && data.getPathSegments().size() >= 2) {
            if (data.getPathSegments().get(0).equals("movie")) {
                String movieId = data.getPathSegments().get(1);
                playChannelById(movieId);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // If we're on CustomSearchFragment or SearchFragment, go back to MainFragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_browse_fragment);
        if (currentFragment instanceof CustomSearchFragment || currentFragment instanceof SearchFragment) {
            sidebarHome.setSelected(true);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commit();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Check if TV code is configured (not empty and not default)
     * Used to determine if pairing is needed
     */
    private boolean isTvCodeConfigured() {
        String tvCode = SettingsActivity.getTvCode(this);
        // Consider configured if not empty and not the default demo code
        return tvCode != null && !tvCode.isEmpty() && !tvCode.equals("5T6FEP");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateManager != null) {
            updateManager.cleanup();
        }
    }
}