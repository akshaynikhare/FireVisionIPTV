package com.cadnative.firevisioniptv.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.R
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.model.dto.PairingRequestBody
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import com.cadnative.firevisioniptv.presentation.ui.player.isTvDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PairingUiState(
    val pin: String = "",
    val statusMessage: String = "",
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
    @ApplicationContext private val context: Context,
    private val apiService: FireVisionApiService
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
        _uiState.update {
            it.copy(
                pin = context.getString(R.string.pairing_pin_placeholder),
                statusMessage = context.getString(R.string.pairing_status_generating),
                serverUrl = serverUrl,
                isTvDevice = isTv
            )
        }
        requestNewPairing()
    }

    fun requestNewPairing() {
        requestJob?.cancel()
        pollingJob?.cancel()
        countdownJob?.cancel()

        _uiState.update {
            it.copy(
                pin = context.getString(R.string.pairing_pin_placeholder),
                statusMessage = context.getString(R.string.pairing_status_connecting),
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
                val response = apiService.requestPairing(
                    PairingRequestBody(
                        deviceName = Build.MODEL,
                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    )
                )
                val body = response.body()

                when {
                    !response.isSuccessful ->
                        showError(context.getString(R.string.pairing_err_server, response.code()))
                    body?.success == true && !body.pin.isNullOrBlank() -> {
                        val pin = body.pin
                        val expiry = parseISO8601(body.expiresAt)
                        expiresAt = expiry

                        _uiState.update {
                            it.copy(
                                pin = pin,
                                statusMessage = context.getString(R.string.pairing_status_waiting),
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
                    }
                    else -> showError(
                        context.getString(
                            R.string.pairing_err_generate,
                            body?.error ?: context.getString(R.string.pairing_err_unknown)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(context.getString(R.string.pairing_err_connection, e.message.orEmpty()))
            }
        }
    }

    fun useDefaultChannelList() {
        pollingJob?.cancel()
        countdownJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = context.getString(R.string.pairing_status_demo_fetching)
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getDemoCode()
                val demoCode = response.body()?.get("code").orEmpty()
                if (response.isSuccessful && demoCode.isNotEmpty()) {
                    AppPreferences.setDemoMode(context, demoCode)
                    _uiState.update {
                        it.copy(
                            isPaired = true,
                            isLoading = false,
                            statusMessage = context.getString(R.string.pairing_status_demo_using)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = context.getString(R.string.pairing_status_demo_unavailable),
                            showRetryButton = true
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = context.getString(R.string.pairing_status_network_error),
                        showRetryButton = true
                    )
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
                    val response = apiService.getPairingStatus(pin)
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        val channelListCode = body.channelListCode
                        if (body.paired && body.status == "completed" && !channelListCode.isNullOrBlank()) {
                            onPairingSuccess(
                                channelListCode,
                                body.username?.takeIf { it.isNotBlank() }
                                    ?: context.getString(R.string.pairing_default_username)
                            )
                            return@launch
                        } else if (body.status == "expired") {
                            showError(context.getString(R.string.pairing_err_expired))
                            return@launch
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("PairingViewModel", "Poll attempt failed: ${e.message}")
                }
            }

            if (System.currentTimeMillis() >= expiresAt) {
                showError(context.getString(R.string.pairing_err_timeout))
            }
        }
    }

    private fun startCountdown(expiryTimeMs: Long) {
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = expiryTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.update {
                        it.copy(countdownText = context.getString(R.string.pairing_countdown_expired))
                    }
                    showError(context.getString(R.string.pairing_err_expired))
                    break
                }

                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                _uiState.update {
                    it.copy(countdownText = context.getString(R.string.pairing_countdown, minutes, seconds))
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
                statusMessage = context.getString(R.string.pairing_status_paired, username),
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
    private fun parseISO8601(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return fallbackExpiry()
        return try {
            // 'X' needs API 24; 'Z' works everywhere but wants +0000, not +00:00.
            val cleaned = dateStr
                .replace("Z", "+0000")
                .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
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
