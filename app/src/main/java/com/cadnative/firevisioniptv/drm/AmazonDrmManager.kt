package com.cadnative.firevisioniptv.drm

import android.content.Context
import android.util.Log
import com.amazon.device.drm.LicensingService
import com.amazon.device.drm.model.LicenseResponse
import com.amazon.device.drm.LicensingListener

/**
 * Manages Amazon Appstore DRM license verification.
 *
 * On Fire TV / Amazon Appstore, this verifies the user has a valid license
 * (i.e. they obtained the app through the Appstore). On non-Amazon devices
 * or during development, the callback may not fire — the app continues normally.
 */
class AmazonDrmManager(private val context: Context) {

    companion object {
        private const val TAG = "AmazonDRM"
    }

    private var onLicenseResult: ((licensed: Boolean) -> Unit)? = null

    private val licensingListener = object : LicensingListener {
        override fun onLicenseCommandResponse(response: LicenseResponse?) {
            val status = response?.requestStatus
            Log.d(TAG, "License response: $status")

            when (status) {
                LicenseResponse.RequestStatus.LICENSED -> {
                    Log.d(TAG, "App is licensed")
                    onLicenseResult?.invoke(true)
                }
                LicenseResponse.RequestStatus.NOT_LICENSED -> {
                    Log.w(TAG, "App is NOT licensed")
                    onLicenseResult?.invoke(false)
                }
                LicenseResponse.RequestStatus.ERROR_VERIFICATION -> {
                    Log.w(TAG, "License verification error — allowing app to continue")
                    onLicenseResult?.invoke(true)
                }
                LicenseResponse.RequestStatus.ERROR_INVALID_LICENSING_KEYS -> {
                    Log.e(TAG, "Invalid licensing keys")
                    onLicenseResult?.invoke(true)
                }
                LicenseResponse.RequestStatus.EXPIRED -> {
                    Log.w(TAG, "License expired")
                    onLicenseResult?.invoke(false)
                }
                else -> {
                    Log.w(TAG, "Unknown license status: $status — allowing app")
                    onLicenseResult?.invoke(true)
                }
            }
        }
    }

    fun verifyLicense(callback: (licensed: Boolean) -> Unit) {
        onLicenseResult = callback
        try {
            LicensingService.verifyLicense(context, licensingListener)
            Log.d(TAG, "License verification requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate license verification", e)
            // Allow app to continue if DRM service is unavailable (e.g. non-Amazon device)
            callback(true)
        }
    }
}
