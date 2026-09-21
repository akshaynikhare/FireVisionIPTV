package com.cadnative.firevisioniptv.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Deletes a SharedPreferences file outright.
 *
 * `Context.deleteSharedPreferences` is API 24, so below that the in-memory map is
 * cleared synchronously and the backing XML removed by hand. Both halves matter:
 * a stale file still bound to a dead master key fails `EncryptedSharedPreferences
 * .create` a second time, which would throw and cost the user their stored
 * credentials rather than recovering.
 */
internal fun clearPrefsFile(context: Context, name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.deleteSharedPreferences(name)
        return
    }
    // commit(), not apply() — the retry below is synchronous and must see the
    // cleared state, so deferring the write to a background thread would race it.
    @Suppress("ApplySharedPref")
    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    runCatching {
        File(File(context.applicationInfo.dataDir, "shared_prefs"), "$name.xml").delete()
    }
}

/**
 * Secure preferences using EncryptedSharedPreferences for sensitive data.
 */
class SecurePreferences(context: Context) {

    val isEncrypted: Boolean = true

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Keystore corrupted — clear and retry once
            clearPrefsFile(context, "secure_prefs")
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                android.util.Log.e("SecurePreferences", "CRITICAL: EncryptedSharedPreferences failed twice — refusing to store sensitive data unencrypted", e)
                throw SecurityException("Cannot create encrypted storage. Device keystore may be corrupted.", e)
            }
        }
    }

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
