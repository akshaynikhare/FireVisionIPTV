package com.cadnative.firevisioniptv.domain.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.protocol.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product analytics go to Firebase; diagnostics go to Sentry.
 *
 * Sentry reports uncaught crashes on its own, but until something calls
 * [logError] nothing reports a *handled* exception — the places that catch and
 * continue are exactly the ones worth seeing.
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    private val analytics: FirebaseAnalytics
) {

    // --- Analytics ---

    fun logEvent(name: String, vararg params: Pair<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        analytics.logEvent(name, bundle)
    }

    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, "screen_name" to screenName)
    }

    fun setUserProperty(key: String, value: String) {
        analytics.setUserProperty(key, value)
    }

    // --- Diagnostics ---

    /** Report a handled exception, with [message] recorded as leading context. */
    fun logError(throwable: Throwable, message: String? = null) {
        message?.let { log(it) }
        Sentry.captureException(throwable)
    }

    fun log(message: String) {
        Sentry.addBreadcrumb(Breadcrumb.info(message))
    }

    fun setUserId(userId: String) {
        analytics.setUserId(userId)
        Sentry.setUser(User().apply { id = userId })
    }

    fun setCustomKey(key: String, value: String) {
        Sentry.setTag(key, value)
    }
}
