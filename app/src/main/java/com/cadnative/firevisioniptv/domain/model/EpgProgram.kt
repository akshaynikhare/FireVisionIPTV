package com.cadnative.firevisioniptv.domain.model

import java.time.Instant

data class EpgProgram(
    val channelEpgId: String,
    val title: String,
    val description: String?,
    val startTime: Instant,
    val endTime: Instant,
    val icon: String?
)
