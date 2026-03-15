package com.cadnative.firevisioniptv.data

import android.content.Context

/**
 * Centralized access to the app's SharedPreferences (FireVisionSettings).
 *
 * Replaces the scattered SettingsActivity.getServerUrl() / getTvCode() static
 * helpers that were called from 6+ files.
 */
object AppPreferences {

    const val PREFS_NAME = "FireVisionSettings"
    private const val SERVER_URL_KEY = "server_url"
    private const val TV_CODE_KEY = "tv_code"
    private const val AUTOLOAD_CHANNEL_ID_KEY = "autoload_channel_id"
    private const val AUTOLOAD_CHANNEL_NAME_KEY = "autoload_channel_name"

    const val DEFAULT_SERVER_URL = "https://tv.cadnative.com"
    const val DEFAULT_TV_CODE = "5T6FEP"

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun getTvCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TV_CODE_KEY, DEFAULT_TV_CODE) ?: DEFAULT_TV_CODE
    }

    fun setAutoloadChannel(context: Context, channelId: String, channelName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(AUTOLOAD_CHANNEL_ID_KEY, channelId)
            .putString(AUTOLOAD_CHANNEL_NAME_KEY, channelName)
            .apply()
    }

    fun getAutoloadChannelId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(AUTOLOAD_CHANNEL_ID_KEY, "") ?: ""
    }

    fun setServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SERVER_URL_KEY, url).apply()
    }

    fun setTvCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TV_CODE_KEY, code).apply()
    }
}
