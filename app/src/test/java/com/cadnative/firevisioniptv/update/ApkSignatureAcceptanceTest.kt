package com.cadnative.firevisioniptv.update

import com.cadnative.firevisioniptv.update.ApkSignatures.Signers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The predicate that decides whether a downloaded APK may replace the installed
 * app. Plain JVM: it is pure set logic, deliberately extracted out of AppUpdater
 * so it can be reasoned about without a PackageManager.
 *
 * Two opposite failure modes, both silent:
 *
 * - Too strict and a legitimate rotated update is refused, in the one component
 *   that cannot ship its own fix.
 * - Too loose and we launch an installer the platform then refuses, leaving the
 *   APK on disk having told the user the install started.
 *
 * The distinction that makes both work is [Signers.multipleSigners]: `[A, B]`
 * from one signer is a rotation lineage, `[A, B]` from two is a current signer
 * set, and they are indistinguishable as flat lists.
 */
class ApkSignatureAcceptanceTest {

    private val keyA = "aa11"
    private val keyB = "bb22"
    private val keyC = "cc33"

    private fun single(vararg digests: String) =
        Signers(digests.toList().sorted(), multipleSigners = false)

    private fun multi(vararg digests: String) =
        Signers(digests.toList().sorted(), multipleSigners = true)

    // ── Single signer: rotation lineage ──────────────────────────

    @Test
    fun `an identically signed update is accepted`() {
        assertTrue(ApkSignatures.accepts(single(keyA), single(keyA)))
    }

    @Test
    fun `a rotated update is accepted when its lineage includes the installed key`() {
        // Installed app still on the old key; update carries proof of rotation.
        assertTrue(ApkSignatures.accepts(single(keyA), single(keyA, keyB)))
    }

    @Test
    fun `an already-rotated install accepts an update carrying the same lineage`() {
        assertTrue(ApkSignatures.accepts(single(keyA, keyB), single(keyA, keyB)))
    }

    @Test
    fun `an unrelated signing key is refused`() {
        assertFalse(ApkSignatures.accepts(single(keyA), single(keyC)))
    }

    @Test
    fun `an update that drops a key the install already has is refused`() {
        // Signed only with the retired key: the installed lineage is not a
        // subset, so this is not a legitimate successor.
        assertFalse(ApkSignatures.accepts(single(keyA, keyB), single(keyA)))
    }

    // ── Multiple signers: exact current set ──────────────────────

    @Test
    fun `adding a co-signer is refused even though the known key is still present`() {
        // The case containment alone gets wrong. Android only honours a rotation
        // lineage for single-signer packages, so it rejects [A] -> [A, C];
        // accepting it here would launch an installer that then refuses the APK.
        assertFalse(ApkSignatures.accepts(single(keyA), multi(keyA, keyC)))
    }

    @Test
    fun `dropping a co-signer is refused`() {
        assertFalse(ApkSignatures.accepts(multi(keyA, keyC), single(keyA)))
    }

    @Test
    fun `a multi-signer package accepts an update with the identical signer set`() {
        assertTrue(ApkSignatures.accepts(multi(keyA, keyC), multi(keyA, keyC)))
    }

    @Test
    fun `a multi-signer package refuses a swapped co-signer`() {
        assertFalse(ApkSignatures.accepts(multi(keyA, keyB), multi(keyA, keyC)))
    }

    @Test
    fun `a multi-signer install refuses a rotation-style lineage`() {
        // Rotation is undefined for multi-signer packages, so the superset that
        // would be valid for a single signer is not valid here.
        assertFalse(ApkSignatures.accepts(multi(keyA, keyB), single(keyA, keyB, keyC)))
    }

    // ── Ordering and failure paths ───────────────────────────────

    @Test
    fun `order does not matter because both sides arrive sorted`() {
        assertTrue(
            ApkSignatures.accepts(
                Signers(listOf(keyA, keyB).sorted(), multipleSigners = true),
                Signers(listOf(keyB, keyA).sorted(), multipleSigners = true)
            )
        )
    }

    @Test
    fun `unreadable signatures are refused rather than treated as a match`() {
        assertFalse(ApkSignatures.accepts(null, single(keyA)))
        assertFalse(ApkSignatures.accepts(single(keyA), null))
        assertFalse(ApkSignatures.accepts(single(), single()))
        assertFalse(ApkSignatures.accepts(null, null))
    }
}
