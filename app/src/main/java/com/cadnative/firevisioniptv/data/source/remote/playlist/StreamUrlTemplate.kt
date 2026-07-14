package com.cadnative.firevisioniptv.data.source.remote.playlist

import android.content.Context
import com.cadnative.firevisioniptv.data.AppPreferences

/**
 * Keeps Xtream credentials out of URLs at rest. Imported stream URLs embed
 * [USER_TOKEN]/[PASS_TOKEN] placeholders instead of real credentials, so the
 * Room DB and the TIF provider store credential-free templates. [resolve]
 * substitutes the tokens from EncryptedSharedPreferences at use time
 * (playback, probing); non-templated URLs pass through unchanged.
 */
object StreamUrlTemplate {
    const val USER_TOKEN = "{username}"
    const val PASS_TOKEN = "{password}"

    fun resolve(context: Context, url: String): String {
        if (!url.contains(USER_TOKEN) && !url.contains(PASS_TOKEN)) return url
        return url
            .replace(USER_TOKEN, AppPreferences.getXtreamUser(context))
            .replace(PASS_TOKEN, AppPreferences.getXtreamPass(context))
    }
}
