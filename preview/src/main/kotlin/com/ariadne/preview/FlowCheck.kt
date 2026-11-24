package com.ariadne.preview

import com.ariadne.app.AppGraph
import com.ariadne.app.PlatformServices
import com.ariadne.app.Route
import com.ariadne.app.TripDraft
import com.ariadne.core.time.SystemClock
import com.ariadne.extract.OcrResult
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

    // ---- first run: no trip, so the front door is the form -----------------
    model.start()
    settle()
    check("first run opens the new-trip form", model.state.value.route == Route.NewTrip)
    check("the form is pre-filled with today", model.state.value.draft.startIso.isNotEmpty())
    check("no trip exists yet", model.state.value.trip == null)

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

    // ---- the shutter: a frame, then two ways out --------------------------
    model.go(Route.Capture)
    settle()
    model.capturePhoto(byteArrayOf(1, 2, 3, 4))
    settle()

    val frameRoute = model.state.value.route
    check("the shutter holds the frame up", frameRoute is Route.Frame)
    val captureId = (frameRoute as? Route.Frame)?.captureId
    check("the frame has an id", captureId != null)
    check("the frame is in the record", graph.store.findCapture(captureId!!) != null)
    check("the frame waits for nightfall", model.state.value.stack.isNotEmpty())

    // ---- photo bytes actually reached storage ----------------------------
    val stored = graph.store.findCapture(captureId)
    check("the photo was written", services.files.writtenPaths.contains(stored?.photoPath))
    check(
        "the file is readable back",
        kotlinx.coroutines.runBlocking { services.files.read(stored?.photoPath ?: "")?.size } == 4,
    )

    // ---- "keep it for nightfall" loses nothing ---------------------------
    model.keepFrameForLater()
    settle()
    check("keeping it returns Home", model.state.value.route == Route.Home)
    check("keeping it does not delete the frame", graph.store.findCapture(captureId) != null)

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

    // Closing into a card must not destroy anything.
    val beforeClose = graph.store.tripSnapshot(live.id)?.closedAt
    model.closeTripIntoCard()
    settleCheck()
    check("closing the trip opens its card", model.state.value.route is Route.TripCard)
    check("closing marks the trip closed", graph.store.tripSnapshot(live.id)?.closedAt != null)
    check("closing was not already done", beforeClose == null)
    check("closing keeps the captures", graph.store.capturesSnapshot(live.id).size >= 5)

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

private fun com.ariadne.store.Store.tripsSnapshot(): List<com.ariadne.model.Trip> =
    kotlinx.coroutines.runBlocking { trips() }

private fun com.ariadne.store.Store.findCapture(id: String): com.ariadne.model.Capture? =
    kotlinx.coroutines.runBlocking { findById(id) }

private fun com.ariadne.store.Store.tripSnapshot(id: String): com.ariadne.model.Trip? =
    kotlinx.coroutines.runBlocking { trip(id) }

private fun com.ariadne.store.Store.capturesSnapshot(id: String): List<com.ariadne.model.Capture> =
    kotlinx.coroutines.runBlocking { capturesForTrip(id) }
