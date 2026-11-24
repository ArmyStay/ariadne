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
        val night: Boolean = false,
        val route: (AppModel) -> Unit = {},
        val settle: Int = 0,
    )

    // Each shot's route lambda is called `settle` times, so a flow that needs more
    // than one step keeps its own cursor here rather than inside the model.
    var budgetStep = 0

    val shots = listOf(
        // The front door on a first run: no trip, so the form.
        Shot("01-new-trip", 440, 900, seed = false, route = { it.startNewTrip() }),
        Shot("01-new-trip-desktop", 1200, 800, seed = false, route = { it.startNewTrip() }),
        Shot("01-onboarding", 440, 900, seed = false),
        Shot("02-home", 440, 900, route = { it.go(Route.Home) }),
        Shot("02-home-desktop", 1200, 800, route = { it.go(Route.Home) }),
        Shot("02b-timeline", 440, 900, route = { it.go(Route.Timeline) }),
        // The register: every trip on this device, one of them active.
        Shot("02a-trips", 440, 900, route = { it.go(Route.Trips) }),
        Shot("02a-trips-desktop", 1200, 800, route = { it.go(Route.Trips) }),
        Shot("02c-stats", 440, 900, route = { it.go(Route.Stats) }),
        // Finished through the real ritual, so the breakdown below is the one the
        // app would actually compute — not a fixture.
        Shot(
            "02c-stats-filled", 440, 900, settle = 8,
            route = { m ->
                if (m.state.value.stack.isNotEmpty()) m.confirmCurrent() else m.go(Route.Stats)
            },
        ),
        Shot("02d-prefs", 440, 900, route = { it.go(Route.Prefs) }),
        Shot("03-trip", 440, 900, route = { it.go(Route.Trip) }),
        Shot("04-capture", 440, 900, route = { it.go(Route.Capture) }),
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
                    m.openNightfall(null)
                }
                m.state.value.stack.isNotEmpty() -> m.confirmCurrent()
                else -> m.go(Route.Stats)
            }
            budgetStep++
        }),
        Shot("05-nightfall", 440, 900, route = { it.openNightfall(null) }),
        Shot("05-nightfall-desktop", 1200, 800, route = { it.openNightfall(null) }),
        Shot("06-record", 440, 900, route = { it.openRecord(null) }, settle = 1),
        Shot("06b-detail", 440, 900, route = { it.openDetailFirst() }, settle = 1),
        Shot("06b-detail-desktop", 1200, 800, route = { it.openDetailFirst() }, settle = 1),
        Shot("07-trip-card", 440, 1000, route = { it.go(Route.TripCard("0")) }),
        Shot("08-search", 440, 900, route = { it.go(Route.Search) }),
        Shot("10-night", 440, 900, night = true, route = { it.go(Route.Home) }),
    )

    shots.forEach { shot ->
        render(outDir, shot.name, shot.w, shot.h, shot.seed, shot.settle, shot.night, shot.route)
    }
    println("rendered ${shots.size} pages into ${outDir.absolutePath}")
}

private fun render(
    outDir: File,
    name: String,
    width: Int,
    height: Int,
    seed: Boolean,
    settle: Int,
    night: Boolean,
    route: (AppModel) -> Unit,
) {
    val graph = AppGraph(ShotServices(), SystemClock())
    val model = graph.model
    model.start()
    settleBlocking(120)
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
        density = Density(1f),
        content = { PreviewRoot(model, wide = width > 900, night = night) },
    )
    scene.render()
    settleBlocking(120)
    repeat(settle.coerceAtLeast(1)) {
        route(model)
        settleBlocking(160)
    }
    // Advance the scene clock past the 180ms page turn, otherwise the still
    // catches a cross-fade at its first frame and looks like a broken route.
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
private fun PreviewRoot(model: AppModel, wide: Boolean, night: Boolean) {
    AriadneApp(model = model, wide = wide, dark = night)
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
