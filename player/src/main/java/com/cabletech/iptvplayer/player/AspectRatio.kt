package com.cabletech.iptvplayer.player

import androidx.media3.common.C

/** Video aspect-ratio / scaling modes presented to the user. */
enum class AspectRatio(val scalingMode: Int) {
    /** Fill the screen, cropping if necessary (default for TV). */
    FILL(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING),

    /** Fit inside the screen bounds, letterboxing/pillarboxing as needed. */
    FIT(C.VIDEO_SCALING_MODE_SCALE_TO_FIT),
}
