package com.cadnative.firevisioniptv

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.core.content.ContextCompat

/**
 * Picture-in-picture wiring for the mobile player. Owned by the activity;
 * PlayerScreen attaches while active and feeds playback-state updates so
 * `setAutoEnterEnabled` (API 31+) stays correct, and receives prev/next
 * channel actions from the PiP window's RemoteActions.
 */
class PipController(private val activity: Activity) {

    enum class PipAction { PREV_CHANNEL, NEXT_CHANNEL }

    /** Set by PlayerScreen while it is composed; routes PiP RemoteActions to the ViewModel. */
    var onPipAction: ((PipAction) -> Unit)? = null

    private var receiver: BroadcastReceiver? = null

    fun attach() {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getIntExtra(EXTRA_ACTION, -1)) {
                    ACTION_PREV -> onPipAction?.invoke(PipAction.PREV_CHANNEL)
                    ACTION_NEXT -> onPipAction?.invoke(PipAction.NEXT_CHANNEL)
                }
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun detach() {
        receiver?.let { runCatching { activity.unregisterReceiver(it) } }
        receiver = null
        onPipAction = null
        clearAutoEnter()
    }

    /** Rebuild PiP params — call on play/pause changes and channel switches. */
    fun update(isPlaying: Boolean, canZap: Boolean) {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (canZap) {
            builder.setActions(
                listOf(
                    remoteAction(ACTION_PREV, "Previous channel", android.R.drawable.ic_media_previous),
                    remoteAction(ACTION_NEXT, "Next channel", android.R.drawable.ic_media_next)
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlaying)
        }
        runCatching { activity.setPictureInPictureParams(builder.build()) }
    }

    /** Leaving the player: browsing screens must never auto-enter PiP. */
    fun clearAutoEnter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                activity.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .setAutoEnterEnabled(false)
                        .setActions(emptyList())
                        .build()
                )
            }
        }
    }

    /** Manual PiP entry from the chrome's PiP button. */
    fun enterPip() {
        runCatching {
            activity.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    private fun remoteAction(action: Int, title: String, iconRes: Int): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(activity.packageName)
            .putExtra(EXTRA_ACTION, action)
        val pending = PendingIntent.getBroadcast(
            activity,
            action,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(Icon.createWithResource(activity, iconRes), title, title, pending)
    }

    companion object {
        private const val ACTION_PIP_CONTROL = "com.cadnative.firevisioniptv.PIP_CONTROL"
        private const val EXTRA_ACTION = "pip_action"
        private const val ACTION_PREV = 1
        private const val ACTION_NEXT = 2
    }
}
