package com.cadnative.firevisioniptv.presentation.ui.player

data class StreamErrorContext(
    val errorMessage: String,
    val lastCheckedAt: Long?,
    val previousStatus: String?,
    val categoryOfflineCount: Int,
    val categoryScannedCount: Int
)

data class StreamErrorMessage(
    val title: String,
    val explanation: String
)

object StreamErrorMessageResolver {

    private const val RECENT_THRESHOLD_MS = 3_600_000L // 1 hour

    fun resolve(context: StreamErrorContext): StreamErrorMessage {
        // Category-wide outage check
        if (context.categoryScannedCount >= 3 &&
            context.categoryOfflineCount >= context.categoryScannedCount / 2
        ) {
            return StreamErrorMessage(
                title = "Source Provider Issue",
                explanation = "Multiple channels in this group are down. " +
                        "This usually means the source provider is experiencing problems."
            ).withRecentSuffix(context)
        }

        val (title, explanation) = when {
            context.errorMessage.contains("Network connection", ignoreCase = true) ->
                "Connection Lost" to
                        "Your device lost its network connection. Check your Wi-Fi or ethernet and try again."

            context.errorMessage.contains("Server error", ignoreCase = true) ->
                "Server Not Responding" to
                        "The channel's streaming server isn't responding. " +
                        "This is a third-party server outside our control."

            context.errorMessage.contains("Invalid stream format", ignoreCase = true) ->
                "Stream Format Error" to
                        "The stream format has changed or is incompatible. " +
                        "The source provider may have updated their feed."

            context.errorMessage.contains("All streams exhausted", ignoreCase = true) ->
                "Channel Offline" to
                        "We tried all available sources for this channel and none are responding right now."

            else ->
                "Stream Unavailable" to
                        "This channel's stream couldn't be loaded. " +
                        "IPTV streams depend on third-party sources that can go offline at any time."
        }

        return StreamErrorMessage(title, explanation).withRecentSuffix(context)
    }

    private fun StreamErrorMessage.withRecentSuffix(context: StreamErrorContext): StreamErrorMessage {
        if (context.previousStatus == "ONLINE" && context.lastCheckedAt != null) {
            val elapsed = System.currentTimeMillis() - context.lastCheckedAt
            if (elapsed < RECENT_THRESHOLD_MS) {
                return copy(
                    explanation = "$explanation This channel was working recently and may come back soon."
                )
            }
        }
        return this
    }
}
