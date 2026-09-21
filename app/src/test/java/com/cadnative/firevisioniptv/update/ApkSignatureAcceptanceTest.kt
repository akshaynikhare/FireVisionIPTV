package com.cadnative.firevisioniptv.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The predicate that decides whether a downloaded APK may replace the installed
 * app. Plain JVM: it is pure set logic, deliberately extracted out of AppUpdater
 * so it can be reasoned about without a PackageManager.
 *
 * The case that matters is signing-key rotation, because the failure is
 * unrecoverable in the field: if this rejects a legitimate rotated update, the
 * only component that could ship the fix is the updater itself.
 */
class ApkSignatureAcceptanceTest {

    private val keyA = "aa11"
    private val keyB = "bb22"
    private val keyC = "cc33"

    @Test
    fun `an identically signed update is accepted`() {
        assertTrue(ApkSignatures.accepts(listOf(keyA), listOf(keyA)))
    }

    @Test
    fun `a rotated update is accepted when its lineage includes the installed key`() {
        // Installed app still on the old key; update carries proof of rotation.
        assertTrue(ApkSignatures.accepts(listOf(keyA), listOf(keyA, keyB)))
    }

    @Test
    fun `an already-rotated install accepts an update carrying the same lineage`() {
        assertTrue(ApkSignatures.accepts(listOf(keyA, keyB), listOf(keyA, keyB)))
    }

    @Test
    fun `an unrelated signing key is refused`() {
        assertFalse(ApkSignatures.accepts(listOf(keyA), listOf(keyC)))
    }

    @Test
    fun `an update that drops a key the install already has is refused`() {
        // Signed only with the retired key: the installed lineage is not a subset,
        // so this is not a legitimate successor.
        assertFalse(ApkSignatures.accepts(listOf(keyA, keyB), listOf(keyA)))
    }

    @Test
    fun `an update adding an unrelated co-signer alongside the known key is accepted`() {
        // Containment, not equality — and safe, because a signer list is only
        // extensible by someone who can sign with the existing key.
        assertTrue(ApkSignatures.accepts(listOf(keyA), listOf(keyA, keyC)))
    }

    @Test
    fun `order does not matter because both sides arrive sorted`() {
        assertTrue(ApkSignatures.accepts(listOf(keyA, keyB), listOf(keyB, keyA)))
    }

    @Test
    fun `unreadable signatures are refused rather than treated as a match`() {
        assertFalse(ApkSignatures.accepts(null, listOf(keyA)))
        assertFalse(ApkSignatures.accepts(listOf(keyA), null))
        assertFalse(ApkSignatures.accepts(emptyList(), emptyList()))
        assertFalse(ApkSignatures.accepts(null, null))
    }
}
