package com.cadnative.firevisioniptv.update

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * The SDK branch here is load-bearing and silent when wrong: `signingInfo` is
 * API 28, and below that the field access throws NoSuchFieldError — an Error,
 * which the caller's `catch (Exception)` does not catch. Getting it wrong
 * disables self-update for every 23–27 user without a single log line, so both
 * sides of the branch need a test that runs at the relevant SDK.
 *
 * Robolectric is here only to make Build.VERSION.SDK_INT real — that is the
 * input under test. The PackageManager is a mock rather than a shadow because
 * ShadowPackageManager does not populate signingInfo for
 * GET_SIGNING_CERTIFICATES, which would test the shadow instead of the branch.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, manifest = Config.NONE)
class ApkSignaturesTest {

    private val pm: PackageManager = mockk()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Suppress("DEPRECATION")
    private fun legacyInfo(vararg signatures: Signature) = PackageInfo().apply {
        this.signatures = arrayOf(*signatures)
    }

    private fun signingInfoOf(multipleSigners: Boolean, vararg signatures: Signature) =
        mockk<SigningInfo>().also {
            every { it.hasMultipleSigners() } returns multipleSigners
            if (multipleSigners) {
                every { it.apkContentsSigners } returns arrayOf(*signatures)
            } else {
                every { it.signingCertificateHistory } returns arrayOf(*signatures)
            }
        }

    // ── Below API 28: PackageInfo.signatures ─────────────────────

    @Test
    @Config(sdk = [23])
    fun `on API 23 the legacy signatures field is read`() {
        val signature = Signature("aabbcc")
        every { pm.getPackageInfo("com.app", any<Int>()) } returns legacyInfo(signature)

        val signers = ApkSignatures.installed(pm, "com.app")!!

        assertEquals(listOf(sha256(signature.toByteArray())), signers.digests)
        assertFalse(signers.multipleSigners)
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 the legacy GET_SIGNATURES flag is requested`() {
        every { pm.getPackageInfo("com.app", any<Int>()) } returns legacyInfo(Signature("aabbcc"))

        ApkSignatures.installed(pm, "com.app")

        // GET_SIGNING_CERTIFICATES does not exist below 28; asking for it there
        // returns a PackageInfo with nothing populated and self-update dies quietly.
        @Suppress("DEPRECATION")
        verify { pm.getPackageInfo("com.app", PackageManager.GET_SIGNATURES) }
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 multiple signers come back sorted so callers compare sets`() {
        val a = Signature("aabbcc")
        val b = Signature("ddeeff")
        every { pm.getPackageInfo("com.app", any<Int>()) } returns legacyInfo(a, b)

        val signers = ApkSignatures.installed(pm, "com.app")!!

        // Below 28 `signatures` is the current signer set, never a lineage,
        // so two entries mean two signers and rotation rules must not apply.
        assertTrue(signers.multipleSigners)
        val digests = signers.digests
        assertEquals(2, digests.size)
        // Sorted, so the comparison never depends on the order the platform
        // happened to report the signers in.
        assertEquals(digests.sorted(), digests)
        assertTrue(digests.contains(sha256(a.toByteArray())))
        assertTrue(digests.contains(sha256(b.toByteArray())))
    }

    @Test
    @Config(sdk = [23])
    fun `on API 23 an unsigned package yields null rather than an empty match`() {
        every { pm.getPackageInfo("com.app", any<Int>()) } returns legacyInfo()

        assertNull(ApkSignatures.installed(pm, "com.app"))
    }

    // ── API 28+: SigningInfo ─────────────────────────────────────

    @Test
    @Config(sdk = [28])
    fun `on API 28 a single signer reports its whole certificate history`() {
        val current = Signature("aabbcc")
        val rotatedFrom = Signature("112233")
        every { pm.getPackageInfo("com.app", any<Int>()) } returns PackageInfo().apply {
            signingInfo = signingInfoOf(multipleSigners = false, current, rotatedFrom)
        }

        val signers = ApkSignatures.installed(pm, "com.app")!!

        // History, not just the current certificate: an update signed with the
        // previous key of a rotated pair is still legitimate.
        assertEquals(
            listOf(sha256(current.toByteArray()), sha256(rotatedFrom.toByteArray())).sorted(),
            signers.digests
        )
        // The flag is what lets accepts() read this as a lineage rather than a
        // two-signer set, which carries the opposite rule.
        assertFalse(signers.multipleSigners)
    }

    @Test
    @Config(sdk = [28])
    fun `on API 28 multiple signers report every current signer`() {
        val a = Signature("aabbcc")
        val b = Signature("ddeeff")
        every { pm.getPackageInfo("com.app", any<Int>()) } returns PackageInfo().apply {
            signingInfo = signingInfoOf(multipleSigners = true, a, b)
        }

        val signers = ApkSignatures.installed(pm, "com.app")!!

        assertEquals(
            listOf(sha256(a.toByteArray()), sha256(b.toByteArray())).sorted(),
            signers.digests
        )
        assertTrue(signers.multipleSigners)
    }

    @Test
    @Config(sdk = [28])
    fun `on API 28 the signing-certificates flag is requested`() {
        every { pm.getPackageInfo("com.app", any<Int>()) } returns PackageInfo().apply {
            signingInfo = signingInfoOf(multipleSigners = false, Signature("aabbcc"))
        }

        ApkSignatures.installed(pm, "com.app")

        verify { pm.getPackageInfo("com.app", PackageManager.GET_SIGNING_CERTIFICATES) }
    }

    @Test
    @Config(sdk = [28])
    fun `an absent signingInfo yields null rather than an empty match`() {
        every { pm.getPackageInfo("com.app", any<Int>()) } returns PackageInfo()

        assertNull(ApkSignatures.installed(pm, "com.app"))
    }

    // ── Failure paths ────────────────────────────────────────────

    @Test
    @Config(sdk = [28])
    fun `a package that cannot be read yields null instead of throwing`() {
        every {
            pm.getPackageInfo("com.absent", any<Int>())
        } throws PackageManager.NameNotFoundException()

        assertNull(ApkSignatures.installed(pm, "com.absent"))
    }

    @Test
    @Config(sdk = [23])
    fun `an archive that cannot be parsed yields null`() {
        every { pm.getPackageArchiveInfo(any(), any<Int>()) } returns null

        assertNull(ApkSignatures.archive(pm, "/nope/missing.apk"))
    }

    @Test
    @Config(sdk = [23])
    fun `an archive on API 23 is read through the legacy field`() {
        val signature = Signature("aabbcc")
        every { pm.getPackageArchiveInfo(any(), any<Int>()) } returns legacyInfo(signature)

        assertEquals(
            listOf(sha256(signature.toByteArray())),
            ApkSignatures.archive(pm, "/tmp/update.apk")!!.digests
        )
    }
}
