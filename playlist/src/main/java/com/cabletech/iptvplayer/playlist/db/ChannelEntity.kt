package com.cabletech.iptvplayer.playlist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cabletech.iptvplayer.core.model.Channel

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = ChannelSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("source_id"), Index("tvg_id")],
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val name: String,
    @ColumnInfo(name = "stream_url") val streamUrl: String,
    @ColumnInfo(name = "logo_url") val logoUrl: String?,
    @ColumnInfo(name = "group_title") val groupTitle: String?,
    @ColumnInfo(name = "tvg_id") val tvgId: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
) {
    fun toDomain(): Channel = Channel(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        tvgId = tvgId,
    )
}

fun Channel.toEntity(sourceId: String, sortOrder: Int = 0) = ChannelEntity(
    id = id,
    sourceId = sourceId,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    tvgId = tvgId,
    sortOrder = sortOrder,
)
