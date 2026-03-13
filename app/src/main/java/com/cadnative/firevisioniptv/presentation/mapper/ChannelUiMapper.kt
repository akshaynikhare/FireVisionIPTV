package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between Channel domain models and ChannelUiModel.
 * 
 * This mapper handles transformations between the domain layer and
 * presentation layer, allowing UI-specific optimizations and formatting.
 */
@Singleton
class ChannelUiMapper @Inject constructor() {
    
    /**
     * Convert a domain Channel to a ChannelUiModel.
     */
    fun toUiModel(channel: Channel): ChannelUiModel {
        return ChannelUiModel(
            id = channel.id,
            name = channel.name,
            logoUrl = channel.logoUrl,
            streamUrl = channel.streamUrl,
            category = channel.category,
            isFavorite = channel.isFavorite
        )
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
