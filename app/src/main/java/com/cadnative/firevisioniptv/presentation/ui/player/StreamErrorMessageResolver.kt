package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.annotation.StringRes
import com.cadnative.firevisioniptv.R

data class StreamErrorContext(
    val errorMessage: String,
    val lastCheckedAt: Long?,
    val previousStatus: String?,
    val categoryOfflineCount: Int,
    val categoryScannedCount: Int
)

/**
 * Resource ids rather than strings: this resolver has no Context and is a pure
 * object, which is what makes it directly unit-testable. The composable that
 * renders the message does the resolving.
 */
data class StreamErrorMessage(
    @StringRes val titleRes: Int,
    @StringRes val explanationRes: Int,
    /** Appended when the channel was working within the last hour. */
    @StringRes val recentSuffixRes: Int? = null
)

object StreamErrorMessageResolver {

    private const val RECENT_THRESHOLD_MS = 3_600_000L // 1 hour

    fun resolve(context: StreamErrorContext): StreamErrorMessage {
        // A category-wide outage says more than any per-channel diagnosis.
        if (context.categoryScannedCount >= 3 &&
            context.categoryOfflineCount >= context.categoryScannedCount / 2
        ) {
            return StreamErrorMessage(
                R.string.stream_err_provider_title,
                R.string.stream_err_provider_body
            ).withRecentSuffix(context)
        }

        val (title, explanation) = when {
            context.errorMessage.contains("Network connection", ignoreCase = true) ->
                R.string.stream_err_connection_title to R.string.stream_err_connection_body

            context.errorMessage.contains("Server error", ignoreCase = true) ->
                R.string.stream_err_server_title to R.string.stream_err_server_body

            context.errorMessage.contains("Invalid stream format", ignoreCase = true) ->
                R.string.stream_err_format_title to R.string.stream_err_format_body

            context.errorMessage.contains("All streams exhausted", ignoreCase = true) ->
                R.string.stream_err_offline_title to R.string.stream_err_offline_body

            else ->
                R.string.stream_err_generic_title to R.string.stream_err_generic_body
        }

        return StreamErrorMessage(title, explanation).withRecentSuffix(context)
    }

    private fun StreamErrorMessage.withRecentSuffix(context: StreamErrorContext): StreamErrorMessage {
        if (context.previousStatus == "ONLINE" && context.lastCheckedAt != null) {
            val elapsed = System.currentTimeMillis() - context.lastCheckedAt
            if (elapsed < RECENT_THRESHOLD_MS) {
                return copy(recentSuffixRes = R.string.stream_err_recent_suffix)
            }
        }
        return this
    }
}
