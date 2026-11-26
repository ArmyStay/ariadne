package com.ariadne.preview

import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.Route
import com.ariadne.app.TripDraft
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
import com.ariadne.model.CurrencyCatalog
import com.ariadne.model.Settings
import com.ariadne.extract.TextRecognizer
import com.ariadne.store.InMemoryStore

/**
 * The flow, walked as a person walks it.
 *
 * Rendering a page proves it compiles and that nothing throws during layout. It
 * does not prove that the shutter puts a frame in the record, that the two ways
 * out of the frame page lead where they say, or that a typed budget survives into
 * the store. This does, and it fails loudly, which is the point.
 *
 *   ./tools/flow-check.sh
 */
private var failures = 0
private var checks = 0

private fun check(name: String, condition: Boolean) {
    checks++
    if (condition) {
        println("  ok   $name")
    } else {
        failures++
        println("  FAIL $name")
    }
}

fun main() {
    println("flow")

    val services = FlowServices()
    val graph = AppGraph(services, SystemClock())
    val model = graph.model

    // ---- first run: no trip, so the title page states the thesis ----------
    model.start()
    settle()
    check("first run opens the title page", model.state.value.route == Route.Onboarding)
    check("the form is pre-filled with today", model.state.value.draft.startIso.isNotEmpty())
    check("no trip exists yet", model.state.value.trip == null)

    model.startNewTrip()
    settle()
    check("begin empty opens the new-trip form", model.state.value.route == Route.NewTrip)

    // ---- a rejected draft says which field is wrong ------------------------
    model.editDraft(name = "")
    model.commitDraft()
    settle()
    check("blank name is refused", model.state.value.draft.error != null)
    check("still on the form after a refusal", model.state.value.route == Route.NewTrip)
    check("no trip was created by a refused draft", graph.store.tripsSnapshot().isEmpty())

    // ---- a bad date is refused, a bad range is refused ---------------------
    model.editDraft(name = "Bangkok", startIso = "24-11-2026")
    model.commitDraft()
    settle()
    check("an unparseable date is refused", model.state.value.draft.error != null)

    model.editDraft(startIso = "2026-11-30", endIso = "2026-11-24")
    model.commitDraft()
    settle()
    check("a trip that ends before it starts is refused", model.state.value.draft.error != null)

    // ---- a real trip, with a place and a budget ---------------------------
    model.editDraft(startIso = "2026-11-24", endIso = "2026-11-30", place = "Bangkok", budget = "25,000.50")
    model.commitDraft()
    settle()

    val trip = model.state.value.trip
    check("the trip was created", trip != null)
    check("the trip kept its name", trip?.name == "Bangkok")
    check("the trip kept its place", trip?.place == "Bangkok")
    check("the budget became minor units", trip?.budgetMinor == 2_500_050L)
    check("a created trip lands on Home", model.state.value.route == Route.Home)
    check("the draft was cleared", model.state.value.draft.name.isEmpty())

    // ---- the budget is a fence, and remaining is arithmetic ---------------
    check("remaining against the fence", trip?.remainingAgainstBudget(1_000_000L) == 1_500_050L)
    check("over budget reads negative", trip?.remainingAgainstBudget(3_000_000L) == -499_950L)
    check("no budget means no remaining", trip?.copy(budgetMinor = null)?.remainingAgainstBudget(1L) == null)

    // ---- the shutter opens over the page, and the frame becomes the sheet -
    //
    // This is the flow the person actually feels. The shutter used to be a route:
    // tapping it left the page, and the photograph arrived on a second page. Now
    // nothing navigates — the camera covers the page, the frame becomes the sheet
    // over that same page, and putting it away returns exactly where it was.
    model.go(Route.Home)
    settle()
    val shutterWasOver = model.state.value.route
    model.openCapture()
    settle()
    check("the shutter opens without navigating", model.state.value.route == shutterWasOver)
    check("and the camera is open", model.state.value.shutterOpen)
    check("Back has something to close", model.state.value.canGoBack)

    model.back()
    settle()
    check(
        "Back with the camera open closes the camera, not the page",
        model.state.value.route == shutterWasOver && !model.state.value.shutterOpen,
    )

    model.openCapture()
    settle()
    model.capturePhoto(byteArrayOf(1, 2, 3, 4))
    settle()

    val captureId = model.state.value.sheetCaptureId
    check("the frame becomes the sheet over the same page", captureId != null)
    check("the camera closes when the frame is kept", !model.state.value.shutterOpen)
    check("and the page underneath never changed", model.state.value.route == shutterWasOver)
    check("the frame is in the record", graph.store.findCapture(captureId!!) != null)
    check("the frame waits for the day to be closed", model.state.value.stack.isNotEmpty())

    // ---- photo bytes actually reached storage ----------------------------
    val stored = graph.store.findCapture(captureId)
    check("the photo was written", services.files.writtenPaths.contains(stored?.photoPath))
    check(
        "the file is readable back",
        kotlinx.coroutines.runBlocking { services.files.read(stored?.photoPath ?: "")?.size } == 4,
    )

    // ---- "add the amount later" loses nothing -----------------------------
    check(
        "the sheet opens with the amount field live",
        model.state.value.sheetCaptureId == captureId &&
            model.state.value.detail?.id == captureId &&
            model.state.value.detailAmount.isEmpty(),
    )
    // ---- the sheet says what the figure will become -----------------------
    //
    // The pair the sheet prints — what was paid, and what it is worth in the trip's
    // currency — is a derivation, so it can be checked without a screen: nothing
    // typed means nothing to convert, and a typed figure names its own currency.
    check("with nothing typed there is nothing to convert", model.sheetPreview() == null)
    "18.41".forEach { c -> model.editDetailAmount(c) }
    val preview = model.sheetPreview()
    val sheetCurrency = model.state.value.detail?.currency ?: model.state.value.trip?.baseCurrency
    check("the sheet can say what the figure will become", preview?.typed == "18.41")
    check("and names the currency the figure is in", preview?.from == sheetCurrency)
    check(
        "a pair in the trip's own currency needs no rate",
        preview?.sameCurrency != true || preview?.base != null,
    )
    repeat("18.41".length) { model.clearDetailAmountDigit() }
    check("clearing the field clears the pair", model.sheetPreview() == null)

    model.keepFrameForLater()
    settle()
    check("adding the amount later closes the sheet", model.state.value.sheetCaptureId == null)
    check("and leaves the person where they were", model.state.value.route == shutterWasOver)
    check("keeping it does not delete the frame", graph.store.findCapture(captureId) != null)

    // ---- the stack: one step in, one step out, and a refusal at a root ---
    //
    // Back is now the app's own stack (`AppModel.history`) rather than a guess on
    // each page. These four checks are the whole contract: a page goes back to the
    // place that opened it, a place goes back to the place before it, and a place
    // with nothing behind it says so instead of swallowing the gesture.
    model.go(Route.Stats)
    settle()
    check("a tab switch lands on that tab", model.state.value.route == Route.Stats)
    model.openRecord(null)
    settle()
    check("a day opens over the tab that asked for it", model.state.value.route is Route.Record)
    check("and the stack now has somewhere to go back to", model.state.value.canGoBack)
    model.back()
    settle()
    check("back from a day returns to its tab", model.state.value.route == Route.Stats)
    model.back()
    settle()
    check("back from a tab returns to the tab before it", model.state.value.route == Route.Home)
    check("and there it stops", !model.back())
    settle()
    check("a refused back does not move the app", model.state.value.route == Route.Home)

    // ---- "fill it in now" opens the capture for editing ------------------
    model.openFrame(captureId)
    settle()
    check("filling it in opens the detail page", model.state.value.route is Route.Detail)
    check("the detail page is showing that frame", model.state.value.detail?.id == captureId)

    // ---- editing an amount is rounded once, in the model -----------------
    model.editDetailMerchant("Shrine")
    "40.99".forEach { c -> model.editDetailAmount(c) }
    model.saveDetail()
    settle()
    val edited = graph.store.findCapture(captureId)
    check("an amount typed in reaches the record", edited?.amountMinor == 4_099L)
    check("a merchant typed in reaches the record", edited?.merchant == "Shrine")

    // ---- an edit can be taken back ---------------------------------------
    //
    // Undo has to restore the row as it was, not merely blank a field: the amount,
    // the merchant, the queue state and the pinned rate all belonged to the
    // photograph before the edit, and all of them belong to it again after.
    check("saving offers its own undo", model.state.value.notice?.actionLabel == "Undo")
    model.undoLastEdit()
    settle()
    val undone = graph.store.findCapture(captureId)
    check("undo restores the figure the edit replaced", undone?.amountMinor == null)
    check("undo restores the merchant", undone?.merchant == null)
    check("undo puts the queue state back", undone?.state == com.ariadne.model.CaptureState.QUEUED)
    check("and the offer goes with the undo", model.state.value.notice == null)

    // ---- the demo path still works in one call ---------------------------
    model.startTripDemo()
    settle()
    check("the demo roll creates a trip", model.state.value.trip?.name == "Bangkok & Home")
    check("the demo roll fills the stack", model.state.value.stack.size >= 5)
    check("the demo trip carries a budget from the form", model.state.value.trip?.budgetMinor == 2_500_000L)

    // ---- the controls that used to be decoration ------------------------
    val live = model.state.value.trip!!
    model.editTripCurrency("THB")
    settleCheck()
    check("changing the base currency reaches the store", graph.store.tripSnapshot(live.id)?.baseCurrency?.code == "THB")
    model.editTripHomeCurrency("USD")
    settleCheck()
    check("changing the home currency reaches the store", graph.store.tripSnapshot(live.id)?.homeCurrency?.code == "USD")
    model.editTripBudget("9000")
    settleCheck()
    check("editing the fence reaches the store", graph.store.tripSnapshot(live.id)?.budgetMinor == 900_000L)

    // ---- the stock the book is printed on ---------------------------------
    // Night paper has existed in the palette since the first commit and had no
    // switch; these two checks are the switch, from the outside.
    model.setTheme(Settings.Theme.DARK)
    settle()
    check("night paper is in state", model.state.value.theme == Settings.Theme.DARK)
    check("night paper is written to the settings", graph.store.settingsSnapshot().theme == Settings.Theme.DARK)
    model.setTheme(Settings.Theme.LIGHT)
    settle()
    check("light paper is written back", graph.store.settingsSnapshot().theme == Settings.Theme.LIGHT)
    check(
        "the catalogue knows the currency the trip is in",
        CurrencyCatalog.search("thb").firstOrNull()?.code == "THB",
    )

    // Closing into a card must not destroy anything.
    val beforeClose = graph.store.tripSnapshot(live.id)?.closedAt
    model.closeTripIntoCard()
    settleCheck()
    check("closing the trip opens its card", model.state.value.route is Route.TripCard)
    check("closing marks the trip closed", graph.store.tripSnapshot(live.id)?.closedAt != null)
    check("closing was not already done", beforeClose == null)
    check("closing keeps the captures", graph.store.capturesSnapshot(live.id).size >= 5)
    check("closing clears the active trip", model.state.value.activeTripId == null)

    // A closed trip is a finished record: the shutter must not file into it.
    val capturesAfterClose = graph.store.capturesSnapshot(live.id).size
    model.capturePhoto(byteArrayOf(9, 9, 9))
    settle()
    check("a closed trip takes no more photographs", model.state.value.route is Route.TripCard)
    check("nothing was added to a closed trip", graph.store.capturesSnapshot(live.id).size == capturesAfterClose)

    // Not one door, all of them. `openCapture` is what the bar's shutter, the
    // Record page, the frame's "take another" and the keyboard shortcut all call,
    // so the guard cannot be bypassed by taking a different route into the camera.
    model.openCapture()
    settle()
    check("the shutter entry point refuses a closed trip", model.state.value.route == Route.Home)
    check(
        "the refused entry filed nothing",
        graph.store.capturesSnapshot(live.id).size == capturesAfterClose,
    )

    // The shared card must state the same numbers as the record.
    val cardText = model.tripCardText()
    check("the card text names the trip", cardText.contains("Bangkok & Home"))
    check("the card text carries a total", cardText.contains("across"))
    check("the card text mentions the fence", cardText.contains("budget"))

    // Deletion is two taps, and the first one deletes nothing.
    model.armTripDeletion()
    settleCheck()
    check("arming deletion deletes nothing", graph.store.tripSnapshot(live.id) != null)
    check("arming is visible in state", model.state.value.deleteArmed)
    model.armTripDeletion()
    settleCheck()
    check("disarming is possible", !model.state.value.deleteArmed)

    model.armTripDeletion()
    settleCheck()
    model.deleteTripNow()
    settleCheck()
    check("confirming deletes the trip", graph.store.tripSnapshot(live.id) == null)
    // Only the deleted trip's photographs go. The earlier trip in this walk keeps
    // its own, which is the whole point of scoping a folder by trip id.
    check(
        "deletion removes that trip's photographs",
        services.files.writtenPaths.none { it.startsWith(live.id) },
    )
    check(
        "deletion leaves the other trip's photographs alone",
        services.files.writtenPaths.isNotEmpty(),
    )
    check("deletion sends you back to the front door", model.state.value.route == Route.NewTrip)
    check("deletion clears the trip from state", model.state.value.trip == null)

    // ---- the exchange actually converts ------------------------------------
    //
    // The FX engine was always exact and always had no input: `upsertRate` existed
    // and nothing ever called it, so a THB amount silently stayed a THB amount and
    // the trip total under-reported. These checks exist so that cannot happen
    // again without a gate going red.
    val fxGraph = AppGraph(FlowServices(), SystemClock())
    fxGraph.model.start()
    settle()
    fxGraph.model.editDraft(name = "Bangkok", startIso = "2026-11-24", endIso = "2026-11-30")
    fxGraph.model.commitDraft()
    settle()

    check("no rate is on file to begin with", fxGraph.model.rateLabel("THB") == null)
    check("a bad rate is refused", fxGraph.model.setFxRate("THB", "abc") == false)
    check("a zero rate is refused", fxGraph.model.setFxRate("THB", "0") == false)
    check("a real rate is accepted", fxGraph.model.setFxRate("THB", "2.5") == true)
    settle()
    check("the rate is stated as a sentence", fxGraph.model.rateLabel("THB") == "1 THB = 2.5 INR")
    check("the rate is on disk", fxGraph.store.allRatesSnapshot().isNotEmpty())

    // 100.00 THB at 2.5 INR = 250.00 INR, through the same engine the app uses.
    val table = com.ariadne.fx.FxTable(fxGraph.store.allRatesSnapshot())
    val converted = table.convert(
        com.ariadne.model.Money.ofMinor(10_000L),
        com.ariadne.model.CurrencyCode.of("THB"),
        com.ariadne.model.CurrencyCode.of("INR"),
        com.ariadne.model.LocalDay.of(2026, 11, 24),
    )
    check("a foreign amount converts", converted.amount.minorUnits == 25_000L)
    check("and the conversion is reported as applied", converted.applied)

    fxGraph.model.clearFxRate("THB")
    settle()
    check("clearing removes the rate", fxGraph.model.rateLabel("THB") == null)
    check("and it is gone from disk", fxGraph.store.allRatesSnapshot().isEmpty())

    // ---- a failed write is shown, not swallowed ----------------------------
    //
    // The rule this round established: no visible control may do nothing. If the
    // store refuses, the form says so in a sentence and stays where it is.
    val refusing = AppGraph(
        object : PlatformServices by FlowServices() {
            override fun store(): com.ariadne.store.Store = RefusingStore(InMemoryStore())
        },
        SystemClock(),
    )
    refusing.model.start()
    settle()
    refusing.model.startNewTrip()
    settle()
    refusing.model.editDraft(name = "A trip that will not save", startIso = "2026-12-01", endIso = "2026-12-06")
    refusing.model.commitDraft()
    settle()
    check("a refused write reports an error on the form", refusing.model.state.value.draft.error != null)
    check("the error says it could not be saved", refusing.model.state.value.draft.error?.contains("could not be saved") == true)
    check("a refused write does not pretend the trip opened", refusing.model.state.value.route == Route.NewTrip)
    check("the failure is kept for the diagnostic line", refusing.model.state.value.failureNote != null)

    println()
    if (failures == 0) {
        println("ALL FLOW CHECKS PASSED ($checks)")
    } else {
        println("$failures of $checks FLOW CHECKS FAILED")
        throw System.exit(1) as Nothing
    }
}

