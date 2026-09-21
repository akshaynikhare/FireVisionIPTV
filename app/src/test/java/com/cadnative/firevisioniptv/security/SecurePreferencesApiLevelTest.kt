package com.cadnative.firevisioniptv.security

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * `Context.deleteSharedPreferences` is API 24. Below that both halves of the
 * fallback matter and neither is observable from the happy path: the in-memory
 * map has to be cleared *synchronously* because the caller's retry is
 * synchronous, and the backing XML has to go, because a stale file still bound
 * to a dead master key fails EncryptedSharedPreferences.create a second time —
 * which throws and costs the user their stored credentials instead of
 * recovering them.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, manifest = Config.NONE)
class SecurePreferencesApiLevelTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val name = "secure_prefs_test"

    private fun prefsFile() = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$name.xml")

    private fun seed() {
        // commit() so the XML exists on disk before the assertions run.
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
            .edit().putString("tv_code", "ABC123").commit()
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 the stored values are gone`() {
        seed()

        clearPrefsFile(context, name)

        assertNull(
            context.getSharedPreferences(name, Context.MODE_PRIVATE).getString("tv_code", null)
        )
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 the backing XML is deleted, not just the in-memory map`() {
        seed()
        assertTrue("fixture did not write the prefs file", prefsFile().exists())

        clearPrefsFile(context, name)

        // A file left behind is still bound to the dead master key, so the
        // caller's retry would fail a second time and throw.
        assertFalse(prefsFile().exists())
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 clearing prefs that were never written does not throw`() {
        clearPrefsFile(context, name)

        assertNull(
            context.getSharedPreferences(name, Context.MODE_PRIVATE).getString("tv_code", null)
        )
    }

    @Test
    @Config(sdk = [24])
    fun `on API 24 the platform delete clears the values`() {
        seed()

        clearPrefsFile(context, name)

        assertNull(
            context.getSharedPreferences(name, Context.MODE_PRIVATE).getString("tv_code", null)
        )
    }

    @Test
    @Config(sdk = [24])
    fun `on API 24 the platform delete removes the file too`() {
        seed()
        assertTrue("fixture did not write the prefs file", prefsFile().exists())

        clearPrefsFile(context, name)

        assertFalse(prefsFile().exists())
    }

    @Test
    @Config(sdk = [33])
    fun `a modern SDK clears through the same entry point`() {
        seed()

        clearPrefsFile(context, name)

        assertNull(
            context.getSharedPreferences(name, Context.MODE_PRIVATE).getString("tv_code", null)
        )
    }
}
