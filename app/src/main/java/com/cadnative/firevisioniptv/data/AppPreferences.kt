package com.cadnative.firevisioniptv.data

import android.content.Context

/**
 * Centralized access to the app's SharedPreferences (FireVisionSettings).
 */
object AppPreferences {

    const val PREFS_NAME = "FireVisionSettings"
    private const val SERVER_URL_KEY = "server_url"
    private const val TV_CODE_KEY = "tv_code"
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

    fun setServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SERVER_URL_KEY, url).apply()
    }

    fun setTvCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TV_CODE_KEY, code).apply()
    }

    fun clearPairing(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(TV_CODE_KEY).apply()
    }
}
