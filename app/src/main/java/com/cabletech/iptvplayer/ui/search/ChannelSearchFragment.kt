package com.cabletech.iptvplayer.ui.search

import android.os.Bundle
import android.view.View
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.player.ui.PlayerActivity
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.ui.browse.ChannelPresenter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Leanback [SearchSupportFragment] for channel search by name or group.
 *
 * Results are filtered in-memory from the local Room DB.
 */
class ChannelSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private lateinit var repository: ChannelSourceRepository
    private val resultsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var allChannels: List<Channel> = emptyList()
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = PlaylistDatabase.getInstance(requireContext())
        repository = ChannelSourceRepository(
            sourceDao = db.channelSourceDao(),
            channelDao = db.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )

        setSearchResultProvider(this)

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Channel) {
                startActivity(PlayerActivity.intent(requireContext(), item))
            }
        }

        lifecycleScope.launch {
            allChannels = repository.observeChannels().first()
        }
    }

    override fun getResultsAdapter(): ObjectAdapter = resultsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        search(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        search(query)
        return true
    }

    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val results = if (query.isBlank()) emptyList()
            else allChannels.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.groupTitle?.contains(query, ignoreCase = true) == true
            }
            resultsAdapter.clear()
            if (results.isNotEmpty()) {
                val adapter = ArrayObjectAdapter(ChannelPresenter())
                adapter.addAll(0, results)
                resultsAdapter.add(ListRow(HeaderItem(0, "Results (${results.size})"), adapter))
            }
        }
    }
}
