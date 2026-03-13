package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel

/**
 * Repository interface for M3U playlist operations.
 * 
 * This interface defines the contract for parsing and managing M3U playlists,
 * which are used to import channel lists in IPTV format.
 */
interface PlaylistRepository {
    
    /**
     * Parse an M3U playlist from a URL.
     * 
     * Downloads and parses the M3U playlist file, extracting channel information.
     * 
     * @param url The URL of the M3U playlist
     * @return Result with list of parsed channels or error
     */
    suspend fun parsePlaylistFromUrl(url: String): Result<List<Channel>>
    
    /**
     * Parse an M3U playlist from a string content.
     * 
     * Parses the M3U playlist content directly, extracting channel information.
     * 
     * @param content The M3U playlist content as a string
     * @return Result with list of parsed channels or error
     */
    suspend fun parsePlaylistFromString(content: String): Result<List<Channel>>
    
    /**
     * Import channels from a parsed playlist.
     * 
     * Saves the parsed channels to the local database, replacing existing data.
     * 
     * @param channels The list of channels to import
     * @return Result indicating success or failure
     */
    suspend fun importChannels(channels: List<Channel>): Result<Unit>
}
