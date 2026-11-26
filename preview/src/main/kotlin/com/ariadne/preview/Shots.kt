package com.ariadne.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.ariadne.app.AppGraph
import com.ariadne.app.AppModel
import com.ariadne.app.PlatformServices
import com.ariadne.app.Route
import com.ariadne.app.ui.AriadneApp
import com.ariadne.core.time.SystemClock
import com.ariadne.design.AccentAudit
import com.ariadne.model.Settings
import com.ariadne.extract.OcrResult
import com.ariadne.extract.TextRecognizer
import com.ariadne.store.InMemoryStore
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders each page to a PNG with no window, no AWT and no display.
 *
 * [ImageComposeScene] composes the real UI into a Skia surface, so what lands in
 * `docs/shots/` is produced by the same code the four app targets compile — not a
 * mockup. Used to review the design and to generate stills for the README.
 *
 *   java … com.ariadne.preview.ShotsKt [outDir]
 */
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "docs/shots").apply { mkdirs() }

    // A phone-shaped column and a desktop-shaped spread for every page that has
    // a different layout at each width.
    data class Shot(
        val name: String,
        val w: Int,
        val h: Int,
        val seed: Boolean = true,
        /** Which stock to print on. Night is the same design, not a second one. */
        val theme: Settings.Theme = Settings.Theme.LIGHT,
        /** System text size. 2f is the setting an accessibility check uses. */
        val fontScale: Float = 1f,
        val route: (AppModel) -> Unit = {},
        val settle: Int = 0,
        /** How far down the page to scroll before the still is taken. */
        val scrollTo: Float = 0f,
    )

    // Each shot's route lambda is called `settle` times, so a flow that needs more
    // than one step keeps its own cursor here rather than inside the model.
    var budgetStep = 0

    val shots = listOf(
        // The front door on a first run: no trip, so the form.
        Shot("01-new-trip", 440, 900, seed = false, route = { it.startNewTrip() }),
        Shot("01-new-trip-desktop", 1200, 800, seed = false, route = { it.startNewTrip() }),
        Shot("01-onboarding", 440, 900, seed = false),
        Shot("02-today", 440, 900, route = { it.go(Route.Home) }),
        Shot("02-today-desktop", 1200, 800, route = { it.go(Route.Home) }),
        // Part-scrolled, so the still shows the scroll rail half way down and not
        // only in the state where it is easy to draw.
        Shot("02-today-scrolled", 440, 900, route = { it.go(Route.Home) }, scrollTo = 0.55f),
        Shot("03-days-scrolled", 440, 900, route = { it.go(Route.Days) }, scrollTo = 0.4f),
        Shot("06-record-scrolled", 440, 900, route = { it.openRecord(null) }, settle = 1, scrollTo = 0.6f),
        Shot("03-days", 440, 900, route = { it.go(Route.Days) }),
        // The register: every trip on this device, one of them active.
        Shot("02a-trips", 440, 900, route = { it.go(Route.Trips) }),
        Shot("02a-trips-desktop", 1200, 800, route = { it.go(Route.Trips) }),
        Shot("02c-stats", 440, 900, route = { it.go(Route.Stats) }),
        Shot("02c-stats-desktop", 1200, 800, route = { it.go(Route.Stats) }),
        // Finished through the real ritual, so the breakdown below is the one the
        // app would actually compute — not a fixture.
        Shot(
            "02c-stats-filled", 440, 900, settle = 8,
            route = { m ->
                if (m.state.value.stack.isNotEmpty()) m.confirmCurrent() else m.go(Route.Stats)
            },
        ),
        Shot("02d-prefs", 440, 900, route = { it.go(Route.Preferences) }),
        Shot("03-trip", 440, 900, route = { it.go(Route.Trip) }),
        Shot("03-trip-desktop", 1200, 800, route = { it.go(Route.Trip) }),
        // The camera over the page you were on: the flow change this phase is about.
        // Nothing navigated, so this still composes Today underneath.
        Shot("04-shutter", 440, 900, route = { it.go(Route.Home); it.openCapture() }),
        Shot("04-shutter-desktop", 1200, 800, route = { it.go(Route.Home); it.openCapture() }),
        // The sheet: the frame, over the same page. Taken through the real capture
        // path so the plate is a real one.
        Shot("04-sheet", 440, 900, route = { it.capturePlaceholder() }, settle = 1),
        Shot("04-sheet-desktop", 1200, 800, route = { it.capturePlaceholder() }, settle = 1),
        // The sheet with a figure in it: the pair, the rate and the field live at
        // the same time, which is the point of the block.
        Shot(
            "04-sheet-typed", 440, 900, settle = 4,
            route = { m ->
                if (m.state.value.sheetCaptureId == null) {
                    m.capturePlaceholder()
                } else if (m.state.value.detailAmount.isEmpty()) {
                    "18.41".forEach { m.editDetailAmount(it) }
                }
            },
        ),
        // The standalone page, which is now only reached when there is no trip to
        // file a photograph into.
        Shot("04-capture-no-trip", 440, 900, route = { it.go(Route.Capture) }),
        // The frame just taken, held up with its two ways out.
        Shot(
            "04b-frame", 440, 900, settle = 4,
            route = { m -> m.capturePlaceholder() },
        ),
        // The front door on a phone with the tab bar hidden, and a seeded Stats page
        // so the budget fence is visible against real numbers.
        // The whole stack finished through the real ritual, then Stats: the budget
        // line is arithmetic over captures that went through Nightfall, not a fixture.
        // A fence is set on the trip (the same call a budget field makes), then the
        // whole stack is finished through the real ritual, then Stats: the budget
        // line is arithmetic over captures that went through Nightfall.
        Shot("02c-stats-budget", 440, 900, settle = 12, route = { m ->
            when {
                budgetStep == 0 -> {
                    m.editTripBudget("140000")
                    m.openCloseDay(null)
                }
                m.state.value.stack.isNotEmpty() -> m.confirmCurrent()
                else -> m.go(Route.Stats)
            }
            budgetStep++
        }),
        Shot("05-close-day", 440, 900, route = { it.openCloseDay(null) }),
        Shot("05-close-day-desktop", 1200, 800, route = { it.openCloseDay(null) }),
        Shot("06-record", 440, 900, route = { it.openRecord(null) }, settle = 1),
        Shot("06b-detail", 440, 900, route = { it.openDetailFirst() }, settle = 1),
        Shot("06b-detail-desktop", 1200, 800, route = { it.openDetailFirst() }, settle = 1),
        Shot("07-trip-card", 440, 1000, route = { it.go(Route.TripCard("0")) }),
        Shot("08-search", 440, 900, route = { it.go(Route.Search) }),
        // Night paper, now reachable from Preferences instead of only from a still.
        Shot("10-night", 440, 900, theme = Settings.Theme.DARK, route = { it.go(Route.Home) }),
        Shot(
            "10-night-card",
            440,
            900,
            theme = Settings.Theme.DARK,
            route = { it.go(Route.TripCard("0")) },
        ),
        // Twice the system text size. These exist so the claim "the layout grows
        // rather than clips" can be checked by looking at a file.
        Shot("02-today-200", 440, 900, fontScale = 2f, route = { it.go(Route.Home) }),
        Shot("05-close-day-200", 440, 900, fontScale = 2f, route = { it.openCloseDay(null) }),
        Shot("02d-prefs-200", 440, 900, fontScale = 2f, route = { it.go(Route.Preferences) }),
        // The rest of the 200% audit. Every surface the review asked about gets a
        // still at twice the system text size, because "it reflows" is a claim a
        // person should be able to check by opening a file, and the register, the
        // record, the detail sheet, the card and the triage page all grew new
        // rows since the last render.
        Shot("01-onboarding-200", 440, 900, seed = false, fontScale = 2f),
        Shot("01-new-trip-200", 440, 900, seed = false, fontScale = 2f, route = { it.startNewTrip() }),
        Shot("02a-trips-200", 440, 900, fontScale = 2f, route = { it.go(Route.Trips) }),
        Shot("03-days-200", 440, 900, fontScale = 2f, route = { it.go(Route.Days) }),
        Shot("03-trip-200", 440, 900, fontScale = 2f, route = { it.go(Route.Trip) }),
        Shot("04-shutter-200", 440, 900, fontScale = 2f, route = { it.go(Route.Home); it.openCapture() }),
        Shot("06-record-200", 440, 900, fontScale = 2f, route = { it.openRecord(null) }, settle = 1),
        Shot("06b-detail-200", 440, 900, fontScale = 2f, route = { it.openDetailFirst() }, settle = 1),
        Shot("07-trip-card-200", 440, 1000, fontScale = 2f, route = { it.go(Route.TripCard("0")) }),
        Shot("08-search-200", 440, 900, fontScale = 2f, route = { it.go(Route.Search) }),
        // Landscape at 200%: the shortest viewport the app has to survive, with
        // the tallest type. The bar, the pinned keypad and the bottom action all
        // have to stay reachable here or the audit's claim is only true in portrait.
        Shot("06b-detail-200-landscape", 900, 440, fontScale = 2f, route = { it.openDetailFirst() }, settle = 1),
        Shot("02a-trips-200-landscape", 900, 440, fontScale = 2f, route = { it.go(Route.Trips) }),
        // The closed-trip matrix, drawn: a finished trip's Home and its shutter.
        // Both are states the third review said had never been rendered.
        Shot(
            "02e-home-closed", 440, 900, settle = 3,
            route = { m ->
                if (m.state.value.trip?.isClosed == true) m.go(Route.Home) else m.closeTripIntoCard()
            },
        ),
        Shot(
            "04-capture-closed", 440, 900, settle = 3,
            route = { m ->
                if (m.state.value.trip?.isClosed == true) m.go(Route.Capture) else m.closeTripIntoCard()
            },
        ),
    )

    AccentAudit.clear()
    shots.forEach { shot ->
        render(
            outDir,
            shot.name,
            shot.w,
            shot.h,
            shot.seed,
            shot.settle,
            shot.theme,
            shot.fontScale,
            shot.scrollTo,
            shot.route,
        )
    }
    println("rendered ${shots.size} pages into ${outDir.absolutePath}")

    // The one-blue rule, checked against every surface that was just drawn. A
    // design rule that is only written down is a rule that erodes; this makes the
    // stills pass fail instead.
    if (AccentAudit.violations.isNotEmpty()) {
        println()
        println("two blue actions on one surface:")
        AccentAudit.violations.forEach { println("  $it") }
        kotlin.system.exitProcess(1)
    }
    println("one blue action per surface: clean")
}

