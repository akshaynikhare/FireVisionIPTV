package com.cadnative.firevisioniptv.presentation.ui.player

import android.content.Context
import android.content.pm.PackageManager

fun isTvDevice(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

fun isMobileDevice(context: Context): Boolean = !isTvDevice(context)
