package com.cabletech.iptvplayer.ui.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.cabletech.iptvplayer.R
import com.cabletech.iptvplayer.epg.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Leanback [Presenter] that renders an [EpgProgramme] as a tile in the EPG grid.
 *
 * Card width is scaled proportionally to the programme's duration relative to a
 * 30-minute baseline so the guide conveys a time-based layout.
 */
class EpgProgrammePresenter : Presenter() {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** Base width in dp for a 30-minute programme. */
    private val baseWidthDp = 160

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_programme, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val prog = item as EpgProgramme
        val view = viewHolder.view
        val ctx = view.context

        val titleView = view.findViewById<TextView>(R.id.epg_prog_title)
        val timeView = view.findViewById<TextView>(R.id.epg_prog_time)

        titleView.text = prog.title

        val start = timeFmt.format(Date(prog.startUtcMs))
        val end = timeFmt.format(Date(prog.stopUtcMs))
        timeView.text = "$start – $end"

        // Scale card width proportionally to duration (30 min baseline = baseWidthDp)
        val durationMin = TimeUnit.MILLISECONDS.toMinutes(prog.stopUtcMs - prog.startUtcMs)
        val density = ctx.resources.displayMetrics.density
        val widthPx = (max(durationMin, 15) * baseWidthDp / 30.0 * density).toInt()
        val lp = view.layoutParams ?: ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.width = widthPx
        view.layoutParams = lp
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
