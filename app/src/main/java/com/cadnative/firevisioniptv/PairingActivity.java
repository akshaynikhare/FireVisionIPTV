package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.cadnative.firevisioniptv.api.ApiClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Activity for PIN-based TV pairing
 * Displays a 6-digit PIN and polls server until user confirms pairing on web dashboard
 */
public class PairingActivity extends FragmentActivity {
    private static final String TAG = "PairingActivity";
    private static final int POLL_INTERVAL_MS = 3000; // 3 seconds
    private static final int MAX_POLL_ATTEMPTS = 200; // 10 minutes (200 * 3s)
    
    private TextView pinDisplay;
    private TextView statusMessage;
    private TextView countdownTimer;
    private TextView serverUrlDisplay;
    private ImageView signupQrCode;
    private ProgressBar loadingSpinner;
    private Button retryButton;
    private Button skipButton;
    
    private String currentPin;
    private long expiresAt;
    private Handler pollHandler;
    private Runnable pollRunnable;
    private int pollAttempts = 0;
    private boolean isPairing = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        
        pinDisplay = findViewById(R.id.pin_display);
        statusMessage = findViewById(R.id.status_message);
        countdownTimer = findViewById(R.id.countdown_timer);
        signupQrCode = findViewById(R.id.signup_qr_code);
        serverUrlDisplay = findViewById(R.id.server_url_display);
        loadingSpinner = findViewById(R.id.loading_spinner);
        retryButton = findViewById(R.id.retry_button);
        skipButton = findViewById(R.id.skip_button);
        
        // Set server URL
        String serverUrl = SettingsActivity.getServerUrl(this);
        serverUrlDisplay.setText(serverUrl);
        
        // Generate signup QR code
        generateSignupQRCode(serverUrl);
        
        // Setup buttons
        retryButton.setOnClickListener(v -> requestNewPairing());
        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(PairingActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
        
        // Start pairing process
        requestNewPairing();
    }
    
    /**
     * Generate QR code for user registration
     */
    private void generateSignupQRCode(String serverUrl) {
        new Thread(() -> {
            try {
                // Create registration URL
                String registrationUrl = serverUrl + "/user/register.html";
                
                // Generate QR code
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(registrationUrl, BarcodeFormat.QR_CODE, 512, 512);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                    }
                }

                // Display QR code on UI thread
                runOnUiThread(() -> signupQrCode.setImageBitmap(bmp));
                
            } catch (WriterException e) {
                Log.e(TAG, "Error generating signup QR code", e);
                runOnUiThread(() -> {
                    // Hide QR code section on error
                    if (signupQrCode != null) {
                        signupQrCode.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }
    
    /**
     * Request a new pairing PIN from server
     */
    private void requestNewPairing() {
        isPairing = true;
        pollAttempts = 0;
        
        // Show loading state
        loadingSpinner.setVisibility(View.VISIBLE);
        pinDisplay.setText("------");
        statusMessage.setText("Connecting to server...");
        countdownTimer.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        
        new Thread(() -> {
            try {
                String baseUrl = SettingsActivity.getServerUrl(this);
                URL url = new URL(baseUrl + "/api/v1/tv/pairing/request");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Send device info
                JSONObject requestData = new JSONObject();
                requestData.put("deviceName", Build.MODEL);
                requestData.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
                
                OutputStream os = connection.getOutputStream();
                os.write(requestData.toString().getBytes("UTF-8"));
                os.close();
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.optBoolean("success", false)) {
                        currentPin = jsonResponse.getString("pin");
                        String expiresAtStr = jsonResponse.getString("expiresAt");
                        expiresAt = parseISO8601(expiresAtStr);
                        
                        runOnUiThread(() -> {
                            loadingSpinner.setVisibility(View.GONE);
                            pinDisplay.setText(currentPin);
                            statusMessage.setText("Waiting for confirmation...");
                            countdownTimer.setVisibility(View.VISIBLE);
                            startPolling();
                            startCountdown();
                        });
                    } else {
                        showError("Failed to generate PIN: " + 
                                jsonResponse.optString("error", "Unknown error"));
                    }
                } else {
                    showError("Server error: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error requesting pairing", e);
                showError("Connection error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Start polling server to check if pairing is confirmed
     */
    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPairing || pollAttempts >= MAX_POLL_ATTEMPTS) {
                    if (pollAttempts >= MAX_POLL_ATTEMPTS) {
                        showError("Pairing timeout. Please try again.");
                    }
                    return;
                }
                
                pollAttempts++;
                checkPairingStatus();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }
    
    /**
     * Check pairing status from server
     */
    private void checkPairingStatus() {
        new Thread(() -> {
            try {
                String baseUrl = SettingsActivity.getServerUrl(this);
                URL url = new URL(baseUrl + "/api/v1/tv/pairing/status/" + currentPin);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    boolean paired = jsonResponse.optBoolean("paired", false);
                    String status = jsonResponse.optString("status", "unknown");
                    
                    if (paired && "completed".equals(status)) {
                        String channelListCode = jsonResponse.getString("channelListCode");
                        String username = jsonResponse.optString("username", "User");
                        onPairingSuccess(channelListCode, username);
                    } else if ("expired".equals(status)) {
                        showError("PIN expired. Please generate a new one.");
                    }
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error checking pairing status", e);
            }
        }).start();
    }
    
    /**
     * Handle successful pairing
     */
    private void onPairingSuccess(String channelListCode, String username) {
        isPairing = false;
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        
        runOnUiThread(() -> {
            // Save channel list code to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("FireVisionSettings", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("tv_code", channelListCode);
            editor.apply();
            
            // Show success message
            statusMessage.setText("✓ Paired successfully!");
            statusMessage.setTextColor(0xFF4CAF50); // Green
            countdownTimer.setVisibility(View.GONE);
            
            Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_LONG).show();
            
            // Wait 2 seconds then launch MainActivity
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(PairingActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, 2000);
        });
    }
    
    /**
     * Show error message
     */
    private void showError(String message) {
        runOnUiThread(() -> {
            loadingSpinner.setVisibility(View.GONE);
            statusMessage.setText(message);
            statusMessage.setTextColor(0xFFF44336); // Red
            retryButton.setVisibility(View.VISIBLE);
            countdownTimer.setVisibility(View.GONE);
            isPairing = false;
            
            if (pollHandler != null && pollRunnable != null) {
                pollHandler.removeCallbacks(pollRunnable);
            }
        });
    }
    
    /**
     * Start countdown timer showing time remaining
     */
    private void startCountdown() {
        Handler countdownHandler = new Handler(Looper.getMainLooper());
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPairing) return;
                
                long now = System.currentTimeMillis();
                long remaining = expiresAt - now;
                
                if (remaining <= 0) {
                    countdownTimer.setText("PIN Expired");
                    showError("PIN expired. Please generate a new one.");
                    return;
                }
                
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                countdownTimer.setText(String.format("Expires in: %d:%02d", minutes, seconds));
                
                countdownHandler.postDelayed(this, 1000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }
    
    /**
     * Parse ISO 8601 date string to milliseconds
     */
    private long parseISO8601(String dateStr) {
        try {
            // Simple ISO 8601 parsing (format: 2024-11-26T10:30:00.000Z)
            dateStr = dateStr.replace("Z", "+00:00");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            return sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date", e);
            // Default to 10 minutes from now
            return System.currentTimeMillis() + (10 * 60 * 1000);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPairing = false;
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
}
