package com.cadnative.firevisioniptv.data

import android.content.Context
import com.cadnative.firevisioniptv.security.SecurePreferences

/**
 * Centralized access to the app's SharedPreferences (FireVisionSettings).
 */
object AppPreferences {

    const val PREFS_NAME = "FireVisionSettings"
    private const val SERVER_URL_KEY = "server_url"
    private const val TV_CODE_KEY = "tv_code"
    private const val DEMO_MODE_KEY = "is_demo_mode"
    private const val EPG_XMLTV_URL_KEY = "epg_xmltv_url"
    private const val PLAYLIST_EPG_URL_KEY = "playlist_epg_url"
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
        // Also clear the demo flag: a manually-set code is a real pairing, not demo,
        // so isPaired resolves true instead of the source reading as "Demo".
        prefs.edit()
            .putString(TV_CODE_KEY, sanitized)
            .putString(PLAYLIST_SOURCE_TYPE_KEY, SOURCE_PAIRED)
            .remove(DEMO_MODE_KEY)
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

    /**
     * EPG URL derived from an imported playlist's `url-tvg`. Kept separate from the user's
     * manual [getEpgXmltvUrl] so a playlist refresh never clobbers a manual setting, and a
     * new playlist with no guide doesn't inherit the previous source's URL.
     */
    fun getPlaylistEpgUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PLAYLIST_EPG_URL_KEY, "") ?: ""
    }

    fun setPlaylistEpgUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PLAYLIST_EPG_URL_KEY, url.trim()).apply()
    }

    /** Effective guide URL: the user's manual override wins, else the playlist-derived one. */
    fun getEffectiveEpgUrl(context: Context): String {
        val manual = getEpgXmltvUrl(context)
        return if (manual.isNotBlank()) manual else getPlaylistEpgUrl(context)
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
            .remove(XTREAM_USER_KEY) // migrate any legacy plaintext creds out of plain prefs
            .remove(XTREAM_PASS_KEY)
            .apply()
        // Credentials are sensitive — store them in EncryptedSharedPreferences, never plain prefs.
        val secure = SecurePreferences(context)
        secure.putString(XTREAM_USER_KEY, username.trim())
        secure.putString(XTREAM_PASS_KEY, password.trim())
    }

    fun getXtreamHost(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(XTREAM_HOST_KEY, "") ?: ""
    }

    fun getXtreamUser(context: Context): String = readSecure(context, XTREAM_USER_KEY)

    fun getXtreamPass(context: Context): String = readSecure(context, XTREAM_PASS_KEY)

    private fun readSecure(context: Context, key: String): String =
        try {
            SecurePreferences(context).getString(key, "") ?: ""
        } catch (_: Exception) {
            ""
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
