package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelUiMapper @Inject constructor() {

    fun toUiModel(
        channel: Channel,
        healthStatus: ChannelHealthStatus = ChannelHealthStatus.UNKNOWN,
        thumbnailPath: String? = null
    ): ChannelUiModel {
        return ChannelUiModel(
            id = channel.id,
            name = channel.name,
            logoUrl = channel.logoUrl,
            streamUrl = channel.streamUrl,
            category = channel.category,
            isFavorite = channel.isFavorite,
            healthStatus = healthStatus,
            thumbnailPath = thumbnailPath
        )
    }

    fun toUiModelsWithHealth(
        channels: List<Channel>,
        healthEntities: List<ChannelHealthEntity>
    ): List<ChannelUiModel> {
        val healthMap = healthEntities.associateBy { it.channelId }
        return channels.map { channel ->
            val healthEntity = healthMap[channel.id]
            val status = healthEntity?.status?.let { s ->
                try { ChannelHealthStatus.valueOf(s) }
                catch (_: Exception) { ChannelHealthStatus.UNKNOWN }
            } ?: ChannelHealthStatus.UNKNOWN
            toUiModel(channel, status, healthEntity?.thumbnailPath)
        }
    }
    
    /**
     * Convert a ChannelUiModel back to a domain Channel.
     * 
     * Note: This creates a minimal Channel with only UI-available data.
     * Full channel data should be retrieved from the repository.
     */
    fun fromUiModel(uiModel: ChannelUiModel): Channel {
        return Channel(
            id = uiModel.id,
            name = uiModel.name,
            streamUrl = uiModel.streamUrl ?: "",
            logoUrl = uiModel.logoUrl,
            category = uiModel.category,
            language = null,
            country = null,
            isFavorite = uiModel.isFavorite
        )
    }
}
