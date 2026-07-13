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
    private const val PLAYER_HINT_COUNT_KEY = "player_hint_count"
    private const val EPG_XMLTV_URL_KEY = "epg_xmltv_url"
    private const val PLAYLIST_SOURCE_TYPE_KEY = "playlist_source_type"
    private const val M3U_URL_KEY = "m3u_url"
    private const val XTREAM_HOST_KEY = "xtream_host"
    private const val XTREAM_USER_KEY = "xtream_user"
    private const val XTREAM_PASS_KEY = "xtream_pass"
    const val DEFAULT_SERVER_URL = "https://tv.cadnative.com"

    /** Playlist source types. PAIRED = managed server (default); M3U/XTREAM = bring-your-own. */
    const val SOURCE_PAIRED = "paired"
    const val SOURCE_M3U = "m3u"
    const val SOURCE_XTREAM = "xtream"
    const val PLAYER_HINT_MAX_SHOWS = 3

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
        // Pairing is a server-backed source — clear any prior BYO playlist selection
        // so refreshChannels() hits the server instead of a stale M3U/Xtream source.
        prefs.edit()
            .putString(TV_CODE_KEY, sanitized)
            .putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_PAIRED)
            .apply()
    }

    fun setDemoMode(context: Context, code: String) {
        val sanitized = code.trim().replace(Regex("[^A-Za-z0-9]"), "")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TV_CODE_KEY, sanitized)
            .putBoolean(DEMO_MODE_KEY, true)
            .putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_PAIRED)
            .apply()
    }

    fun getPlayerHintCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PLAYER_HINT_COUNT_KEY, 0)
    }

    fun incrementPlayerHintCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(PLAYER_HINT_COUNT_KEY, getPlayerHintCount(context) + 1).apply()
    }

    /**
     * Optional user-supplied XMLTV EPG URL. When set, the app fetches and parses this
     * guide directly (TiviMate/Kodi style) as an additional source, so EPG no longer
     * depends solely on the server. Empty string means "not configured".
     */
    fun getEpgXmltvUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(EPG_XMLTV_URL_KEY, "") ?: ""
    }

    fun setEpgXmltvUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(EPG_XMLTV_URL_KEY, url.trim()).apply()
    }

    // ── Bring-your-own playlist source ──────────────────────────────────────

    /** Current playlist source type: [SOURCE_PAIRED] (default), [SOURCE_M3U], or [SOURCE_XTREAM]. */
    fun getPlaylistSourceType(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_PAIRED) ?: SOURCE_PAIRED
    }

    fun setM3uSource(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_M3U)
            .putString(M3U_URL_KEY, url.trim())
            .apply()
    }

    fun getM3uUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(M3U_URL_KEY, "") ?: ""
    }

    fun setXtreamSource(context: Context, host: String, username: String, password: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_XTREAM)
            .putString(XTREAM_HOST_KEY, host.trim().trimEnd('/'))
            .putString(XTREAM_USER_KEY, username.trim())
            .putString(XTREAM_PASS_KEY, password.trim())
            .apply()
    }

    fun getXtreamHost(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(XTREAM_HOST_KEY, "") ?: ""
    }

    fun getXtreamUser(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(XTREAM_USER_KEY, "") ?: ""
    }

    fun getXtreamPass(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(XTREAM_PASS_KEY, "") ?: ""
    }

    /** Switch back to the managed (paired) source. */
    fun useManagedSource(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_PAIRED).apply()
    }

    /** True when a usable channel source exists (paired code OR a BYO playlist). */
    fun hasAnySource(context: Context): Boolean {
        return when (getPlaylistSourceType(context)) {
            SOURCE_M3U -> getM3uUrl(context).isNotBlank()
            SOURCE_XTREAM -> getXtreamHost(context).isNotBlank()
            else -> hasChannelSelection(context)
        }
    }

    fun clearPairing(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(TV_CODE_KEY)
            .remove(DEMO_MODE_KEY)
            .apply()
    }
}