private fun render(
    outDir: File,
    name: String,
    width: Int,
    height: Int,
    seed: Boolean,
    settle: Int,
    theme: Settings.Theme,
    fontScale: Float,
    scrollTo: Float,
    route: (AppModel) -> Unit,
) {
    val graph = AppGraph(ShotServices(), SystemClock())
    val model = graph.model
    model.start()
    settleBlocking(120)
    // Wait for the preference to reach the store: `AriadneApp` calls start() from
    // a LaunchedEffect, which reloads settings, and a still taken before that
    // write lands would be printed on yesterday's paper.
    model.setTheme(theme)
    settleBlocking(150)
    if (seed) {
        // Creating a trip is asynchronous by design, so the route is posted only
        // once the store has actually accepted it.
        model.startSeedTrip()
        settleBlocking(200)
    }
    // `AriadneApp` starts the model from a LaunchedEffect, so the route has to be
    // posted *after* the first composition, or start() re-binds the trip and
    // navigates over the top of it.
    val scene = ImageComposeScene(
        width = width,
        height = height,
        density = Density(1f, fontScale),
        content = { PreviewRoot(model, wide = width > 900) },
    )
    scene.render()
    settleBlocking(120)
    repeat(settle.coerceAtLeast(1)) {
        route(model)
        settleBlocking(160)
    }
    // Advance the scene clock past the 180ms page turn, otherwise the still
    // catches a cross-fade at its first frame and looks like a broken route.
    // A wheel a person would have made. The rail has to be right at rest and in
    // motion, so at least three stills catch it part-way down a page.
    if (scrollTo > 0f) {
        scene.render(50_000_000L)
        val target = kotlin.math.round(140f * scrollTo).toInt()
        repeat(target) {
            scene.sendPointerEvent(
                eventType = androidx.compose.ui.input.pointer.PointerEventType.Scroll,
                position = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f),
                scrollDelta = androidx.compose.ui.geometry.Offset(0f, 1f),
            )
        }
        settleBlocking(220)
    }

    var nano = 1_000_000_000L
    repeat(24) {
        scene.render(nano)
        nano += 16_000_000L
    }
    val image = scene.render(nano)
    val data = image.encodeToData(EncodedImageFormat.PNG)
        ?: error("could not encode $name")
    File(outDir, "$name.png").writeBytes(data.bytes)
    scene.close()
    graph.close()
    println("  $name (${width}×${height})")
}

/** Opens the first photograph of the day in the detail page. */
private fun AppModel.openDetailFirst() {
    val first = state.value.dayCaptures.firstOrNull() ?: return
    openDetail(first.id)
}

/**
 * Pump the model's own coroutines without a test dispatcher: the model runs on
 * Dispatchers.Default, so a short sleep is the honest way to let a write land
 * before the frame is composed.
 */
private fun settleBlocking(millis: Long) = Thread.sleep(millis)

@Composable
private fun PreviewRoot(model: AppModel, wide: Boolean) {
    // No `dark` override: the shot sets the model's preference, exactly as a person
    // would, so the stills prove the preference works rather than the parameter.
    AriadneApp(model = model, wide = wide)
}

private class ShotServices : PlatformServices {
    private val store = InMemoryStore()
    override fun store(): com.ariadne.store.Store = store
    override fun recognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(imagePath: String): OcrResult =
            OcrResult.Failure("render pass: OCR not bound")
    }
    override fun close() = Unit
}
