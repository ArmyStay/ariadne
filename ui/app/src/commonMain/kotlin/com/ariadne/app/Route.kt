package com.ariadne.app

import com.ariadne.model.Capture
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Category
import com.ariadne.model.CategoryTotal
import com.ariadne.model.DayCard
import com.ariadne.model.DaySummary
import com.ariadne.model.LocalDay
import com.ariadne.model.Money
import com.ariadne.model.Reminder
import com.ariadne.model.Settings
import com.ariadne.model.ShareCodes
import com.ariadne.model.Traveller
import com.ariadne.model.TravellerShare
import com.ariadne.model.Trip
import com.ariadne.model.entryStatus

/** Every route in the app, including the four primary dock destinations. */
/**
 * The four places in the bar. Everything else is stacked on top of one of them.
 *
 * This exists so navigation can be one rule instead of a per-call-site decision:
 * moving *between* places starts a fresh stack (Back from Days should not walk you
 * back through the record you read on the way), while moving *into* a page pushes
 * the place onto the stack.
 */
public fun Route.isDestination(): Boolean =
    this == Route.Home || this == Route.Days || this == Route.Stats || this == Route.More

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
    /** The archive of past and current trips. */
    public data object Trips : Route

    /** The fourth dock destination: a quiet index for secondary actions. */
    public data object More : Route

    /** Photo-free expense entry. */
    public data object ManualEntry : Route

    /** The local data archive flow. */
    public data object DataExport : Route

    /** Informational legal page, explicitly marked as a draft until reviewed. */
    public data object PrivacyPolicy : Route

    public data object Terms : Route

    /** 2. First run: what this is, in one page. */
    public data object Onboarding : Route

    /**
     * 01c. Joining a trip somebody else planned.
     *
     * The other half of the first instalment: one person types the plan once and reads
     * six groups of symbols out, the other types them back and has the same trip — same
     * dates, same currency, same budget — without an account, an invitation or a server.
     */
    public data object JoinTrip : Route

    /**
     * 01b. A new trip, typed in.
     *
     * This is the front door: on a first run there is no trip, so there is
     * nothing to show until one exists. Name, place, dates, currency, budget.
     */
    public data object NewTrip : Route

    /** Legacy route retained as a Timeline alias. */
    public data object Trip : Route

    /** 4. The shutter. */
    public data object Capture : Route

    /**
     * 4b. The frame just taken, held up.
     *
     * Two ways out and no way to lose it: keep it as it is and finish it later at
     * a later amount, or fill it in now — amount, category, what it was for.
     */
    public data class Frame(val captureId: String) : Route

    /** 5. Closing the day: the cards it still owes, one at a time. */
    public data class CloseDay(val day: LocalDay) : Route

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
     * never required: closing the day remains two taps end to end, and nothing here is
     * mandatory, including the category and the note.
     */
    public data class Detail(val captureId: String) : Route

    /** 8. Search by what you remember. */
    public data object Search : Route

    /**
     * 9. Two phones, one ledger.
     *
     * One page for both directions of the hand-off: the plan code to read out, the
     * day's figures to hand back, and the line that brings somebody else's figures in.
     * Nothing here is a link and nothing here syncs — the page says so, twice.
     */
    public data object Handoff : Route

    /**
     * Retired name for [Preferences], kept so a route persisted by an older build
     * still resolves. Nothing in the UI navigates here; the shell maps it.
     */
    public data object Settings : Route

    // ── the four destinations in the bottom bar ──────────────────────────────
    // These are tabs, not pages: the routed surfaces above still stack on top of
    // them. Capture is a fifth destination but has no tab — it is the shutter.

    /** Today's record, recent expenses, and the active trip. */
    public data object Home : Route

    /** The active trip overview and its days. */
    public data object Days : Route

    /** What it came to. */
    public data object Stats : Route

    /** Settings and local profile details, reached from More. */
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

/**
 * A line at the foot of the page that says what just happened.
 *
 * It exists for the one thing a ledger of photographs cannot do silently: taking a
 * correction back. An edit overwrites a figure the person may have read a hundred
 * times; without a way to undo it, the only safe edit is no edit. [actionLabel] is
 * null for notices that are statements rather than offers.
 *
 * [id] changes on every notice so the UI can key on it and a screen reader
 * announces a new line rather than a changed one.
 */
