package com.ariadne.app

import com.ariadne.model.Capture
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.DayCard
import com.ariadne.model.LocalDay
import com.ariadne.model.Settings
import com.ariadne.model.Trip

/** Every surface in the app. There are six, and none of them is a spreadsheet. */
/**
 * Where a launch lands, decided once, from two facts.
 *
 * Every startup path asks this function and no other — the cold launch, a
 * restored process, and a deep link whose page cannot exist yet. Written as a
 * pure function rather than as branches inside `start()` so a test can state the
 * rule in one line:
 *
 *  - the device has never held a trip -> [Route.Onboarding];
 *  - a trip is open -> [Route.Home];
 *  - trips exist but every one is closed -> the register, [Route.Trips].
 *
 * The third case is not a fallback: with no open trip there is no shutter to
 * open, no record to read and no day to close, and inventing a trip would invent
 * a day boundary. The register is where closed trips are still readable.
 */
public fun startRoute(hasAnyTrip: Boolean, hasOpenTrip: Boolean): Route = when {
    !hasAnyTrip -> Route.Onboarding
    hasOpenTrip -> Route.Home
    else -> Route.Trips
}

/**
 * The seven pages. One sealed type, one `when`, no navigation library: a trip is
 * short and the surfaces are few, so the routing table should be readable in full.
 */
public sealed interface Route {
    /** 1. The shelf of trips. */
    public data object Trips : Route

    /** 2. First run: what this is, in one page. */
    public data object Onboarding : Route

    /**
     * 01b. A new trip, typed in.
     *
     * This is the front door: on a first run there is no trip, so there is
     * nothing to show until one exists. Name, place, dates, currency, budget.
     */
    public data object NewTrip : Route

    /** 3. One trip as an object: dates, progress, currencies. */
    public data object Trip : Route

    /** 4. The shutter. */
    public data object Capture : Route

    /**
     * 4b. The frame just taken, held up.
     *
     * Two ways out and no way to lose it: keep it as it is and finish it later at
     * Nightfall, or fill it in now — amount, category, what it was for.
     */
    public data class Frame(val captureId: String) : Route

    /** 5. Nightfall: the card stack that finishes the day. */
    public data class Nightfall(val day: LocalDay) : Route

    /** 6. The record: days and their photographs. */
    public data class Record(val day: LocalDay?) : Route

    /** 7. The trip card, which is the thing you keep. */
    public data class TripCard(val tripId: String) : Route

    /**
     * One capture, open in the hand: the photograph large, the figure editable,
     * a category to claim, a line to remember it by.
     *
     * This is the page the ritual hands you to when a card is worth more than a
     * yes or a no — a receipt from a shop you will want to name again. It is
     * never required: Nightfall remains two taps end to end, and nothing here is
     * mandatory, including the category and the note.
     */
    public data class Detail(val captureId: String) : Route

    /** 8. Search by what you remember. */
    public data object Search : Route

    /**
     * Retired name for [Preferences], kept so a route persisted by an older build
     * still resolves. Nothing in the UI navigates here; the shell maps it.
     */
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

    /**
     * Preferences: the stock, the currencies, the exchange.
     *
     * "Preferences" is the one canonical name for this surface. The Home action
     * that opens it is printed `PREFS`, because that is the word a printed index
     * would use at that size; the route, the page title and the code all say
     * Preferences.
     */
    public data object Preferences : Route
}

/** Everything a screen can see. One immutable snapshot, replaced wholesale. */
/**
 * A trip being typed, before it exists.
 *
 * Dates are held as text because that is what a keyboard produces and what a
 * human reads back; they are parsed on the way into the model, and the page shows
 * the parse result so a typo is visible before the trip is created, not after.
 * The budget is held as text for the same reason, and converted to minor units in
 * exactly one place.
 */
public data class TripDraft(
    public val name: String = "",
    public val place: String = "",
    public val startIso: String = "",
    public val endIso: String = "",
    public val currency: String = "INR",
    public val budget: String = "",
    public val error: String? = null,
) {
    public val isEmpty: Boolean get() = name.isBlank() && startIso.isBlank() && endIso.isBlank()
}

/**
 * Where the detail page was opened from.
 *
 * A photograph can be reached from the day's record or from a search result, and
 * Back has to return to the place the person was, not to a fixed page. Search
 * keeps its query and its results either way, so going back lands on the same
 * list the person was reading.
 */
public enum class DetailOrigin { Record, Search }

public data class AppUiState(
    public val route: Route = Route.Onboarding,
    /**
     * Which stock the book is printed on: light paper, night paper, or whatever
     * the device is currently asking for. Loaded from the store at start and
     * written back when it is changed; the palette itself lives in `:core:design`
     * and has done since the first commit — this is only what finally selects it.
     */
    public val theme: Settings.Theme = Settings.Theme.SYSTEM,
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
    /** The capture open in the detail page, when one is. */
    public val detail: Capture? = null,
    /** Where the detail page was opened from, so Back can return there. */
    public val detailOrigin: DetailOrigin = DetailOrigin.Record,
    public val detailAmount: String = "",
    public val detailMerchant: String = "",
    public val detailNote: String = "",
    public val detailCategory: Category? = null,
    /** The trip's spending by what it was, in base-currency minor units. */
    public val categoryTotals: List<CategoryTotal> = emptyList(),
    /**
     * Every day of the trip with what was kept on it, and what it came to in base
     * currency. Loaded in one pass so the timeline can show the shape of the whole
     * trip rather than only the day currently open.
     */
    public val dayCapturesAll: Map<String, List<Capture>> = emptyMap(),
    public val dayFigures: Map<String, Long> = emptyMap(),
    public val searchQuery: String = "",
    public val searchResults: List<Capture> = emptyList(),
    public val busy: Boolean = false,
    /** The trip being composed on the new-trip page. Empty until they start one. */
    public val draft: TripDraft = TripDraft(),
    /**
     * True once the person has tapped "delete" once.
     *
     * Deletion is two taps on purpose: there is no copy of this record anywhere
     * else, so one tap is not enough consent and an undo would be a lie.
     */
    public val deleteArmed: Boolean = false,

    /** True for a moment after the card was copied, so the label can say so. */
    public val cardCopied: Boolean = false,

    /**
     * Every trip this device holds, open and closed, newest first.
     *
     * The register. One trip is *active* at a time - the one captures land in -
     * and that is deliberately a single trip rather than a filter: a photograph
     * taken now belongs to exactly one journey, and asking which on every shutter
     * would be a question nobody wants at a market stall.
     */
    public val trips: List<Trip> = emptyList(),

    /** Which trip a new capture lands in. Null only before the first trip exists. */
    public val activeTripId: String? = null,

    /**
     * Whether the hanging rail is shown on a wide page.
     *
     * It is the reader's furniture, not the page's: some people want the dates
     * and figures always in the margin, some want the whole width for the record.
     * Remembered for the session; it is a view preference, not a document.
     */
    public val railDocked: Boolean = true,

    /** A refused exchange rate, in one line, for the field that refused it. */
    public val rateError: String? = null,

    /**
     * The last error the app caught, as one line, for the diagnostic on
     * Preferences. Null almost always — it is here so that a failure is written
     * down somewhere a person can find rather than only in a console.
     */
    public val failureNote: String? = null,
) {
    public val current: Capture? get() = stack.getOrNull(stackIndex)
    public val remaining: Int get() = (stack.size - stackIndex).coerceAtLeast(0)
}
