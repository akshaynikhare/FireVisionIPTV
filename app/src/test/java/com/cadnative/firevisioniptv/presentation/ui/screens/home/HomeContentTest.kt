package com.cadnative.firevisioniptv.presentation.ui.screens.home

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Home's banner state is the awkward bit: it has to be stable enough that a late
 * `featuredChannels` emission doesn't dispose the node holding focus, and live
 * enough that a favourite toggle or an EPG refresh actually shows. Making the
 * whole list sticky bought the first and broke the second, silently — the row
 * kept rendering the models captured when membership last changed.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, manifest = Config.NONE, qualifiers = "w960dp-h540dp-television-xhdpi")
class HomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Counts matching nodes instead of asserting a single one: the same channel
     * name legitimately renders in the hero, the Featured row and its category
     * row, so assertIsDisplayed()'s one-node requirement would fail on working
     * code.
     */
    private fun textCount(text: String, exact: Boolean = false) =
        composeRule.onAllNodesWithText(text, substring = !exact)
            .fetchSemanticsNodes().size

    private fun channel(id: String, name: String, now: String? = null) = ChannelUiModel(
        id = id,
        name = name,
        logoUrl = null,
        category = "News",
        isFavorite = false,
        nowProgramTitle = now
    )

    @Test
    fun `featured models refresh when the same channels re-emit with new details`() {
        val featured = mutableStateOf(listOf(channel("1", "Alpha"), channel("2", "Beta")))

        composeRule.setContent {
            FireVisionTheme {
                HomeContent(
                    channels = featured.value,
                    featuredChannels = featured.value,
                    recentlyWatched = emptyList(),
                    forYou = emptyList(),
                    popularCategories = emptyList(),
                    lastPlayedChannelId = null,
                    onChannelClick = {},
                    onNavigateToChannels = {},
                    onToggleFavorite = {},
                    onMultiviewClick = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue("fixture never rendered the original name", textCount("Alpha") > 0)

        // Same ids, same order — only the details changed, which is what a
        // favourite toggle or a channel-name sync looks like.
        featured.value = listOf(channel("1", "Alpha Renamed"), channel("2", "Beta"))
        composeRule.waitForIdle()

        assertTrue("the updated name never appeared", textCount("Alpha Renamed") > 0)
        assertTrue("the stale name is still on screen", textCount("Alpha", exact = true) == 0)
    }

    @Test
    fun `a late featured emission replaces the channels fallback`() {
        val channels = listOf(channel("9", "Fallback"))
        val featured = mutableStateOf(emptyList<ChannelUiModel>())

        composeRule.setContent {
            FireVisionTheme {
                HomeContent(
                    channels = channels,
                    featuredChannels = featured.value,
                    recentlyWatched = emptyList(),
                    forYou = emptyList(),
                    popularCategories = emptyList(),
                    lastPlayedChannelId = null,
                    onChannelClick = {},
                    onNavigateToChannels = {},
                    onToggleFavorite = {},
                    onMultiviewClick = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue("fallback never rendered", textCount("Fallback") > 0)

        featured.value = listOf(channel("1", "RealFeatured"))
        composeRule.waitForIdle()

        // Only that the real featured channel took over the banner. "Fallback" is
        // deliberately not asserted absent: it is still in `channels`, so its
        // category row keeps rendering it, which is correct.
        assertTrue("the real featured channel never appeared", textCount("RealFeatured") > 0)
    }

    @Test
    fun `an empty emission keeps the previous banner rather than blanking it`() {
        val featured = mutableStateOf(listOf(channel("1", "Persisted")))

        composeRule.setContent {
            FireVisionTheme {
                HomeContent(
                    channels = emptyList(),
                    featuredChannels = featured.value,
                    recentlyWatched = emptyList(),
                    forYou = emptyList(),
                    popularCategories = emptyList(),
                    lastPlayedChannelId = null,
                    onChannelClick = {},
                    onNavigateToChannels = {},
                    onToggleFavorite = {},
                    onMultiviewClick = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue("fixture never rendered", textCount("Persisted") > 0)

        // A transient empty Flow emission must not tear the hero down for a frame.
        featured.value = emptyList()
        composeRule.waitForIdle()

        assertTrue("the banner was blanked by an empty emission", textCount("Persisted") > 0)
    }
}
