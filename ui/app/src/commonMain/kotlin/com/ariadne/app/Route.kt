package com.ariadne.app

import com.ariadne.model.Capture
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Trip

/** Every surface in the app. There are six, and none of them is a spreadsheet. */
/**
 * The seven pages. One sealed type, one `when`, no navigation library: a trip is
 * short and the surfaces are few, so the routing table should be readable in full.
 */
public sealed interface Route {
    /** 1. The shelf of trips. */
    public data object Trips : Route

    /** 2. First run: what this is, in one page. */
    public data object Onboarding : Route

    /** 3. One trip as an object: dates, progress, currencies. */
    public data object Trip : Route

    /** 4. The shutter. */
    public data object Capture : Route

    /** 5. Nightfall: the card stack that finishes the day. */
    public data class Nightfall(val day: LocalDay) : Route

    /** 6. The record: days and their photographs. */
    public data class Record(val day: LocalDay?) : Route

    /** 7. The trip card, which is the thing you keep. */
    public data class TripCard(val tripId: String) : Route

    /** 8. Search by what you remember. */
    public data object Search : Route

    /** 9. Settings, quiet and short. */
    public data object Settings : Route

    // ── the four destinations in the bottom bar ──────────────────────────────
    // These are tabs, not pages: the routed surfaces above still stack on top of
    // them. Capture is a fifth destination but has no tab — it is the shutter.

    /** The shelf, with today's figure on it. */
    public data object Home : Route

    /** The trip as a run of days. */
    public data object Timeline : Route

    /** What it came to. */
    public data object Stats : Route

    /** Preferences. */
    public data object Prefs : Route
}

/** Everything a screen can see. One immutable snapshot, replaced wholesale. */
public data class AppUiState(
    public val route: Route = Route.Onboarding,
    public val trip: Trip? = null,
    public val today: LocalDay? = null,
    public val dayCards: List<DayCard> = emptyList(),
    public val dayCaptures: List<Capture> = emptyList(),
    public val stack: List<Capture> = emptyList(),
    public val stackIndex: Int = 0,
    public val amountInput: String = "",
    public val dayTotalMinor: Long = 0,
    public val dayCurrency: String = "INR",
    public val perCurrency: Map<String, Long> = emptyMap(),
    public val searchQuery: String = "",
    public val searchResults: List<Capture> = emptyList(),
    public val busy: Boolean = false,
) {
    public val current: Capture? get() = stack.getOrNull(stackIndex)
    public val remaining: Int get() = (stack.size - stackIndex).coerceAtLeast(0)
}
