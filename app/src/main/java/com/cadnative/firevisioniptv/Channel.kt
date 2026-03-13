package com.cadnative.firevisioniptv

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Channel(
    var channelName: String = "",
    var channelId: String = "",
    var channelUrl: String = "",
    var channelImg: String = "",
    var channelGroup: String = "",
    var channelLanguage: String = "",
    var channelDrmKey: String = "",
    var channelDrmType: String = ""
) : Parcelable
