package com.cadnative.firevisioniptv.api

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.cadnative.firevisioniptv.Channel
import com.cadnative.firevisioniptv.SettingsActivity

object ApiClient {
    private const val TAG = "ApiClient"
    private const val DEFAULT_BASE_URL = "https://tv.cadnative.com"
    private const val TIMEOUT = 30000 // 30 seconds

    /**
     * Get the base URL from settings, or use default
     */
    private fun getBaseUrl(context: Context): String {
        val savedUrl = SettingsActivity.getServerUrl(context)
        return if (!savedUrl.isNullOrEmpty()) savedUrl else DEFAULT_BASE_URL
    }

    interface ChannelListCallback {
        fun onSuccess(channels: List<Channel>)
        fun onError(error: String)
    }

    interface AppVersionCallback {
        fun onSuccess(versionInfo: AppVersionInfo)
        fun onError(error: String)
    }

    class AppVersionInfo(json: JSONObject) {
        var updateAvailable: Boolean = json.optBoolean("updateAvailable", false)
        var isMandatory: Boolean = json.optBoolean("isMandatory", false)
        var versionName: String = ""
        var versionCode: Int = 0
        var downloadUrl: String = ""
        var fileSize: Long = 0
        var releaseNotes: String = ""

        init {
            if (json.has("latestVersion")) {
                val latest = json.getJSONObject("latestVersion")
                versionName = latest.optString("versionName", "")
                versionCode = latest.optInt("versionCode", 0)
                downloadUrl = latest.optString("downloadUrl", "")
                fileSize = latest.optLong("apkFileSize", 0)
                releaseNotes = latest.optString("releaseNotes", "")
            }
        }
    }

    /**
     * Fetch channel list from server
     */
    @JvmStatic
    fun fetchChannelList(context: Context, callback: ChannelListCallback?) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val baseUrl = getBaseUrl(context)
                val tvCode = SettingsActivity.getTvCode(context)
                val url = URL("$baseUrl/api/v1/channels")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-TV-Code", tvCode)

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readStream(connection.inputStream)
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.optBoolean("success", false)) {
                        val channelsArray = jsonResponse.getJSONArray("data")
                        val channels = parseChannels(channelsArray)
                        callback?.onSuccess(channels)
                    } else {
                        callback?.onError("Server returned error")
                    }
                } else {
                    callback?.onError("Server responded with code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching channel list", e)
                callback?.onError(e.message ?: "Unknown error")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    /**
     * Check for app updates
     */
    @JvmStatic
    fun checkForUpdates(context: Context, currentVersionCode: Int, callback: AppVersionCallback?) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val baseUrl = getBaseUrl(context)
                val tvCode = SettingsActivity.getTvCode(context)
                val url = URL("$baseUrl/api/v1/app/version?currentVersion=$currentVersionCode")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-TV-Code", tvCode)

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readStream(connection.inputStream)
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.optBoolean("success", false)) {
                        val versionInfo = AppVersionInfo(jsonResponse)
                        callback?.onSuccess(versionInfo)
                    } else {
                        callback?.onError("Failed to check version")
                    }
                } else {
                    callback?.onError("Server responded with code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                callback?.onError(e.message ?: "Unknown error")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    /**
     * Parse JSON array to Channel list
     */
    private fun parseChannels(jsonArray: JSONArray): List<Channel> {
        val channels = mutableListOf<Channel>()

        for (i in 0 until jsonArray.length()) {
            val channelObj = jsonArray.getJSONObject(i)

            val language = if (channelObj.has("metadata")) {
                channelObj.getJSONObject("metadata").optString("language", "Unknown")
            } else {
                "Unknown"
            }

            // Prefer tvgLogo over channelImg for logo URL
            val tvgLogo = channelObj.optString("tvgLogo", "")
            val channelImg = channelObj.optString("channelImg", "")
            val resolvedLogo = tvgLogo.ifEmpty { channelImg }

            val channel = Channel(
                channelId = channelObj.optString("channelId", ""),
                channelName = channelObj.optString("channelName", "Unknown"),
                channelUrl = channelObj.optString("channelUrl", ""),
                channelImg = resolvedLogo,
                channelGroup = channelObj.optString("channelGroup", "Uncategorized"),
                channelDrmKey = channelObj.optString("channelDrmKey", ""),
                channelDrmType = channelObj.optString("channelDrmType", ""),
                channelLanguage = language
            )

            channels.add(channel)
        }

        return channels
    }

    /**
     * Read input stream to string
     */
    private fun readStream(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readText()
        }
    }
}
