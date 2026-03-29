package com.cabletech.iptvplayer.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.cabletech.iptvplayer.core.model.Channel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Leanback [Presenter] that renders a [ChannelWithEpg] (or plain [Channel]) as a card.
 *
 * Shows the channel logo, name, and — when EPG data is available — the current and next
 * programme titles in the card footer.
 */
class ChannelPresenter : Presenter() {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_card, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val (channel, nowNext) = when (item) {
            is ChannelWithEpg -> item.channel to item.nowNext
            is Channel -> item to null
            else -> return
        }

        val view = viewHolder.view
        val nameView = view.findViewById<TextView>(R.id.channel_name)
        val logoView = view.findViewById<ImageView>(R.id.channel_logo)
        val nowView = view.findViewById<TextView>(R.id.epg_now)
        val nextView = view.findViewById<TextView>(R.id.epg_next)

        nameView.text = channel.name

        if (channel.logoUrl != null) {
            // Load with Coil/Glide in a real integration:
            // Coil.load(logoView, channel.logoUrl)
            logoView.contentDescription = channel.name
        } else {
            logoView.setImageResource(R.drawable.ic_channel_placeholder)
        }

        val now = nowNext?.now
        val next = nowNext?.next

        if (now != null) {
            val start = timeFmt.format(Date(now.startUtcMs))
            nowView.text = "$start  ${now.title}"
            nowView.visibility = View.VISIBLE
        } else {
            nowView.visibility = View.GONE
        }

        if (next != null) {
            val start = timeFmt.format(Date(next.startUtcMs))
            nextView.text = "$start  ${next.title}"
            nextView.visibility = View.VISIBLE
        } else {
            nextView.visibility = View.GONE
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val view = viewHolder.view
        view.findViewById<ImageView>(R.id.channel_logo).setImageDrawable(null)
        view.findViewById<TextView>(R.id.epg_now).visibility = View.GONE
        view.findViewById<TextView>(R.id.epg_next).visibility = View.GONE
    }
}
