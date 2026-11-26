package com.ariadne.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.ariadne.app.AppModel
import com.ariadne.app.Route
import com.ariadne.app.ui.pages.CapturePage
import com.ariadne.app.ui.pages.DetailPage
import com.ariadne.app.ui.pages.FramePage
import com.ariadne.app.ui.pages.NewTripPage
import com.ariadne.app.ui.pages.HomePage
import com.ariadne.app.ui.pages.NightfallPage
import com.ariadne.app.ui.pages.OnboardingPage
import com.ariadne.app.ui.pages.RecordPage
import com.ariadne.app.ui.pages.SearchPage
import com.ariadne.app.ui.pages.StatsPage
import com.ariadne.app.ui.pages.TimelinePage
import com.ariadne.app.ui.pages.TripCardPage
import com.ariadne.app.ui.pages.TripsPage
import com.ariadne.app.ui.pages.TripPage
import com.ariadne.design.LocalAccentBudget
import com.ariadne.design.LocalMotionPreference
import com.ariadne.design.LocalSurfaceId
import com.ariadne.design.rememberAccentBudget
import com.ariadne.design.Motion
import com.ariadne.design.MotionPreference
import com.ariadne.design.Paper
import com.ariadne.model.Settings
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * The app: four tab destinations, five pages that stack on top of them, one state
 * object, one `when`.
 *
 * The tab bar is part of the shell rather than of each page, so it cannot drift
 * between screens. It is hidden on the surfaces that take the whole screen —
 * onboarding, the shutter, the ritual, and the card — because those are moments
 * rather than places, and a bar under them would make them feel like errands.
 *
 * Capture gets no tab of its own: it is the shutter in the middle of the bar,
 * which is the one control in the product that is always one tap away.
 */
@Composable
public fun AriadneApp(
    model: AppModel,
    wide: Boolean,
    /**
     * Force a stock, for previews and stills. Null — the normal case — means
     * "ask the preference, and if it says system, ask the device".
     */
    dark: Boolean? = null,
    reducedMotion: Boolean = false,
    onShutter: () -> Unit = { model.capturePlaceholder() },
) {
    LaunchedEffect(Unit) { model.start() }
    val state by model.state.collectAsState()
    // Night paper is the same design on dark stock, not a second theme: the
    // palette has lived in :core:design from the start, and this is the one place
    // that decides which half of it the app is printed on.
    val systemDark = isSystemInDarkTheme()
    val night = dark ?: when (state.theme) {
        Settings.Theme.LIGHT -> false
        Settings.Theme.DARK -> true
        Settings.Theme.SYSTEM -> systemDark
    }
    val paper = if (night) Paper.Night else Paper.Light
    val route = state.route

    CompositionLocalProvider(LocalMotionPreference provides MotionPreference(reducedMotion)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(paper.bg)
                // Bars and the keyboard, at the one place that owns the frame:
                // a page should never have to know whether the IME is open. The
                // insets are consumed here so a page's own bottom action cannot be
                // covered by a keyboard it cannot see.
                .windowInsetsPadding(WindowInsets.systemBars)
                // Consumed, then the keyboard is added on top of what is left.
                // Padding both in full would leave a dead band as tall as the
                // navigation bar between the last action and the keyboard.
                .consumeWindowInsets(WindowInsets.systemBars)
                .imePadding()
                .onPreviewKeyEvent { event -> handleKey(event, model, route) },
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    val reduceMotion = LocalMotionPreference.current.reduced
                    val spec = tween<Float>(
                        durationMillis = if (reduceMotion) 0 else Motion.Page,
                        easing = Motion.Ease,
                    )
                    Crossfade(
                        targetState = route,
                        animationSpec = spec,
                        label = "route",
                    ) { target ->
                        // A few points of sideways drift as the page arrives: the
                        // app is a book, so pages turn across, not up.
                        val drift = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(Motion.Drift) }
                        androidx.compose.runtime.LaunchedEffect(target) { drift.animateTo(0f, spec) }
                        Box(Modifier.offset(x = drift.value.dp)) {
                            // Each surface gets its own budget for the one blue
                            // action. Keyed by route: a page turn must not charge
                            // the outgoing page's blue to the incoming one.
                            CompositionLocalProvider(
                                LocalAccentBudget provides rememberAccentBudget(target),
                                LocalSurfaceId provides target,
                            ) {
                                Destination(target, state, model, paper, wide, onShutter)
                            }
                        }
                    }
                }

                if (route.showsTabBar()) {
                    TabBar(
                        paper = paper,
                        selected = route.tab(),
                        // A stacked page is announced as stacked from Home rather
                        // than as Home: a screen reader should hear where the bar
                        // is, not be told the person is on a tab they are not on.
                        barDescription = route.stackedFrom(),
                        onSelect = { tab -> model.go(tab.route()) },
                        onCapture = { model.openCapture() },
                        waiting = state.stack.size,
                        captureEnabled = state.trip?.isClosed == false,
                    )
                }
            }
        }
    }
}

