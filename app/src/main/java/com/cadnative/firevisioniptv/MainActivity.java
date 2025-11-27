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
    private SidebarManager sidebarManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Check if this is the first launch and TV code is not configured
        if (isFirstLaunch() && !isTvCodeConfigured()) {
            // Mark that we've launched before
            markFirstLaunchComplete();
            
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

        // Initialize sidebar with SidebarManager
        sidebarManager = new SidebarManager(this);
        sidebarManager.setup(SidebarManager.SidebarItem.HOME);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commitNow();

            // Check for auto-load channel
            checkAndPlayAutoloadChannel();
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

    /**
     * Show home fragment (called by SidebarManager)
     */
    public void showHomeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, new MainFragment())
                .commit();
    }

    /**
     * Show search fragment (called by SidebarManager)
     */
    public void showSearchFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, new CustomSearchFragment())
                .commit();
    }

    /**
     * Show categories fragment (called by SidebarManager)
     */
    public void showCategoriesFragment() {
        // Refresh MainFragment
        MainFragment mainFragment = new MainFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, mainFragment)
                .commit();
    }

    /**
     * Show favorites fragment (called by SidebarManager)
     */
    public void showFavoritesFragment() {
        // Create MainFragment that will show favorites
        MainFragment favFragment = MainFragment.newInstanceForFavorites();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, favFragment)
                .commit();
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
        // If we're on CustomSearchFragment, go back to MainFragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_browse_fragment);
        if (currentFragment instanceof CustomSearchFragment) {
            sidebarManager.selectSidebarItem(SidebarManager.SidebarItem.HOME);
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

    /**
     * Check if this is the first time the app is being launched
     */
    private boolean isFirstLaunch() {
        android.content.SharedPreferences prefs = getSharedPreferences("FireVisionSettings", MODE_PRIVATE);
        return !prefs.getBoolean("has_launched_before", false);
    }

    /**
     * Mark that the app has been launched before
     */
    private void markFirstLaunchComplete() {
        android.content.SharedPreferences prefs = getSharedPreferences("FireVisionSettings", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("has_launched_before", true);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateManager != null) {
            updateManager.cleanup();
        }
    }
}