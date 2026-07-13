package com.cadnative.firevisioniptv.presentation.ui

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Device performance profile, computed once at startup.
 * Low-end TV boxes (low RAM or <= 4 cores) get reduced motion.
 */
data class PerfProfile(val reduceMotion: Boolean)

val LocalPerfProfile = staticCompositionLocalOf { PerfProfile(reduceMotion = false) }

fun detectPerfProfile(context: Context): PerfProfile {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRam = activityManager?.isLowRamDevice == true
    val fewCores = Runtime.getRuntime().availableProcessors() <= 4
    return PerfProfile(reduceMotion = isLowRam || fewCores)
}