/** The model launches into a scope; give it a moment to land. */
private fun settle() {
    Thread.sleep(120)
}

/** Longer, for a call that writes to the store and then re-reads it. */
private fun settleCheck() {
    Thread.sleep(200)
}

private class FlowServices : PlatformServices {
    private val store = InMemoryStore()
    val files = RecordingFiles()

    override fun store() = store

    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
            OcrResult.Failure("no OCR in the flow check")
    }

    override fun files() = files

    override fun close() = Unit
}

/**
 * A store whose writes throw, standing in for the real defect: when `trip` grew
 * two columns without the schema version moving, the INSERT named columns the
 * table did not have. The exception was swallowed inside a launched coroutine
 * and the page sat there looking like a button that does not work.
 *
 * This class exists so that behaviour is asserted rather than remembered.
 */
private class RefusingStore(private val delegate: com.ariadne.store.Store) : com.ariadne.store.Store by delegate {
    override suspend fun createTrip(trip: com.ariadne.model.Trip) {
        throw IllegalStateException("no such column: place (code 1 SQLITE_ERROR)")
    }
}

private class RecordingFiles : com.ariadne.app.PhotoFiles {
    val writtenPaths = mutableSetOf<String>()
    private val bytes = mutableMapOf<String, ByteArray>()

    override suspend fun write(
        tripId: String,
        day: com.ariadne.model.LocalDay,
        id: String,
        bytes: ByteArray,
    ): String {
        val path = com.ariadne.app.PhotoNaming.relative(tripId, day, id)
        this.bytes[path] = bytes
        writtenPaths += path
        return path
    }

    override suspend fun read(path: String): ByteArray? = bytes[path]

    override suspend fun delete(path: String) {
        bytes.remove(path)
        writtenPaths -= path
    }

    override suspend fun footprintBytes(): Long = bytes.values.sumOf { it.size.toLong() }
}

private fun com.ariadne.store.Store.allRatesSnapshot() =
    kotlinx.coroutines.runBlocking { allRates() }

private fun com.ariadne.store.Store.settingsSnapshot(): com.ariadne.model.Settings =
    kotlinx.coroutines.runBlocking { settings() }

private fun com.ariadne.store.Store.tripsSnapshot(): List<com.ariadne.model.Trip> =
    kotlinx.coroutines.runBlocking { trips() }

private fun com.ariadne.store.Store.findCapture(id: String): com.ariadne.model.Capture? =
    kotlinx.coroutines.runBlocking { findById(id) }

private fun com.ariadne.store.Store.tripSnapshot(id: String): com.ariadne.model.Trip? =
    kotlinx.coroutines.runBlocking { trip(id) }

private fun com.ariadne.store.Store.capturesSnapshot(id: String): List<com.ariadne.model.Capture> =
    kotlinx.coroutines.runBlocking { capturesForTrip(id) }
