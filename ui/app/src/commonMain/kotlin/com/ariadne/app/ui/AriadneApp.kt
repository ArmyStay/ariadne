package com.ariadne.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ariadne.app.ui.pages.PrefsPage
import com.ariadne.app.ui.pages.RecordPage
import com.ariadne.app.ui.pages.SearchPage
import com.ariadne.app.ui.pages.StatsPage
import com.ariadne.app.ui.pages.TimelinePage
import com.ariadne.app.ui.pages.TripCardPage
import com.ariadne.app.ui.pages.TripPage
import com.ariadne.design.LocalMotionPreference
import com.ariadne.design.Motion
import com.ariadne.design.MotionPreference
import com.ariadne.design.Paper

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
    dark: Boolean = false,
    reducedMotion: Boolean = false,
    onShutter: () -> Unit = { model.capturePlaceholder() },
) {
    LaunchedEffect(Unit) { model.start() }
    val state by model.state.collectAsState()
    val paper = if (dark) Paper.Night else Paper.Light
    val route = state.route

    CompositionLocalProvider(LocalMotionPreference provides MotionPreference(reducedMotion)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(paper.bg)
                .windowInsetsPadding(WindowInsets.systemBars)
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
                        Destination(target, state, model, paper, wide, onShutter)
                    }
                }

                if (route.showsTabBar()) {
                    TabBar(
                        paper = paper,
                        selected = route.tab(),
                        onSelect = { tab -> model.go(tab.route()) },
                        onCapture = { model.go(Route.Capture) },
                        waiting = state.stack.size,
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
        Route.Prefs -> PrefsPage(state, model, paper, wide)

        Route.Trip -> TripPage(state, model, paper, wide)

        Route.Capture -> CapturePage(
            state = state,
            model = model,
            paper = paper,
            wide = wide,
            onShutter = onShutter,
            onImport = { model.go(Route.Record(state.today)) },
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
        Route.Trips -> if (state.trip == null) NewTripPage(state, model, paper, wide) else HomePage(state, model, paper, wide)

        Route.Settings -> PrefsPage(state, model, paper, wide)
    }
}

/** Which tab the current route belongs to. */
private fun Route.tab(): Tab = when (this) {
    Route.Timeline, is Route.Record, is Route.Detail -> Tab.Timeline
    Route.Stats -> Tab.Stats
    Route.Prefs, Route.Settings -> Tab.Prefs
    else -> Tab.Home
}

private fun Tab.route(): Route = when (this) {
    Tab.Home -> Route.Home
    Tab.Timeline -> Route.Timeline
    Tab.Stats -> Route.Stats
    Tab.Prefs -> Route.Prefs
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
