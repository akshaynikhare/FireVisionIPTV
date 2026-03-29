package com.cabletech.iptvplayer.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter

/**
 * Sentinel object representing the "Open Programme Guide" action in the browse row.
 */
object GuideAction

/**
 * Leanback [Presenter] that renders the [GuideAction] as a card prompting the user
 * to open the full EPG grid.
 */
class GuideActionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guide_action, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val label = viewHolder.view.findViewById<TextView>(R.id.guide_action_label)
        label.text = viewHolder.view.context.getString(R.string.action_open_guide)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
