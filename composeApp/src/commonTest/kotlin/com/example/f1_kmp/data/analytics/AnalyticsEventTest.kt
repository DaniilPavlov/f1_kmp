package com.example.f1_kmp.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsEventTest {

    @Test
    fun screenView_paramsOptionalClass() {
        val withClass = AnalyticsEvent.ScreenView("home", "HomeScreen")
        assertEquals("screen_view", withClass.name)
        assertEquals("home", withClass.params["screen_name"])
        assertEquals("HomeScreen", withClass.params["screen_class"])

        val bare = AnalyticsEvent.ScreenView("results")
        assertFalse(bare.params.containsKey("screen_class"))
    }

    @Test
    fun comparisonEvents_includeOptionalSeason() {
        val h2h = AnalyticsEvent.H2hCompared("a", "b", "season", "2026")
        assertEquals("h2h_compared", h2h.name)
        assertEquals("2026", h2h.params["season"])

        val bare = AnalyticsEvent.H2hCompared("a", "b", "career")
        assertFalse(bare.params.containsKey("season"))

        val ctors = AnalyticsEvent.H2hConstructorsCompared("x", "y", "season", "2024")
        assertEquals("h2h_constructors_compared", ctors.name)
        assertEquals("2024", ctors.params["season"])
    }

    @Test
    fun remainingEvents_haveStableNamesAndParams() {
        assertEquals("tab_switched", AnalyticsEvent.TabSwitched("home").name)
        assertEquals(
            mapOf("race_name" to "Monaco", "season" to "2026", "round" to "8"),
            AnalyticsEvent.RaceOpened("Monaco", "2026", "8").params,
        )
        assertEquals("driver_opened", AnalyticsEvent.DriverOpened("id", "Name").name)
        assertEquals("constructor_opened", AnalyticsEvent.ConstructorOpened("id", "Name").name)
        assertEquals("circuit_opened", AnalyticsEvent.CircuitOpened("id", "Name").name)
        assertEquals("news_opened", AnalyticsEvent.NewsOpened("headline").name)
        assertEquals("hall_of_fame_opened", AnalyticsEvent.HallOfFameOpened.name)
        assertTrue(AnalyticsEvent.HallOfFameOpened.params.isEmpty())
        assertEquals("season_rewind_opened", AnalyticsEvent.SeasonRewindOpened.name)
        assertEquals("share_tapped", AnalyticsEvent.ShareTapped("weekend").name)
        assertEquals("theme_changed", AnalyticsEvent.ThemeChanged("dark").name)
        assertEquals("locale_changed", AnalyticsEvent.LocaleChanged("ru").name)
        assertEquals(true, AnalyticsEvent.RaceReminderToggled(true).params["enabled"])
        assertEquals("query", AnalyticsEvent.RaceSearched("query").params["query"])
    }
}
