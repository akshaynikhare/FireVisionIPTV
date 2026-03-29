package com.cabletech.iptvplayer.ui.browse

import android.os.Bundle
import android.view.View
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.epg.EpgRepository
import com.cabletech.iptvplayer.epg.NowNextInfo
import com.cabletech.iptvplayer.epg.db.EpgDatabase
import com.cabletech.iptvplayer.player.ui.PlayerActivity
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import com.cabletech.iptvplayer.ui.epg.EpgGridFragment
import com.cabletech.iptvplayer.ui.onboarding.OnboardingFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main channel browsing screen.
 *
 * Uses Leanback [BrowseSupportFragment] to render channels grouped by [Channel.groupTitle].
 * Channel cards display now/next EPG programme info when available (matched via [Channel.tvgId]).
 * Selecting a channel launches [PlayerActivity]; a dedicated "Programme Guide" row opens
 * the full [EpgGridFragment].
 */
class ChannelBrowseFragment : BrowseSupportFragment() {

    private lateinit var channelRepo: ChannelSourceRepository
    private lateinit var epgRepo: EpgRepository
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = getString(R.string.title_sources)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = rowsAdapter

        val db = PlaylistDatabase.getInstance(requireContext())
        channelRepo = ChannelSourceRepository(
            sourceDao = db.channelSourceDao(),
            channelDao = db.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )

        val epgDb = EpgDatabase.getInstance(requireContext())
        epgRepo = EpgRepository(dao = epgDb.epgDao(), client = OkHttpClientFactory.create())

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is ChannelWithEpg -> startActivity(PlayerActivity.intent(requireContext(), item.channel))
                is Channel -> startActivity(PlayerActivity.intent(requireContext(), item))
                is GuideAction -> openEpgGrid()
            }
        }

        observeChannels()
    }

    private fun observeChannels() {
        lifecycleScope.launch {
            channelRepo.observeChannels().collectLatest { channels ->
                if (channels.isEmpty()) {
                    showOnboarding()
                    return@collectLatest
                }

                // Enrich each channel with EPG now/next using its tvg-id
                val withEpg = channels.map { channel ->
                    val tvgId = channel.tvgId
                    val nowNext = if (tvgId != null) {
                        NowNextInfo(
                            now = epgRepo.getNowPlaying(tvgId),
                            next = epgRepo.getNextUp(tvgId),
                        )
                    } else null
                    ChannelWithEpg(channel, nowNext)
                }

                populateRows(withEpg)
            }
        }
    }

    private fun populateRows(channels: List<ChannelWithEpg>) {
        rowsAdapter.clear()

        // Channel rows grouped by groupTitle
        val grouped = channels.groupBy {
            it.channel.groupTitle ?: getString(R.string.title_sources)
        }
        grouped.entries.forEachIndexed { index, (group, groupChannels) ->
            val channelAdapter = ArrayObjectAdapter(ChannelPresenter())
            channelAdapter.addAll(0, groupChannels)
            rowsAdapter.add(ListRow(HeaderItem(index.toLong(), group), channelAdapter))
        }

        // Programme Guide row
        val guideAdapter = ArrayObjectAdapter(GuideActionPresenter())
        guideAdapter.add(GuideAction)
        rowsAdapter.add(
            ListRow(
                HeaderItem(grouped.size.toLong(), getString(R.string.title_epg_guide)),
                guideAdapter,
            )
        )
    }

    private fun showOnboarding() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, OnboardingFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openEpgGrid() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, EpgGridFragment())
            .addToBackStack(null)
            .commit()
    }
}
