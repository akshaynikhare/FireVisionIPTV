package com.cadnative.firevisioniptv.domain.service

import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.protocol.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * These four methods are the app's only route for a *handled* exception to reach
 * Sentry — nothing else in the codebase calls captureException. Before the
 * Crashlytics removal they pointed at a SDK that was no longer initialized, so
 * they compiled, ran, and reported nowhere. A test that asserts the destination
 * is the only thing that makes that failure mode loud.
 */
class AnalyticsHelperTest {

    private val analytics: FirebaseAnalytics = mockk(relaxed = true)
    private lateinit var helper: AnalyticsHelper

    @Before
    fun setUp() {
        mockkStatic(Sentry::class)
        every { Sentry.captureException(any()) } returns io.sentry.protocol.SentryId.EMPTY_ID
        every { Sentry.addBreadcrumb(any<Breadcrumb>()) } returns Unit
        every { Sentry.setTag(any(), any()) } returns Unit
        every { Sentry.setUser(any()) } returns Unit
        helper = AnalyticsHelper(analytics)
    }

    @After
    fun tearDown() {
        unmockkStatic(Sentry::class)
    }

    @Test
    fun `logError reports the throwable to Sentry`() {
        val boom = IllegalStateException("boom")

        helper.logError(boom)

        verify(exactly = 1) { Sentry.captureException(boom) }
    }

    @Test
    fun `logError records the message as a breadcrumb before capturing`() {
        val crumb = slot<Breadcrumb>()
        every { Sentry.addBreadcrumb(capture(crumb)) } returns Unit
        val boom = IllegalStateException("boom")

        helper.logError(boom, "loading home data")

        assertEquals("loading home data", crumb.captured.message)
        verify(exactly = 1) { Sentry.captureException(boom) }
    }

    @Test
    fun `logError without a message adds no breadcrumb`() {
        helper.logError(IllegalStateException("boom"))

        verify(exactly = 0) { Sentry.addBreadcrumb(any<Breadcrumb>()) }
    }

    @Test
    fun `log adds a breadcrumb`() {
        val crumb = slot<Breadcrumb>()
        every { Sentry.addBreadcrumb(capture(crumb)) } returns Unit

        helper.log("scanning channels")

        assertEquals("scanning channels", crumb.captured.message)
    }

    @Test
    fun `setCustomKey becomes a Sentry tag`() {
        helper.setCustomKey("source", "m3u")

        verify(exactly = 1) { Sentry.setTag("source", "m3u") }
    }

    @Test
    fun `setUserId reaches both Analytics and Sentry`() {
        val user = slot<User>()
        every { Sentry.setUser(capture(user)) } returns Unit

        helper.setUserId("device-42")

        verify(exactly = 1) { analytics.setUserId("device-42") }
        assertEquals("device-42", user.captured.id)
    }
}
