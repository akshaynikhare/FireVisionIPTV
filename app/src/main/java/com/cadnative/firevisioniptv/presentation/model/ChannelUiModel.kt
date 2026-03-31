package com.cadnative.firevisioniptv.presentation.model

import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus

data class ChannelUiModel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String? = null,
    val category: String,
    val isFavorite: Boolean,
    val tvgId: String? = null,
    val healthStatus: ChannelHealthStatus = ChannelHealthStatus.UNKNOWN,
    val thumbnailPath: String? = null,
    val alternateStreamUrls: List<String> = emptyList(),
    val nowProgramTitle: String? = null
)
