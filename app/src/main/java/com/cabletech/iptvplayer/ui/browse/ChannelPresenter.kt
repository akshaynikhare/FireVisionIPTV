package com.cabletech.iptvplayer.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.cabletech.iptvplayer.core.model.Channel

/**
 * Leanback [Presenter] that renders a [Channel] as a card in the browse grid.
 *
 * Shows the channel logo (placeholder when absent) and name label below the card.
 */
class ChannelPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_card, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val channel = item as Channel
        val view = viewHolder.view

        val nameView = view.findViewById<TextView>(R.id.channel_name)
        val logoView = view.findViewById<ImageView>(R.id.channel_logo)

        nameView.text = channel.name

        if (channel.logoUrl != null) {
            // In a real integration load with Coil/Glide:
            // Coil.load(logoView, channel.logoUrl)
            logoView.contentDescription = channel.name
        } else {
            logoView.setImageResource(R.drawable.ic_channel_placeholder)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val view = viewHolder.view
        view.findViewById<ImageView>(R.id.channel_logo).setImageDrawable(null)
    }
}