public data class Notice(
    public val text: String,
    public val actionLabel: String? = null,
    public val id: Long = 0,
    /**
     * What the action does.
     *
     * There used to be exactly one thing a notice could offer — take the last edit
     * back — and the bar said so by calling `undoLastEdit()` directly. The evening
     * reminder needs a notice that offers the day instead, so the offer is named.
     */
    public val kind: NoticeKind = NoticeKind.UNDO,
)

public enum class NoticeKind { UNDO, CLOSE_DAY }

public data class AppUiState(
    public val route: Route = Route.Onboarding,
    public val isStartingUp: Boolean = true,
    /**
     * Where Back goes, oldest first.
     *
     * The app used to have no stack at all: every route was a single value, Back
     * was whatever the platform did with it (usually nothing), and a page stacked
     * on a page had to remember for itself where it came from — which is why the
     * detail page carried a `detailOrigin` enum and Back from search could land on
     * a record nobody had opened. One list, pushed by [AppModel.go] and popped by
     * [AppModel.back], is the whole mechanism.
     */
    public val history: List<Route> = emptyList(),
    /**
     * Which stock the book is printed on: light paper, night paper, or whatever
     * the device is currently asking for. Loaded from the store at start and
     * written back when it is changed; the palette itself lives in `:core:design`
     * and has done since the first commit — this is only what finally selects it.
     */
    public val theme: Settings.Theme = Settings.Theme.LIGHT,
    public val displayName: String = "",
    public val trip: Trip? = null,
    public val today: LocalDay? = null,
    public val dayCards: List<DayCard> = emptyList(),
    public val dayCaptures: List<Capture> = emptyList(),
    public val stack: List<Capture> = emptyList(),
    public val stackIndex: Int = 0,
    public val amountInput: String = "",
    public val dayTotalMinor: Long = 0,
    public val dayCurrency: String = "INR",
    public val dayPerCurrency: Map<String, Long> = emptyMap(),
    /** Confirmed money across the whole trip, kept in each receipt's currency. */
    public val tripPerCurrency: Map<String, Long> = emptyMap(),
    /** Confirmed trip total after conversion to the trip's base currency. */
    public val tripTotalMinor: Long = 0,
    /**
     * Money on this day that has no rate, in its own currency.
     *
     * It is not in [dayTotalMinor] — that is the whole point. The day total used to
     * add unconverted minor units to base ones, which is how THB 2,555 and INR 50
     * became "₹2,605". These figures are named on screen instead.
     */
    public val dayUnconverted: Map<String, Long> = emptyMap(),
    /** The day each foreign rate was true, for "≈ at 20 Nov". */
    public val dayRateDates: Map<String, String> = emptyMap(),
    /** The same, for the whole trip. */
    public val tripUnconverted: Map<String, Long> = emptyMap(),
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

    /**
     * True once the person has tapped "delete this photograph" once.
     *
     * The same rule as a trip, for the same reason: there is no copy of this record
     * anywhere else, so one tap is not enough consent. Kept separate from
     * [deleteArmed] because arming one must not arm the other — a page with both
     * offers would otherwise delete a trip on the second tap of a photograph.
     */
    public val entryDeleteArmed: Boolean = false,

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
    /** Platform-specific explanation of where trip and photograph data is kept. */
    public val storageDisclosure: String = "This preview keeps data in memory for this session only.",
    /** How rates reach this target, including whether automatic fetching is available. */
    public val rateDisclosure: String = "Rates are entered manually and kept in the current preview session.",

    /**
     * The last error the app caught, as one line, for the diagnostic on
     * Preferences. Null almost always — it is here so that a failure is written
     * down somewhere a person can find rather than only in a console.
     */
    public val failureNote: String? = null,

    /** The last thing that happened, if it is still worth worth saying. */
    public val notice: Notice? = null,

    /** Capture or import failure shown on the shutter while it remains open. */
    public val captureError: String? = null,

    /**
     * The camera, open over whatever page is showing.
     *
     * It used to be a route: tapping the shutter navigated to a viewfinder page, and
     * the photograph arrived on a second page after that. Three screens to keep a
     * receipt, two of which were the same nothing.
     *
     * Now the shutter is a *mode over the page you are on*. Nothing navigates: the
     * camera rises over Today, the frame becomes the sheet over the same Today, and
     * putting the amount in drops you back exactly where you were — same tab, same
     * scroll, same day.
     */
    public val shutterOpen: Boolean = false,

    /** The photograph whose sheet is open over the page, if one is. */
    public val sheetCaptureId: String? = null,

    // ── two phones, one ledger ───────────────────────────────────────────────

    /** Somebody else's figures for this trip, in the order they arrived. */
    public val travellers: List<Traveller> = emptyList(),

    /** Their figures across the whole trip, converted to the trip's currency. */
    public val travellerShares: List<TravellerShare> = emptyList(),

    /** Their figures for the day on screen, converted. Empty until a code arrives. */
    public val travellerSharesForDay: List<TravellerShare> = emptyList(),

    /** The code being typed on the join page, exactly as typed. */
    public val joinCode: String = "",
    /** What to call the trip on this phone. The code does not carry a name. */
    public val joinName: String = "",
    /** Why the typed code cannot be read, in one line, or null. */
    public val joinError: String? = null,
    /** What the typed code says, once it reads. */
    public val joinPlan: ShareCodes.Plan? = null,

    /** The ledger line being pasted on the hand-off page. */
    public val ledgerPaste: String = "",
    public val ledgerError: String? = null,
    /** What the pasted line says, once it reads. */
    public val ledgerPreview: ShareCodes.Ledger? = null,

    /** True for a moment after a code was copied, so the label can say so. */
    public val codeCopied: Boolean = false,

    // ── the evening reminder ─────────────────────────────────────────────────

    /** On or off, and the clock it is set for. */
    public val reminder: Reminder = Reminder(),

    /** The sheet is open: either the one-time offer or the settings row. */
    public val reminderSheetOpen: Boolean = false,

    /** True when this is the one-time offer after the first trip, not a settings visit. */
    public val reminderIsOffer: Boolean = false,

    /**
     * What the platform managed to do, in one line, or null when it did exactly what
     * the person asked for. Never a silent failure dressed as success.
     */
    public val reminderNote: String? = null,

    /**
     * The day the open shutter will file into.
     *
     * Normally today, but a person who opens the record on Tuesday and presses the
     * shutter is filing Tuesday's receipt, and the app should take them at their
     * word. Set by [AppModel.openCapture] and [AppModel.chooseShutterDay], and it can
     * never be a day that has not happened: a future day is not one of the choices,
     * so the value here is always a day the record may hold.
     */
    public val shutterDay: LocalDay? = null,
) {
    public val current: Capture? get() = stack.getOrNull(stackIndex)
    public val remaining: Int get() = (stack.size - stackIndex).coerceAtLeast(0)

    /**
     * Every capture in the trip that still needs something: an amount, or a
     * confirmation that it has none. One list, computed from the entry state and
     * nothing else, so no two pages can disagree about whether the trip is done.
     *
     * The Home page's "all settled" pill used to read `stack.isEmpty()` — the
     * pending stack of whichever day happened to be loaded last. Visit another day
     * and Home claimed the trip was settled while the timeline still showed an open
     * card. This is that bug, fixed at the source rather than in the label.
     */
    public val tripPending: List<Capture>
        get() = dayCapturesAll.values.flatten().filter { it.entryStatus.isPending }

    /** True when nothing anywhere in the trip is waiting on the person. */
    public val tripSettled: Boolean get() = tripPending.isEmpty()

    /** What one day of the trip still owes, read the same way every page reads it. */
    public fun daySummary(iso: String): DaySummary = DaySummary.of(dayCapturesAll[iso].orEmpty())

    /** True when Back has somewhere to go inside the app. */
    /**
     * Back is available when there is a page to return to **or** something is open
     * over this one: an overlay is what Back should close first, before it moves
     * between pages.
     */
    public val canGoBack: Boolean
        get() = shutterOpen || sheetCaptureId != null || reminderSheetOpen || history.isNotEmpty()
}

/**
 * What the sheet's typed figure will become, worked out before it is saved.
 *
 * The sheet shows two figures side by side — what was paid, and what that is worth
 * in the trip's currency — with the rate that joins them printed underneath. That
 * pair used to exist only *after* saving, on the detail page, which meant the person
 * typed a number in Bangkok and found out what it had cost them in rupees later.
 *
 * This is a derivation, not state: computed from the typed text, the capture's
 * currency, the trip's base currency and the rate table for today. [base] is null
 * when no rate is on file, which is a real answer — the sheet then says the amount
 * will be counted in its own currency rather than inventing a conversion.
 */
public data class SheetPreview(
    public val typed: String,
    public val from: CurrencyCode,
    /** The figure in the trip's base currency, or null when there is no rate. */
    public val base: Money?,
    /** "1 SGD ≈ 74.6298 INR", printed under the pair. */
    public val rateLabel: String?,
    /** The amount already is in the trip's currency: there is nothing to convert. */
    public val sameCurrency: Boolean,
)
