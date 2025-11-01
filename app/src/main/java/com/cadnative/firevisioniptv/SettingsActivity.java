package com.cadnative.firevisioniptv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

/**
 * Settings Activity for server configuration
 */
public class SettingsActivity extends FragmentActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "FireVisionSettings";
    private static final String SERVER_URL_KEY = "server_url";
    private static final String AUTOLOAD_CHANNEL_ID_KEY = "autoload_channel_id";
    private static final String AUTOLOAD_CHANNEL_NAME_KEY = "autoload_channel_name";
    private static final String DEFAULT_SERVER_URL = "https://tv.cadnative.com";

    private EditText serverUrlInput;
    private TextView currentServerInfo;
    private TextView autoloadChannelInfo;
    private View saveButton;
    private View clearAutoloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        serverUrlInput = findViewById(R.id.server_url_input);
        currentServerInfo = findViewById(R.id.current_server_info);
        autoloadChannelInfo = findViewById(R.id.autoload_channel_info);
        saveButton = findViewById(R.id.save_button);
        clearAutoloadButton = findViewById(R.id.clear_autoload_button);

        // Initialize default URL if not set
        initializeDefaultUrl();

        // Load current server URL
        loadCurrentServerUrl();

        // Load auto-load channel info
        loadAutoloadChannelInfo();

        // Setup save button
        saveButton.setOnClickListener(v -> saveServerUrl());

        // Setup clear auto-load button
        clearAutoloadButton.setOnClickListener(v -> clearAutoloadChannel());
    }

    private void initializeDefaultUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUrl = prefs.getString(SERVER_URL_KEY, "");

        if (currentUrl.isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SERVER_URL_KEY, DEFAULT_SERVER_URL);
            editor.apply();
        }
    }

    private void loadCurrentServerUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUrl = prefs.getString(SERVER_URL_KEY, "");

        if (!currentUrl.isEmpty()) {
            serverUrlInput.setText(currentUrl);
            currentServerInfo.setText("Current server: " + currentUrl);
        } else {
            currentServerInfo.setText("Current server: Not set");
        }
    }

    private void saveServerUrl() {
        String url = serverUrlInput.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate URL format (basic validation)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SERVER_URL_KEY, url);
        editor.apply();

        // Update current server info
        currentServerInfo.setText("Current server: " + url);

        Toast.makeText(this, "Server URL saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadAutoloadChannelInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String channelName = prefs.getString(AUTOLOAD_CHANNEL_NAME_KEY, "");

        if (!channelName.isEmpty()) {
            autoloadChannelInfo.setText("Auto-load: " + channelName);
        } else {
            autoloadChannelInfo.setText("No channel set");
        }
    }

    private void clearAutoloadChannel() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(AUTOLOAD_CHANNEL_ID_KEY);
        editor.remove(AUTOLOAD_CHANNEL_NAME_KEY);
        editor.apply();

        autoloadChannelInfo.setText("No channel set");
        Toast.makeText(this, "Auto-load channel cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Static method to get server URL from SharedPreferences
     */
    public static String getServerUrl(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(SERVER_URL_KEY, "");
    }

    /**
     * Static method to set auto-load channel
     */
    public static void setAutoloadChannel(android.content.Context context, String channelId, String channelName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AUTOLOAD_CHANNEL_ID_KEY, channelId);
        editor.putString(AUTOLOAD_CHANNEL_NAME_KEY, channelName);
        editor.apply();
    }

    /**
     * Static method to get auto-load channel ID
     */
    public static String getAutoloadChannelId(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(AUTOLOAD_CHANNEL_ID_KEY, "");
    }
}
