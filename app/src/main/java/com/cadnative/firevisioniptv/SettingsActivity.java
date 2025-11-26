package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Settings Activity for server configuration
 */
public class SettingsActivity extends FragmentActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "FireVisionSettings";
    private static final String SERVER_URL_KEY = "server_url";
    private static final String TV_CODE_KEY = "tv_code";
    private static final String AUTOLOAD_CHANNEL_ID_KEY = "autoload_channel_id";
    private static final String AUTOLOAD_CHANNEL_NAME_KEY = "autoload_channel_name";
    private static final String DEFAULT_SERVER_URL = "https://tv.cadnative.com";
    private static final String DEFAULT_TV_CODE = "5T6FEP";

    private SidebarManager sidebarManager;
    private EditText serverUrlInput;
    private EditText tvCodeInput;
    private TextView currentServerInfo;
    private TextView autoloadChannelInfo;
    private ImageView qrCodeImageView;
    private View saveButton;
    private View pairDeviceButton;
    private View clearAutoloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_new);

        // Initialize sidebar with SidebarManager
        sidebarManager = new SidebarManager(this);
        sidebarManager.setup(SidebarManager.SidebarItem.SETTINGS);

        serverUrlInput = findViewById(R.id.server_url_input);
        tvCodeInput = findViewById(R.id.tv_code_input);
        currentServerInfo = findViewById(R.id.current_server_info);
        autoloadChannelInfo = findViewById(R.id.autoload_channel_info);
        qrCodeImageView = findViewById(R.id.qr_code_image);
        saveButton = findViewById(R.id.save_button);
        pairDeviceButton = findViewById(R.id.pair_device_button);
        clearAutoloadButton = findViewById(R.id.clear_autoload_button);

        // Initialize defaults if not set
        initializeDefaults();

        // Load current server URL
        loadCurrentServerUrl();

        // Load current TV code
        loadCurrentTvCode();

        // Load auto-load channel info
        loadAutoloadChannelInfo();

        // Generate registration QR code
        generateQRCode();

        // Setup save button
        saveButton.setOnClickListener(v -> saveSettings());

        // Setup pair device button
        pairDeviceButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PairingActivity.class);
            startActivity(intent);
        });

        // Setup clear auto-load button
        clearAutoloadButton.setOnClickListener(v -> clearAutoloadChannel());
    }

    private void initializeDefaults() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;

        String currentUrl = prefs.getString(SERVER_URL_KEY, "");
        if (currentUrl.isEmpty()) {
            editor.putString(SERVER_URL_KEY, DEFAULT_SERVER_URL);
            changed = true;
        }

        String currentTvCode = prefs.getString(TV_CODE_KEY, "");
        if (currentTvCode.isEmpty()) {
            editor.putString(TV_CODE_KEY, DEFAULT_TV_CODE);
            changed = true;
        }

        if (changed) {
            editor.apply();
        }
    }

    private void loadCurrentServerUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUrl = prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL);

        serverUrlInput.setText(currentUrl);
        currentServerInfo.setText("Current server: " + currentUrl);
    }

    private void loadCurrentTvCode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentCode = prefs.getString(TV_CODE_KEY, DEFAULT_TV_CODE);

        tvCodeInput.setText(currentCode);
    }

    private void saveSettings() {
        String url = serverUrlInput.getText().toString().trim();
        String tvCode = tvCodeInput.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tvCode.isEmpty()) {
            Toast.makeText(this, "Please enter a TV code", Toast.LENGTH_SHORT).show();
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
        editor.putString(TV_CODE_KEY, tvCode);
        editor.apply();

        // Update current server info
        currentServerInfo.setText("Current server: " + url);

        // Regenerate QR code with new values
        generateQRCode();

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
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
     * Generate QR code for user registration
     */
    private void generateQRCode() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String serverUrl = prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL);

            // Create registration URL for QR code
            String registrationUrl = serverUrl + "/user/register.html";

            String qrContent = registrationUrl;

            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Display QR code
            qrCodeImageView.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Static method to get server URL from SharedPreferences
     */
    public static String getServerUrl(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL);
    }

    /**
     * Static method to get TV code from SharedPreferences
     */
    public static String getTvCode(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(TV_CODE_KEY, DEFAULT_TV_CODE);
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
