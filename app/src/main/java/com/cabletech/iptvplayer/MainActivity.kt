package com.cabletech.iptvplayer

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.cabletech.iptvplayer.playlist.sync.PlaylistSyncWorker
import com.cabletech.iptvplayer.ui.browse.ChannelBrowseFragment

/**
 * Entry-point activity for the IPTV Player application.
 *
 * Hosts [ChannelBrowseFragment] as its root fragment and schedules the
 * periodic playlist background sync on first launch.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, ChannelBrowseFragment())
                .commit()
        }

        // Schedule background sync (no-op if already scheduled)
        PlaylistSyncWorker.schedule(this)
    }
}
