package com.cabletech.iptvplayer.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import kotlinx.coroutines.launch

/**
 * Onboarding screen shown when no M3U source has been added yet.
 *
 * The user enters a playlist URL and name, then taps "Add Source" to
 * trigger the first fetch.
 */
class OnboardingFragment : Fragment() {

    private lateinit var repository: ChannelSourceRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = PlaylistDatabase.getInstance(requireContext())
        repository = ChannelSourceRepository(
            sourceDao = db.channelSourceDao(),
            channelDao = db.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )

        val nameField = view.findViewById<EditText>(R.id.source_name)
        val urlField  = view.findViewById<EditText>(R.id.source_url)
        val addButton = view.findViewById<Button>(R.id.add_source_button)

        addButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val url  = urlField.text.toString().trim()
            if (name.isEmpty() || url.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in name and URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                runCatching {
                    val source = repository.addSource(name, url)
                    repository.refreshSource(source.id)
                }.onSuccess {
                    parentFragmentManager.popBackStack()
                }.onFailure { err ->
                    Toast.makeText(requireContext(), "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
