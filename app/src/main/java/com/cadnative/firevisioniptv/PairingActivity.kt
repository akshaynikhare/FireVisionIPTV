package com.cadnative.firevisioniptv

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cadnative.firevisioniptv.presentation.ui.screens.PairingScreen
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.cadnative.firevisioniptv.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Activity for PIN-based TV pairing.
 * Displays a 6-digit PIN and polls server until user confirms pairing on web dashboard.
 */
class PairingActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PairingActivity"
        private const val POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 200
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val POLL_READ_TIMEOUT_MS = 10_000
    }

    // Compose state
    private var pin by mutableStateOf("------")
    private var statusMessage by mutableStateOf("Generating PIN...")
    private var statusColor by mutableStateOf(androidx.compose.ui.graphics.Color.White)
    private var countdownText by mutableStateOf("")
    private var isLoading by mutableStateOf(true)
    private var showRetryButton by mutableStateOf(false)
    private var showCountdown by mutableStateOf(false)
    private var qrCodeBitmap by mutableStateOf<Bitmap?>(null)
    private var serverUrl by mutableStateOf("")

    @Volatile private var currentPin: String? = null
    @Volatile private var expiresAt: Long = 0
    private var pollHandler: Handler? = null
    private var pollRunnable: Runnable? = null
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var pollAttempts = 0
    @Volatile private var isPairing = false
    @Volatile private var isRequestingPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverUrl = AppPreferences.getServerUrl(this)

        setContent {
            FireVisionTheme {
                PairingScreen(
                    pin = pin,
                    statusMessage = statusMessage,
                    statusColor = statusColor,
                    countdownText = countdownText,
                    isLoading = isLoading,
                    showRetryButton = showRetryButton,
                    showCountdown = showCountdown,
                    qrCodeBitmap = qrCodeBitmap,
                    serverUrl = serverUrl,
                    onRetryClick = { requestNewPairing() },
                    onUseDefaultClick = { useDefaultChannelList() }
                )
            }
        }

        requestNewPairing()
    }

    private fun generateSignupQRCode(serverUrl: String, pin: String) {
        Thread {
            try {
                val registrationUrl = "$serverUrl/pair?pin=$pin"
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(registrationUrl, BarcodeFormat.QR_CODE, 512, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }

                runOnUiThread { qrCodeBitmap = bmp }
            } catch (e: WriterException) {
                Log.e(TAG, "Error generating signup QR code", e)
            }
        }.start()
    }

    private fun requestNewPairing() {
        if (isRequestingPin) return
        isRequestingPin = true
        isPairing = true
        pollAttempts = 0

        // Stop any existing polling/countdown
        pollHandler?.let { handler -> pollRunnable?.let { handler.removeCallbacks(it) } }
        countdownHandler?.let { handler -> countdownRunnable?.let { handler.removeCallbacks(it) } }

        isLoading = true
        pin = "------"
        statusMessage = "Connecting to server..."
        statusColor = androidx.compose.ui.graphics.Color.White
        showCountdown = false
        showRetryButton = false

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val baseUrl = AppPreferences.getServerUrl(this@PairingActivity)
                val url = URL("$baseUrl/api/v1/tv/pairing/request")
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestData = JSONObject().apply {
                    put("deviceName", Build.MODEL)
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                }

                connection.outputStream.use { os ->
                    os.write(requestData.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.optBoolean("success", false)) {
                        currentPin = jsonResponse.getString("pin")
                        val expiresAtStr = jsonResponse.getString("expiresAt")
                        expiresAt = parseISO8601(expiresAtStr)

                        runOnUiThread {
                            isLoading = false
                            pin = currentPin ?: "------"
                            statusMessage = "Waiting for confirmation..."
                            statusColor = androidx.compose.ui.graphics.Color.White
                            showCountdown = true
                            generateSignupQRCode(baseUrl, currentPin ?: "")
                            startPolling()
                            startCountdown()
                        }
                    } else {
                        showError("Failed to generate PIN: ${jsonResponse.optString("error", "Unknown error")}")
                    }
                } else {
                    showError("Server error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting pairing", e)
                showError("Connection error: ${e.message}")
            } finally {
                connection?.disconnect()
                isRequestingPin = false
            }
        }.start()
    }

    private fun startPolling() {
        pollHandler = Handler(Looper.getMainLooper())
        pollRunnable = object : Runnable {
            override fun run() {
                if (!isPairing || pollAttempts >= MAX_POLL_ATTEMPTS) {
                    if (pollAttempts >= MAX_POLL_ATTEMPTS) {
                        showError("Pairing timeout. Please try again.")
                    }
                    return
                }
                pollAttempts++
                checkPairingStatus()
                pollHandler?.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        pollHandler?.postDelayed(pollRunnable!!, POLL_INTERVAL_MS)
    }

    private fun checkPairingStatus() {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val baseUrl = AppPreferences.getServerUrl(this@PairingActivity)
                val url = URL("$baseUrl/api/v1/tv/pairing/status/$currentPin")
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = POLL_READ_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val paired = jsonResponse.optBoolean("paired", false)
                    val status = jsonResponse.optString("status", "unknown")

                    if (paired && status == "completed") {
                        val channelListCode = jsonResponse.getString("channelListCode")
                        val username = jsonResponse.optString("username", "User")
                        onPairingSuccess(channelListCode, username)
                    } else if (status == "expired") {
                        showError("PIN expired. Please generate a new one.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking pairing status", e)
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun onPairingSuccess(channelListCode: String, username: String) {
        isPairing = false
        pollRunnable?.let { pollHandler?.removeCallbacks(it) }

        runOnUiThread {
            val prefs: SharedPreferences = getSharedPreferences("FireVisionSettings", MODE_PRIVATE)
            prefs.edit().putString("tv_code", channelListCode).apply()

            statusMessage = "Paired successfully!"
            statusColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            showCountdown = false

            Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_LONG).show()

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@PairingActivity, ComposeMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }, 2000)
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            isLoading = false
            statusMessage = message
            statusColor = androidx.compose.ui.graphics.Color(0xFFF44336)
            showRetryButton = true
            showCountdown = false
            isPairing = false
            pollHandler?.let { handler ->
                pollRunnable?.let { handler.removeCallbacks(it) }
            }
        }
    }

    private fun startCountdown() {
        // Clean up any previous countdown
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (!isPairing) return

                val remaining = expiresAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    countdownText = "PIN Expired"
                    showError("PIN expired. Please generate a new one.")
                    return
                }

                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                countdownText = String.format("Expires in: %d:%02d", minutes, seconds)
                countdownHandler?.postDelayed(this, 1000)
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }

    private fun useDefaultChannelList() {
        isPairing = false
        pollHandler?.let { handler ->
            pollRunnable?.let { handler.removeCallbacks(it) }
        }

        val prefs: SharedPreferences = getSharedPreferences("FireVisionSettings", MODE_PRIVATE)
        prefs.edit().putString("tv_code", AppPreferences.DEFAULT_TV_CODE).apply()

        Toast.makeText(this, "Using default channel list", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ComposeMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun parseISO8601(dateStr: String): Long {
        return try {
            val cleaned = dateStr.replace("Z", "+00:00")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            sdf.parse(cleaned)?.time ?: (System.currentTimeMillis() + 10 * 60 * 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date", e)
            System.currentTimeMillis() + 10 * 60 * 1000
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPairing = false
        pollHandler?.let { handler ->
            pollRunnable?.let { handler.removeCallbacks(it) }
        }
        countdownHandler?.let { handler ->
            countdownRunnable?.let { handler.removeCallbacks(it) }
        }
    }
}
