package com.cabletech.iptvplayer

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Entry-point activity for the IPTV Player application.
 *
 * Hosts the main Leanback [BrowseFragment] (channel/source browser) as its
 * primary fragment.  All TV navigation is handled within the fragment stack;
 * the activity itself is intentionally thin.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
