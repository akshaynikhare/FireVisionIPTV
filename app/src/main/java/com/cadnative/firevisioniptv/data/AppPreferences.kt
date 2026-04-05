package com.cadnative.firevisioniptv.data

import android.content.Context

/**
 * Centralized access to the app's SharedPreferences (FireVisionSettings).
 */
object AppPreferences {

    const val PREFS_NAME = "FireVisionSettings"
    private const val SERVER_URL_KEY = "server_url"
    private const val TV_CODE_KEY = "tv_code"
    private const val DEMO_MODE_KEY = "is_demo_mode"
    const val DEFAULT_SERVER_URL = "https://tv.cadnative.com"

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun getTvCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TV_CODE_KEY, "") ?: ""
    }

    fun hasChannelSelection(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(TV_CODE_KEY)
    }

    fun isDemoMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(DEMO_MODE_KEY, false)
    }

    fun setServerUrl(context: Context, url: String) {
        val sanitized = url.trim()
        require(sanitized.startsWith("https://")) { "Server URL must use HTTPS" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SERVER_URL_KEY, sanitized).apply()
    }

    fun setTvCode(context: Context, code: String) {
        val sanitized = code.trim().replace(Regex("[^A-Za-z0-9]"), "")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TV_CODE_KEY, sanitized).apply()
    }

    fun setDemoMode(context: Context, code: String) {
        val sanitized = code.trim().replace(Regex("[^A-Za-z0-9]"), "")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TV_CODE_KEY, sanitized)
            .putBoolean(DEMO_MODE_KEY, true)
            .apply()
    }

    fun clearPairing(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(TV_CODE_KEY)
            .remove(DEMO_MODE_KEY)
            .apply()
    }
}
