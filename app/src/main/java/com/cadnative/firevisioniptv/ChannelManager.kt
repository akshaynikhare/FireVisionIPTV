package com.cadnative.firevisioniptv

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import org.json.JSONObject

/**
 * Manages channel synchronization with Fire TV's TIF database.
 * Handles inserting, updating, and removing channels.
 */
class ChannelManager(private val context: Context) {

    private val inputId: String = TvContract.buildInputId(
        ComponentName(context, FireVisionTvInputService::class.java)
    )

    /**
     * Sync all entitled channels to Fire TV TIF database.
     */
    fun syncChannelsToTif() {
        Log.d(TAG, "Starting channel sync to TIF database")

        // Get current channels from the app (currently empty - channels are managed server-side)
        // TIF integration is available but not actively used
        val appChannels = mutableListOf<Channel>()

        // Get existing TIF channels
        val existingTifChannels = getExistingTifChannels()

        // Insert or update channels
        for (channel in appChannels) {
            val channelId = channel.channelId
            val existingId = existingTifChannels[channelId]
            if (existingId != null) {
                updateChannel(existingId, channel)
            } else {
                insertChannel(channel)
            }
        }

        Log.d(TAG, "Channel sync completed. Synced ${appChannels.size} channels")
    }

    /**
     * Get existing channels from TIF database.
     */
    private fun getExistingTifChannels(): Map<String, Long> {
        val existingChannels = mutableMapOf<String, Long>()
        val resolver = context.contentResolver

        val channelsUri = TvContract.buildChannelsUriForInput(inputId)
        val projection = arrayOf(
            TvContract.Channels._ID,
            TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA
        )

        try {
            resolver.query(channelsUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val internalData = cursor.getString(1)

                    // Extract channel ID from internal data
                    try {
                        val json = JSONObject(internalData)
                        val channelId = json.optString("channelId")
                        if (channelId.isNotEmpty()) {
                            existingChannels[channelId] = id
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing internal data", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying TIF channels", e)
        }

        return existingChannels
    }

    /**
     * Insert a new channel into TIF database.
     */
    private fun insertChannel(channel: Channel): Uri? {
        val resolver = context.contentResolver

        return try {
            // Build internal provider data with channel info and deep link
            val internalData = JSONObject().apply {
                put("channelId", channel.channelId)
                put("channelUrl", channel.channelUrl)
                // Create deep link for playback using URI scheme
                put("playbackDeepLinkUri", "firevisioniptv://play/channel/${channel.channelId}")
            }

            // Build channel content values
            val values = ContentValues().apply {
                put(TvContract.Channels.COLUMN_INPUT_ID, inputId)
                put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER)
                put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.channelName))
                put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, channel.channelId)
                put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalData.toString())
                // Add channel logo if available
                channel.channelImg?.takeIf { it.isNotEmpty() }?.let {
                    put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, it)
                }
            }

            val channelUri = resolver.insert(TvContract.Channels.CONTENT_URI, values)
            Log.d(TAG, "Inserted channel: ${channel.channelName} -> $channelUri")
            channelUri
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting channel: ${channel.channelName}", e)
            null
        }
    }

    /**
     * Update an existing channel in TIF database.
     */
    private fun updateChannel(tifChannelId: Long, channel: Channel) {
        val resolver = context.contentResolver

        try {
            val channelUri = TvContract.buildChannelUri(tifChannelId)

            val values = ContentValues().apply {
                put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.channelName))
                channel.channelImg?.takeIf { it.isNotEmpty() }?.let {
                    put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, it)
                }
            }

            resolver.update(channelUri, values, null, null)
            Log.d(TAG, "Updated channel: ${channel.channelName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating channel: ${channel.channelName}", e)
        }
    }

    /**
     * Remove all channels from TIF database.
     */
    fun removeAllChannels() {
        val resolver = context.contentResolver
        val channelsUri = TvContract.buildChannelsUriForInput(inputId)

        val deleted = resolver.delete(channelsUri, null, null)
        Log.d(TAG, "Removed $deleted channels from TIF")
    }

    /**
     * Truncate channel name to 25 characters (Fire TV requirement).
     */
    private fun truncateName(name: String?): String {
        if (name == null) return "Unknown"
        if (name.length <= 25) return name
        return "${name.substring(0, 22)}..."
    }

    /**
     * Trigger channel initialization broadcast.
     */
    fun triggerChannelUpdate() {
        val intent = Intent(TvContract.ACTION_INITIALIZE_PROGRAMS).apply {
            component = ComponentName(context, ChannelUpdateReceiver::class.java)
        }
        context.sendBroadcast(intent)
        Log.d(TAG, "Triggered channel update broadcast")
    }

    companion object {
        private const val TAG = "ChannelManager"
    }
}