@Composable
private fun Destination(
    route: Route,
    state: com.ariadne.app.AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    onShutter: () -> Unit,
) {
    when (route) {
        Route.Onboarding -> OnboardingPage(model, paper, wide)

        Route.NewTrip -> NewTripPage(state, model, paper, wide)

        is Route.Frame -> FramePage(state, model, paper, wide, route.captureId)

        // The four tabs.
        Route.Home -> HomePage(state, model, paper, wide)
        Route.Timeline -> TimelinePage(state, model, paper, wide)
        Route.Stats -> StatsPage(state, model, paper, wide)
        Route.Preferences -> PreferencesPage(state, model, paper, wide, state.failureNote)

        Route.Trip -> TripPage(state, model, paper, wide)

        Route.Capture -> CapturePage(
            state = state,
            model = model,
            paper = paper,
            wide = wide,
            onShutter = onShutter,
            // No fallback needed: the capture page opens the platform picker
            // itself through LocalPhotoPicker, and on a target with no picker it
            // does not offer the control at all.
            onImport = {},
            // Bytes from the platform camera go straight into the record; the
            // page then holds the frame up and asks what to do with it.
            onFrame = { bytes -> if (bytes.isNotEmpty()) model.capturePhoto(bytes) },
        )

        is Route.Nightfall -> NightfallPage(state, model, paper, wide)

        is Route.Record -> RecordPage(state, model, paper, wide)
        is Route.Detail -> DetailPage(state, model, paper, wide)

        is Route.TripCard -> TripCardPage(state, model, paper, wide)

        Route.Search -> SearchPage(state, model, paper, wide)

        // Retired surfaces: kept in the sealed type so persisted routes from an
        // older build still resolve, but they show the destination that replaced
        // them rather than a second copy of it.
        // The register, always. It used to fall through to the home page once a trip
        // existed, which left no way to see the others or start a new one.
        Route.Trips -> TripsPage(state, model, paper, wide)

        // Retired name: a route restored from an older build lands on the same
        // page rather than on a second copy of it.
        Route.Settings -> PreferencesPage(state, model, paper, wide, state.failureNote)
    }
}

/**
 * Which tab the current route belongs to — or none.
 *
 * The register and Preferences are *stacked* pages: they belong to the Home
 * family but they are not the Home tab, and marking Home selected while someone
 * is reading the register told them they were somewhere they were not. They get
 * a visible bar, no selected destination, and a page action back to Home.
 */
private fun Route.tab(): Tab? = when (this) {
    Route.Trips -> null
    Route.Preferences, Route.Settings -> null
    Route.Trip, is Route.TripCard -> Tab.Trip
    // Detail is a sheet with a keypad pinned to its foot, not a destination: its
    // bar is hidden (see `showsTabBar`), so there is no tab to mark either way.
    is Route.Detail -> null
    Route.Timeline, is Route.Record -> Tab.Timeline
    Route.Stats -> Tab.Stats
    Route.Home, Route.Onboarding, Route.NewTrip, Route.Capture, is Route.Frame,
    is Route.Nightfall, Route.Search -> Tab.Home
}

private fun Tab.route(): Route = when (this) {
    // Home is today's page, not the register. The register is one row down the
    // index, and pressing Home returns from wherever you wandered to today.
    Tab.Home -> Route.Home
    Tab.Timeline -> Route.Timeline
    Tab.Stats -> Route.Stats
    Tab.Trip -> Route.Trip
}

/** How a stacked page describes the bar it is showing, if it shows one. */
private fun Route.stackedFrom(): String? = when (this) {
    Route.Trips -> "Trips register, stacked from Home"
    Route.Preferences, Route.Settings -> "Preferences, stacked from Home"
    else -> null
}

/**
 * The bar is hidden where a moment should fill the screen: first run, the
 * shutter, the ritual, and the card you keep.
 */
private fun Route.showsTabBar(): Boolean = when (this) {
    Route.Onboarding, Route.NewTrip, Route.Capture,
    is Route.Nightfall, is Route.TripCard, is Route.Detail, is Route.Frame -> false
    else -> true
}

/**
 * Desktop triage, exactly as promised: digits type the amount, Enter confirms,
 * J and K move through the stack, Escape leaves Nightfall.
 */
private fun handleKey(event: KeyEvent, model: AppModel, route: Route): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (route !is Route.Nightfall) return false
    return when {
        event.key == Key.Enter || event.key == Key.NumPadEnter -> { model.confirmCurrent(); true }
        event.key == Key.J || event.key == Key.DirectionDown -> { model.next(); true }
        event.key == Key.K || event.key == Key.DirectionUp -> { model.previous(); true }
        event.key == Key.Escape -> { model.go(Route.Capture); true }
        event.key == Key.Backspace -> { model.backspace(); true }
        else -> {
            val name = event.key.toString()
            val ch = name.removePrefix("Key(").removeSuffix(")").firstOrNull()
            if (ch != null && (ch in '0'..'9' || ch == '.')) { model.typeAmount(ch); true } else false
        }
    }
}
