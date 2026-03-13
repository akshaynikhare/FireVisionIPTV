package com.cadnative.firevisioniptv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cadnative.firevisioniptv.presentation.ui.screens.LegacySettingsScreen
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.cadnative.firevisioniptv.update.UpdateManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Settings Activity for server configuration.
 * Now uses Jetpack Compose for the UI.
 */
class SettingsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
        private const val PREFS_NAME = "FireVisionSettings"
        private const val SERVER_URL_KEY = "server_url"
        private const val TV_CODE_KEY = "tv_code"
        private const val AUTOLOAD_CHANNEL_ID_KEY = "autoload_channel_id"
        private const val AUTOLOAD_CHANNEL_NAME_KEY = "autoload_channel_name"
        private const val DEFAULT_SERVER_URL = "https://tv.cadnative.com"
        private const val DEFAULT_TV_CODE = "5T6FEP"

        @JvmStatic
        fun getServerUrl(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        }

        @JvmStatic
        fun getTvCode(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(TV_CODE_KEY, DEFAULT_TV_CODE) ?: DEFAULT_TV_CODE
        }

        @JvmStatic
        fun setAutoloadChannel(context: Context, channelId: String, channelName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit()
                .putString(AUTOLOAD_CHANNEL_ID_KEY, channelId)
                .putString(AUTOLOAD_CHANNEL_NAME_KEY, channelName)
                .apply()
        }

        @JvmStatic
        fun getAutoloadChannelId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(AUTOLOAD_CHANNEL_ID_KEY, "") ?: ""
        }
    }

    // Compose state
    private var serverUrl by mutableStateOf("")
    private var tvCode by mutableStateOf("")
    private var currentServerInfo by mutableStateOf("Current server: Not configured")
    private var autoloadChannelInfo by mutableStateOf("No channel set")
    private var appVersionInfo by mutableStateOf("Version: Loading...")
    private var qrCodeBitmap by mutableStateOf<Bitmap?>(null)

    private var updateManager: UpdateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = UpdateManager(this)
        initializeDefaults()
        loadState()
        generateQRCode()

        setContent {
            FireVisionTheme {
                LegacySettingsScreen(
                    serverUrl = serverUrl,
                    tvCode = tvCode,
                    currentServerInfo = currentServerInfo,
                    autoloadChannelInfo = autoloadChannelInfo,
                    appVersionInfo = appVersionInfo,
                    qrCodeBitmap = qrCodeBitmap,
                    onServerUrlChange = { serverUrl = it },
                    onTvCodeChange = { tvCode = it },
                    onSaveSettings = { saveSettings() },
                    onPairDevice = {
                        startActivity(Intent(this, PairingActivity::class.java))
                    },
                    onClearAutoload = { clearAutoloadChannel() },
                    onCheckUpdates = { checkForUpdates() },
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    private fun loadState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serverUrl = prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        tvCode = prefs.getString(TV_CODE_KEY, DEFAULT_TV_CODE) ?: DEFAULT_TV_CODE
        currentServerInfo = "Current server: $serverUrl"

        val channelName = prefs.getString(AUTOLOAD_CHANNEL_NAME_KEY, "") ?: ""
        autoloadChannelInfo = if (channelName.isNotEmpty()) "Auto-load: $channelName" else "No channel set"

        loadAppVersionInfo()
    }

    private fun initializeDefaults() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false

        if (prefs.getString(SERVER_URL_KEY, "").isNullOrEmpty()) {
            editor.putString(SERVER_URL_KEY, DEFAULT_SERVER_URL)
            changed = true
        }
        if (prefs.getString(TV_CODE_KEY, "").isNullOrEmpty()) {
            editor.putString(TV_CODE_KEY, DEFAULT_TV_CODE)
            changed = true
        }
        if (changed) editor.apply()
    }

    private fun saveSettings() {
        val url = serverUrl.trim()
        val code = tvCode.trim()

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL", Toast.LENGTH_SHORT).show()
            return
        }
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter a TV code", Toast.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(SERVER_URL_KEY, url)
            .putString(TV_CODE_KEY, code)
            .apply()

        currentServerInfo = "Current server: $url"
        generateQRCode()
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun clearAutoloadChannel() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .remove(AUTOLOAD_CHANNEL_ID_KEY)
            .remove(AUTOLOAD_CHANNEL_NAME_KEY)
            .apply()

        autoloadChannelInfo = "No channel set"
        Toast.makeText(this, "Auto-load channel cleared", Toast.LENGTH_SHORT).show()
    }

    private fun loadAppVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            appVersionInfo = "Version: $versionName (Build $versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            appVersionInfo = "Version: Unknown"
        }
    }

    private fun checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
        updateManager?.checkForUpdates(true)
    }

    private fun generateQRCode() {
        Thread {
            try {
                val registrationUrl = "$serverUrl/user/register.html"
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
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateManager?.cleanup()
    }
}
