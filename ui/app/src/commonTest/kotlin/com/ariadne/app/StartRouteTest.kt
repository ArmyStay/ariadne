package com.ariadne.app

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The startup rule, stated three times and nowhere else.
 *
 * Every P0 finding of the third review came back to this table: a guide that said
 * one thing, a shell that did another, and a capture page reachable a third way.
 */
class StartRouteTest {

    @Test
    fun nothing_ever_saved_opens_onboarding() {
        assertEquals(Route.Onboarding, startRoute(hasAnyTrip = false, hasOpenTrip = false))
    }

    @Test
    fun an_open_trip_opens_home() {
        assertEquals(Route.Home, startRoute(hasAnyTrip = true, hasOpenTrip = true))
    }

    @Test
    fun trips_that_are_all_closed_open_the_register() {
        assertEquals(Route.Trips, startRoute(hasAnyTrip = true, hasOpenTrip = false))
    }

    @Test
    fun an_open_trip_wins_over_the_register_even_if_the_first_one_is_closed() {
        // `hasOpenTrip` is about any open trip, not the most recent one: a person
        // who closed last week's trip and is halfway through this one lands Home.
        assertEquals(Route.Home, startRoute(hasAnyTrip = true, hasOpenTrip = true))
    }
}
