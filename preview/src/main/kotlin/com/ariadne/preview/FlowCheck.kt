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

private fun com.ariadne.store.Store.tripsSnapshot(): List<com.ariadne.model.Trip> =
    kotlinx.coroutines.runBlocking { trips() }

private fun com.ariadne.store.Store.findCapture(id: String): com.ariadne.model.Capture? =
    kotlinx.coroutines.runBlocking { findById(id) }
