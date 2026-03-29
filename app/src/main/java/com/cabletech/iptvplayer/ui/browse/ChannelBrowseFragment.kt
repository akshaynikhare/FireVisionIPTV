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
import com.cabletech.iptvplayer.player.ui.PlayerActivity
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.ui.onboarding.OnboardingFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main channel browsing screen.
 *
 * Uses Leanback [BrowseSupportFragment] to render channels grouped by
 * [Channel.groupTitle].  Selecting a channel launches [PlayerActivity].
 *
 * If no sources exist, the onboarding fragment is shown instead.
 */
class ChannelBrowseFragment : BrowseSupportFragment() {

    private lateinit var repository: ChannelSourceRepository
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = getString(R.string.title_sources)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = rowsAdapter

        val db = PlaylistDatabase.getInstance(requireContext())
        repository = ChannelSourceRepository(
            sourceDao = db.channelSourceDao(),
            channelDao = db.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Channel) {
                startActivity(PlayerActivity.intent(requireContext(), item))
            }
        }

        observeChannels()
    }

    private fun observeChannels() {
        lifecycleScope.launch {
            repository.observeChannels().collectLatest { channels ->
                if (channels.isEmpty()) {
                    showOnboarding()
                    return@collectLatest
                }
                populateRows(channels)
            }
        }
    }

    private fun populateRows(channels: List<Channel>) {
        rowsAdapter.clear()

        // Group by groupTitle; ungrouped channels go into "All Channels"
        val grouped = channels.groupBy { it.groupTitle ?: getString(R.string.title_sources) }
        grouped.entries.forEachIndexed { index, (group, groupChannels) ->
            val channelAdapter = ArrayObjectAdapter(ChannelPresenter())
            channelAdapter.addAll(0, groupChannels)
            val header = HeaderItem(index.toLong(), group)
            rowsAdapter.add(ListRow(header, channelAdapter))
        }
    }

    private fun showOnboarding() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, OnboardingFragment())
            .addToBackStack(null)
            .commit()
    }
}
