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
     * Whether an update signed with [archive] may replace an app signed with [installed].
     *
     * Not equality. Android supports signing-key rotation: an app still running the
     * old key reports history `[A]`, while a legitimate update signed with the
     * rotated key reports the proof-of-rotation lineage `[A, B]`. Requiring
     * identical histories rejects exactly the transition the platform allows —
     * and rejects it in the one component that cannot be fixed by an update,
     * because it *is* the updater.
     *
     * Containment is no weaker than equality: the lineage in an APK's signing
     * block has to be signed by each preceding key, so an attacker without the
     * original private key cannot claim [installed] in it, and the platform
     * installer enforces the same rule independently. An APK signed only with a
     * key the installed app has already rotated away from is still refused,
     * since the installed history would not be a subset.
     */
    fun accepts(installed: List<String>?, archive: List<String>?): Boolean {
        if (installed.isNullOrEmpty() || archive.isNullOrEmpty()) return false
        return archive.containsAll(installed)
    }

    /** Digests of the certificates the installed app is signed with, or null if unreadable. */
    fun installed(pm: PackageManager, packageName: String): List<String>? =
        runCatching {
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            digests(pm.getPackageInfo(packageName, flags))
        }.getOrNull()

    /** Digests of the certificates an APK file on disk is signed with, or null if unreadable. */
    fun archive(pm: PackageManager, apkPath: String): List<String>? =
        runCatching {
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            digests(pm.getPackageArchiveInfo(apkPath, flags))
        }.getOrNull()

    private fun digests(info: PackageInfo?): List<String>? {
        if (info == null) return null
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return null
            // Rotated keys report history; multi-signer APKs report all current signers.
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        if (raw.isNullOrEmpty()) return null
        return raw.map { sha256(it.toByteArray()) }.sorted()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
