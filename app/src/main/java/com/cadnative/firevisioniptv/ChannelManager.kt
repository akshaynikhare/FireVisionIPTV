package com.cadnative.firevisioniptv

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.json.JSONObject

/**
 * Manages channel and EPG synchronization with Fire TV's TIF database.
 * Uses Hilt EntryPoint to access DAO/repositories from non-injected components.
 */
class ChannelManager private constructor(
    private val context: Context,
    private val channelDao: ChannelDao,
    private val epgRepository: EpgRepository
) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ChannelManagerEntryPoint {
        fun channelDao(): ChannelDao
        fun epgRepository(): EpgRepository
    }

    private val inputId: String = TvContract.buildInputId(
        ComponentName(context, FireVisionTvInputService::class.java)
    )

    /**
     * Sync all active channels to Fire TV TIF database, then sync EPG.
     */
    suspend fun syncChannelsToTif() {
        Log.d(TAG, "Starting channel sync to TIF database")

        val appChannels = channelDao.getAllActiveChannels()
        val existingTifChannels = getExistingTifChannels()
        val appChannelIds = appChannels.map { it.id }.toSet()

        // Remove TIF channels that no longer exist in app
        for ((channelId, tifId) in existingTifChannels) {
            if (channelId !in appChannelIds) {
                try {
                    context.contentResolver.delete(TvContract.buildChannelUri(tifId), null, null)
                    Log.d(TAG, "Removed stale TIF channel: $channelId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing stale channel: $channelId", e)
                }
            }
        }

        // Insert or update channels
        for (channel in appChannels) {
            val existingId = existingTifChannels[channel.id]
            if (existingId != null) {
                updateChannel(existingId, channel)
            } else {
                insertChannel(channel)
            }
        }

        Log.d(TAG, "Channel sync completed. Synced ${appChannels.size} channels")

        syncEpgToTif()
    }

    /**
     * Sync EPG program data to TvContract.Programs for channels with a tvgId.
     */
    private suspend fun syncEpgToTif() {
        Log.d(TAG, "Starting EPG sync to TIF")

        try {
            epgRepository.ensureLoaded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load EPG data", e)
            return
        }

        val tifChannels = getExistingTifChannels()
        val appChannels = channelDao.getAllActiveChannels()
        var programCount = 0

        for (channel in appChannels) {
            val tvgId = channel.tvgId ?: continue
            val tifChannelId = tifChannels[channel.id] ?: continue

            try {
                val programsUri = TvContract.buildProgramsUriForChannel(tifChannelId)
                context.contentResolver.delete(programsUri, null, null)

                val (now, next) = epgRepository.getNowNext(tvgId)

                listOfNotNull(now, next).forEach { program ->
                    val values = ContentValues().apply {
                        put(TvContract.Programs.COLUMN_CHANNEL_ID, tifChannelId)
                        put(TvContract.Programs.COLUMN_TITLE, program.title)
                        program.description?.let {
                            put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, it)
                        }
                        put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, program.startTime.toEpochMilli())
                        put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, program.endTime.toEpochMilli())
                        program.icon?.let {
                            put(TvContract.Programs.COLUMN_POSTER_ART_URI, it)
                        }
                    }
                    context.contentResolver.insert(TvContract.Programs.CONTENT_URI, values)
                    programCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing EPG for channel: ${channel.name}", e)
            }
        }

        Log.d(TAG, "EPG sync completed. Synced $programCount programs")
    }

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

                    try {
                        val json = JSONObject(internalData ?: "{}")
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

    private fun insertChannel(channel: ChannelEntity): Uri? {
        val resolver = context.contentResolver

        return try {
            val internalData = JSONObject().apply {
                put("channelId", channel.id)
                put("channelUrl", channel.streamUrl)
                put("playbackDeepLinkUri", "firevisioniptv://play/channel/${channel.id}")
            }

            val values = ContentValues().apply {
                put(TvContract.Channels.COLUMN_INPUT_ID, inputId)
                put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER)
                put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.name))
                put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, channel.id)
                put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalData.toString())
                channel.logoUrl?.takeIf { it.isNotEmpty() }?.let {
                    put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, it)
                }
            }

            val channelUri = resolver.insert(TvContract.Channels.CONTENT_URI, values)
            Log.d(TAG, "Inserted channel: ${channel.name} -> $channelUri")
            channelUri
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting channel: ${channel.name}", e)
            null
        }
    }

    private fun updateChannel(tifChannelId: Long, channel: ChannelEntity) {
        val resolver = context.contentResolver

        try {
            val channelUri = TvContract.buildChannelUri(tifChannelId)

            val internalData = JSONObject().apply {
                put("channelId", channel.id)
                put("channelUrl", channel.streamUrl)
                put("playbackDeepLinkUri", "firevisioniptv://play/channel/${channel.id}")
            }

            val values = ContentValues().apply {
                put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.name))
                put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalData.toString())
                channel.logoUrl?.takeIf { it.isNotEmpty() }?.let {
                    put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, it)
                }
            }

            resolver.update(channelUri, values, null, null)
            Log.d(TAG, "Updated channel: ${channel.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating channel: ${channel.name}", e)
        }
    }

    fun removeAllChannels() {
        val resolver = context.contentResolver
        val channelsUri = TvContract.buildChannelsUriForInput(inputId)
        val deleted = resolver.delete(channelsUri, null, null)
        Log.d(TAG, "Removed $deleted channels from TIF")
    }

    private fun truncateName(name: String?): String {
        if (name == null) return "Unknown"
        if (name.length <= 25) return name
        return "${name.substring(0, 22)}..."
    }

    fun triggerChannelUpdate() {
        val intent = Intent(TvContract.ACTION_INITIALIZE_PROGRAMS).apply {
            component = ComponentName(context, ChannelUpdateReceiver::class.java)
        }
        context.sendBroadcast(intent)
        Log.d(TAG, "Triggered channel update broadcast")
    }

    companion object {
        private const val TAG = "ChannelManager"

        fun create(context: Context): ChannelManager {
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext, ChannelManagerEntryPoint::class.java
            )
            return ChannelManager(
                context = appContext,
                channelDao = entryPoint.channelDao(),
                epgRepository = entryPoint.epgRepository()
            )
        }
    }
}
