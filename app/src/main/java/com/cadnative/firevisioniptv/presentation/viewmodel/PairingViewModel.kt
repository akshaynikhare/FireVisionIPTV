package com.cadnative.firevisioniptv.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.AppPreferences
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import com.cadnative.firevisioniptv.presentation.ui.player.isTvDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.cadnative.firevisioniptv.data.PinnedHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PairingUiState(
    val pin: String = "------",
    val statusMessage: String = "Generating PIN...",
    val statusColor: Color = Color.White,
    val countdownText: String = "",
    val isLoading: Boolean = true,
    val showRetryButton: Boolean = false,
    val showCountdown: Boolean = false,
    val qrCodeBitmap: Bitmap? = null,
    val serverUrl: String = "",
    val pairingUrl: String = "",
    val isTvDevice: Boolean = true,
    val isPaired: Boolean = false
)

/**
 * ViewModel for PIN-based TV pairing.
 *
 * Encapsulates the pairing flow: PIN generation, server polling,
 * countdown timer, and QR code generation — all using coroutines.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null
    private var pollingJob: Job? = null
    private var countdownJob: Job? = null
    @Volatile
    private var expiresAt: Long = 0

    private val isTv = isTvDevice(context)

    init {
        val serverUrl = AppPreferences.getServerUrl(context)
        _uiState.update { it.copy(serverUrl = serverUrl, isTvDevice = isTv) }
        requestNewPairing()
    }

    fun requestNewPairing() {
        requestJob?.cancel()
        pollingJob?.cancel()
        countdownJob?.cancel()

        _uiState.update {
            it.copy(
                pin = "------",
                statusMessage = "Connecting to server...",
                statusColor = Color.White,
                isLoading = true,
                showRetryButton = false,
                showCountdown = false,
                isPaired = false
            )
        }

        requestJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = AppPreferences.getServerUrl(context)
                val requestData = JSONObject().apply {
                    put("deviceName", Build.MODEL)
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                }

                val response = PinnedHttpClient.post(
                    "$baseUrl/api/v1/tv/pairing/request",
                    requestData.toString()
                )

                response.use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string() ?: "{}")

                        if (json.optBoolean("success", false)) {
                            val pin = json.getString("pin")
                            val expiresAtStr = json.getString("expiresAt")
                            val expiry = parseISO8601(expiresAtStr)
                            expiresAt = expiry

                            _uiState.update {
                                it.copy(
                                    pin = pin,
                                    statusMessage = "Waiting for confirmation...",
                                    statusColor = Color.White,
                                    isLoading = false,
                                    showCountdown = true,
                                    pairingUrl = "$baseUrl/pair?pin=$pin"
                                )
                            }

                            if (isTv) {
                                generateQrCode(baseUrl, pin)
                            }
                            startPolling(pin)
                            startCountdown(expiry)
                        } else {
                            showError(
                                "Failed to generate PIN: ${json.optString("error", "Unknown error")}"
                            )
                        }
                    } else {
                        showError("Server error: ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                showError("Connection error: ${e.message}")
            }
        }
    }

    fun useDefaultChannelList() {
        pollingJob?.cancel()
        countdownJob?.cancel()

        _uiState.update { it.copy(isLoading = true, statusMessage = "Fetching demo channels...") }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = AppPreferences.getServerUrl(context)
                val response = PinnedHttpClient.get(
                    "$baseUrl/api/v1/app/demo-code",
                    mapOf("Accept" to "application/json")
                )
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string() ?: "{}")
                        val demoCode = json.optString("code", "")
                        if (demoCode.isNotEmpty()) {
                            AppPreferences.setDemoMode(context, demoCode)
                            _uiState.update {
                                it.copy(isPaired = true, isLoading = false, statusMessage = "Using demo channel list")
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, statusMessage = "Demo channels unavailable", showRetryButton = true)
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, statusMessage = "Demo channels unavailable", showRetryButton = true)
                        }
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, statusMessage = "Network error — try again", showRetryButton = true)
                }
            }
        }
    }

    private fun startPolling(pin: String) {
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            // Use time-based termination instead of fixed attempt count
            while (isActive && System.currentTimeMillis() < expiresAt) {
                delay(POLL_INTERVAL_MS)

                try {
                    val baseUrl = AppPreferences.getServerUrl(context)
                    val response = PinnedHttpClient.get(
                        "$baseUrl/api/v1/tv/pairing/status/$pin",
                        mapOf("Accept" to "application/json")
                    )
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val paired = json.optBoolean("paired", false)
                            val status = json.optString("status", "unknown")

                            if (paired && status == "completed") {
                                val channelListCode = json.getString("channelListCode")
                                val username = json.optString("username", "User")
                                onPairingSuccess(channelListCode, username)
                                return@launch
                            } else if (status == "expired") {
                                showError("PIN expired. Please generate a new one.")
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PairingViewModel", "Poll attempt failed: ${e.message}")
                }
            }

            if (System.currentTimeMillis() >= expiresAt) {
                showError("Pairing timeout. Please try again.")
            }
        }
    }

    private fun startCountdown(expiryTimeMs: Long) {
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = expiryTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.update { it.copy(countdownText = "PIN Expired") }
                    showError("PIN expired. Please generate a new one.")
                    break
                }

                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                _uiState.update {
                    it.copy(countdownText = String.format("Expires in: %d:%02d", minutes, seconds))
                }

                delay(1000)
            }
        }
    }

    private fun onPairingSuccess(channelListCode: String, username: String) {
        pollingJob?.cancel()
        countdownJob?.cancel()

        AppPreferences.setTvCode(context, channelListCode)

        _uiState.update {
            it.copy(
                statusMessage = "Paired successfully! Welcome, $username!",
                statusColor = Color(0xFF4CAF50),
                showCountdown = false,
                isPaired = true
            )
        }
    }

    private fun showError(message: String) {
        pollingJob?.cancel()
        countdownJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = false,
                statusMessage = message,
                statusColor = Color(0xFFF44336),
                showRetryButton = true,
                showCountdown = false
            )
        }
    }

    private fun generateQrCode(serverUrl: String, pin: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pairingUrl = "$serverUrl/pair?pin=$pin"
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(
                    pairingUrl, BarcodeFormat.QR_CODE, 1024, 1024
                )
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(
                            x, y,
                            if (bitMatrix[x, y]) android.graphics.Color.BLACK
                            else android.graphics.Color.WHITE
                        )
                    }
                }

                _uiState.update { it.copy(qrCodeBitmap = bmp) }
            } catch (_: WriterException) {
                // QR generation failed silently
            }
        }
    }

    @Suppress("SimpleDateFormat")
    private fun parseISO8601(dateStr: String): Long {
        return try {
            val cleaned = dateStr.replace("Z", "+00:00")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            sdf.parse(cleaned)?.time ?: fallbackExpiry()
        } catch (_: Exception) {
            fallbackExpiry()
        }
    }

    private fun fallbackExpiry(): Long = System.currentTimeMillis() + 10 * 60 * 1000

    override fun onCleared() {
        super.onCleared()
        requestJob?.cancel()
        pollingJob?.cancel()
        countdownJob?.cancel()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3000L
    }
}
