package com.cadnative.firevisioniptv.update

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Reads APK signing certificates across API levels.
 *
 * `PackageInfo.signingInfo` only exists from API 28. Below that the field access
 * throws [NoSuchFieldError] — an [Error], which `catch (Exception)` does not catch,
 * so the caller cannot simply guard with a try/catch. Getting this wrong disables
 * self-update entirely on older devices rather than failing loudly, hence the
 * explicit branch.
 *
 * Signatures come back as sorted SHA-256 digests so callers compare sets rather
 * than a single certificate: an app signed by multiple signers, or one whose key
 * has been rotated, has more than one valid certificate and comparing only the
 * first would reject a legitimate update.
 */
internal object ApkSignatures {

    /**
     * Certificate digests plus the one bit needed to interpret them.
     *
     * A flat list cannot distinguish the two ways a package ends up with more
     * than one certificate, and they have opposite rules: `[A, B]` from a
     * single signer is a rotation *lineage*, where an update carrying the
     * lineage is legitimate; `[A, B]` from two signers is the *current signer
     * set*, which an update has to match exactly. [multipleSigners] is what
     * tells them apart.
     */
    data class Signers(
        val digests: List<String>,
        val multipleSigners: Boolean
    )

    /**
     * Whether an update signed with [archive] may replace an app signed with [installed].
     *
     * Mirrors what the platform installer will do, because anything looser only
     * means launching an installer that then refuses the APK — leaving the
     * download on disk and reporting success to the user.
     *
     * - **Single signer**: containment, not equality. Android supports key
     *   rotation, so an app still on the old key reports `[A]` while a
     *   legitimate rotated update reports the proof-of-rotation lineage
     *   `[A, B]`. Demanding identical histories rejects exactly the transition
     *   the platform allows, in the one component that cannot ship its own fix.
     *   This is not weaker than equality: a lineage has to be signed by each
     *   preceding key, so an attacker without the original private key cannot
     *   claim [installed] in it.
     * - **Multiple signers on either side**: exact set equality. Rotation is
     *   only defined for single-signer packages; a multi-signer package must
     *   present the same current signer set. Containment would accept
     *   `[A] -> [A, C]`, which Android rejects.
     */
    fun accepts(installed: Signers?, archive: Signers?): Boolean {
        if (installed == null || archive == null) return false
        if (installed.digests.isEmpty() || archive.digests.isEmpty()) return false
        if (installed.multipleSigners || archive.multipleSigners) {
            // Both sides arrive sorted, so list equality is set equality.
            return installed.digests == archive.digests
        }
        return archive.digests.containsAll(installed.digests)
    }

    /** Signing certificates of the installed app, or null if unreadable. */
    fun installed(pm: PackageManager, packageName: String): Signers? =
        runCatching { signers(pm.getPackageInfo(packageName, signatureFlags())) }.getOrNull()

    /** Signing certificates of an APK file on disk, or null if unreadable. */
    fun archive(pm: PackageManager, apkPath: String): Signers? =
        runCatching { signers(pm.getPackageArchiveInfo(apkPath, signatureFlags())) }.getOrNull()

    @Suppress("DEPRECATION")
    private fun signatureFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

    private fun signers(info: PackageInfo?): Signers? {
        if (info == null) return null
        val multipleSigners: Boolean
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return null
            multipleSigners = signingInfo.hasMultipleSigners()
            if (multipleSigners) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            val signatures = info.signatures
            // Below 28 there is no lineage to read: `signatures` is always the
            // current signer set, so more than one of them means multi-signer.
            multipleSigners = (signatures?.size ?: 0) > 1
            signatures
        }
        if (raw.isNullOrEmpty()) return null
        return Signers(raw.map { sha256(it.toByteArray()) }.sorted(), multipleSigners)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
