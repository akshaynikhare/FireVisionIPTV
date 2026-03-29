package com.cabletech.iptvplayer.ui.epg

import android.os.Bundle
import android.view.View
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.cabletech.iptvplayer.R
import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.epg.EpgProgramme
import com.cabletech.iptvplayer.epg.EpgRepository
import com.cabletech.iptvplayer.epg.db.EpgDatabase
import com.cabletech.iptvplayer.player.ui.PlayerActivity
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * EPG programme guide showing a time-based schedule for each channel.
 *
 * Uses Leanback [BrowseSupportFragment] where each header row corresponds to a channel and
 * each row contains [EpgProgramme] tiles covering a 6-hour window from now.
 * Selecting a programme launches [PlayerActivity] for its channel.
 */
class EpgGridFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    // Maps tvg-id -> Channel so we can launch PlayerActivity on programme selection
    private val channelByTvgId = mutableMapOf<String, Channel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = getString(R.string.title_epg_guide)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = rowsAdapter

        val playlistDb = PlaylistDatabase.getInstance(requireContext())
        val channelRepo = ChannelSourceRepository(
            sourceDao = playlistDb.channelSourceDao(),
            channelDao = playlistDb.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )

        val epgDb = EpgDatabase.getInstance(requireContext())
        val epgRepo = EpgRepository(dao = epgDb.epgDao(), client = OkHttpClientFactory.create())

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is EpgProgramme) {
                val channel = channelByTvgId[item.channelId] ?: return@OnItemViewClickedListener
                startActivity(PlayerActivity.intent(requireContext(), channel))
            }
        }

        loadGuide(channelRepo, epgRepo)
    }

    private fun loadGuide(channelRepo: ChannelSourceRepository, epgRepo: EpgRepository) {
        lifecycleScope.launch {
            val channels = channelRepo.observeChannels().first()

            val nowMs = System.currentTimeMillis()
            val windowMs = nowMs + TimeUnit.HOURS.toMillis(6)

            var rowIndex = 0
            channels.filter { it.tvgId != null }.forEach { channel ->
                val tvgId = channel.tvgId!!
                channelByTvgId[tvgId] = channel

                val programmes = epgRepo.observeSchedule(tvgId, nowMs, windowMs).first()
                if (programmes.isEmpty()) return@forEach

                val progAdapter = ArrayObjectAdapter(EpgProgrammePresenter())
                progAdapter.addAll(0, programmes)

                rowsAdapter.add(ListRow(HeaderItem(rowIndex.toLong(), channel.name), progAdapter))
                rowIndex++
            }
        }
    }
}
