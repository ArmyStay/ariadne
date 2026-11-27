package com.ariadne.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.ariadne.design.Bone
import com.ariadne.design.Body
import com.ariadne.design.Label
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.design.TravelGlobeArtwork
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.ariadne.app.AppModel
import com.ariadne.app.Route
import com.ariadne.app.ui.pages.CaptureOverlay
import com.ariadne.app.ui.pages.CapturePage
import com.ariadne.app.ui.pages.DetailPage
import com.ariadne.app.ui.pages.FramePage
import com.ariadne.app.ui.pages.NewTripPage
import com.ariadne.app.ui.pages.HomePage
import com.ariadne.app.ui.pages.CloseDayPage
import com.ariadne.app.ui.pages.OnboardingPage
import com.ariadne.app.ui.pages.PreferencesPage
import com.ariadne.app.ui.pages.ReminderOverlay
import com.ariadne.app.ui.pages.RecordPage
import com.ariadne.app.ui.pages.SearchPage
import com.ariadne.app.ui.pages.StatsPage
import com.ariadne.app.ui.pages.TimelinePage
import com.ariadne.app.ui.pages.TripCardPage
import com.ariadne.app.ui.pages.TripsPage
import com.ariadne.app.ui.pages.TripPage
import com.ariadne.app.ui.pages.MorePage
import com.ariadne.app.ui.pages.ManualEntryPage
import com.ariadne.app.ui.pages.DataExportPage
import com.ariadne.app.ui.pages.HandoffPage
import com.ariadne.app.ui.pages.JoinPage
import com.ariadne.app.ui.pages.LegalPage
import com.ariadne.app.ui.pages.cameraAvailable
import com.ariadne.app.ui.pages.pickerAvailable
import com.ariadne.app.ui.pages.rememberPhotoImport
import com.ariadne.design.AriadneTheme
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
 * The app: four tab destinations, pages that stack on top of them, one state
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
    /**
     * The platform's back gesture, installed by the target that has one.
     *
     * A composable parameter rather than an `expect/actual`: only the Android
     * target has a system back gesture, the handler it needs is a composable from
     * `androidx.activity`, and this module has no Android source set to put an
     * actual in. The target passes `{ enabled, onBack -> BackHandler(enabled,
     * onBack) }` and everything else leaves it null and keeps the Escape key.
     *
     * Predictive back comes for free where the platform supports it: the callback
     * registered by `BackHandler` is what the system animates *from*, so passing
     * the real stack depth here is what makes the gesture show the page it will
     * return to instead of the launcher.
     */
    onBack: (@Composable (enabled: Boolean, onBack: () -> Unit) -> Unit)? = null,
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

    // Back, before the frame is drawn: the stack is the single source of truth for
    // whether there is anywhere to go, and the platform closes the app when there
    // is not — which is what a root destination should do.
    if (onBack != null) {
        onBack(state.canGoBack) { model.back() }
    }

    // The shell contains real Material components — the bar, its items, the docked
    // shutter — and each of them asks the theme for whatever it is not told. Without
    // this they answered in the stock purple.
    AriadneTheme(paper) {
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
                .onPreviewKeyEvent { event -> handleKey(event, model, route, state) },
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    val reduceMotion = LocalMotionPreference.current.reduced
                    val spec = tween<Float>(
                        durationMillis = if (reduceMotion) 0 else Motion.Page,
                        easing = Motion.Ease,
                    )
                    Crossfade(
                        targetState = if (state.isStartingUp) null else route,
                        animationSpec = spec,
                        label = "route",
                    ) { target ->
                        if (target == null) {
                            StartupPage(paper)
                        } else {
                            // A few points of sideways drift as the page arrives: the
                            // app is a book, so pages turn across, not up.
                            val drift = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(Motion.Drift) }
                            androidx.compose.runtime.LaunchedEffect(target) { drift.animateTo(0f, spec) }
                            Column(Modifier.fillMaxSize()) {
                                val parent = state.history.lastOrNull()
                                if (parent != null && target.needsParentBack()) {
                                    QuietAction(
                                        "Back to ${parent.backLabel()}",
                                        paper,
                                        { model.back() },
                                        modifier = Modifier.padding(
                                            start = if (wide) Bone.MarginWide else Bone.Margin,
                                            top = 8.dp,
                                            bottom = 4.dp,
                                        ),
                                        color = paper.inkSoft,
                                    )
                                }
                                Box(Modifier.weight(1f)) {
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
                        }
                    }
                }

                // What just happened, in one line, above whatever is at the foot of
                // the page — including the bar, because an undo offer must be the
                // last thing between the person and the change they want back.
                state.notice?.takeIf { state.sheetCaptureId == null && !state.shutterOpen }?.let { notice ->
                    NoticeBar(
                        notice = notice,
                        paper = paper,
                        // The offer names what it does: an undo most of the time, the
                        // day for the evening reminder.
                        onAction = { model.noticeAction() },
                        onDismiss = { model.dismissNotice() },
                    )
                }

                // The dock's shutter, on a target that has no camera.
                //
                // On a phone this is one tap to a viewfinder, which is the product. In
                // a browser there is no viewfinder, so the same tap used to open a
                // page whose frame then had to be pressed to reach the same file
                // dialog: two taps to say "here is the photograph I already took".
                // Where a picker exists, the dock goes straight to it.
                val cameraHere = cameraAvailable()
                val pickerHere = pickerAvailable()
                val bringOneIn = rememberPhotoImport(
                    onBytes = { model.importPhoto(it) },
                    onUnavailable = { model.openCapture() },
                    onFailure = model::photoImportFailed,
                )
                if (route.showsTabBar() && !state.shutterOpen && state.sheetCaptureId == null) {
                    AriadneBar(
                        paper = paper,
                        selected = route.tab(),
                        // A stacked page is announced with its actual parent, not
                        // as though the underlying destination were selected.
                        barDescription = route.stackedFrom(state.history.lastOrNull()),
                        onSelect = { tab -> model.go(tab.route()) },
                        onCapture = {
                            if (!cameraHere && pickerHere) bringOneIn() else model.openCapture()
                        },
                        waiting = state.tripPending.size,
                        captureEnabled = state.trip?.isClosed == false,
                    )
                }
            }

            // These are true covers of the current page, not extra full-height
            // children in its column. They keep the camera, sheet and dock from
            // pushing one another off-screen or competing for the same decision.
            if (state.shutterOpen) {
                CaptureOverlay(
                    state = state,
                    model = model,
                    paper = paper,
                    wide = wide,
                    onFrame = { bytes -> if (bytes.isNotEmpty()) model.capturePhoto(bytes) },
                )
            }
            state.sheetCaptureId?.let { id ->
                FramePage(state, model, paper, wide, id)
            }

            // Last, so it covers everything: the reminder question is about the app
            // rather than about where you are in it, and it is asked over whatever
            // page the trip happens to have left you on. Not over a viewfinder or a
            // photograph being framed — a question asked there is a question
            // interrupting a photograph.
            if (!state.shutterOpen && state.sheetCaptureId == null) {
                ReminderOverlay(state = state, model = model, paper = paper, wide = wide)
            }
        }
    }
    }
}

/**
 * The page between the splash and the record.
 *
 * It is the only screen in the app with nothing to do, so it is the one place the book
 * can be shown as a cover: the wordmark set large, its blue rule, the promise in tracked
 * caps, a line of serif italic underneath — and the globe-and-flight artwork bleeding off
 * the bottom-right corner, where the eye already is.
 *
 * The bar at the foot is honest about itself: nothing in this app knows how much of the
 * record has loaded, so it sweeps rather than filling to a percentage it made up, and the
 * line underneath says what is actually happening.
 */
@Composable
private fun StartupPage(paper: Paper) {
    Box(Modifier.fillMaxSize().background(paper.bg)) {
        // The artwork is full width and hangs off the bottom edge, so it has no visible
        // side to give it away; a vertical wash over its top third finishes the join
        // with the paper, because the wallpaper's sky is pale but not this pale.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .aspectRatio(0.5333f)
                .offset(y = 120.dp),
        ) {
            TravelGlobeArtwork(
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
                opacity = if (paper.isNight) 0.22f else 0.94f,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to paper.bg,
                            0.34f to paper.bg.copy(alpha = 0.55f),
                            0.55f to Color.Transparent,
                        ),
                    ),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The cover sits at a third of the height: above it is sky, below it the
            // globe — and the aeroplane, at about 56 %, is below both.
            Spacer(Modifier.weight(1.3f))
            Title("Ariadne", paper, size = 62.sp, lineHeight = 66.sp)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.width(62.dp).height(2.dp).background(paper.blue))
            Spacer(Modifier.height(22.dp))
            Label("Opening your record", paper, color = paper.inkSoft, size = 9.sp, tracking = 2.6.sp)
            Spacer(Modifier.height(34.dp))
            Body(
                "Every journey\nleaves a trace",
                paper,
                serif = true,
                italic = true,
                align = TextAlign.Center,
                color = paper.inkSoft,
                size = 17.sp,
                lineHeight = 25.sp,
            )
            Spacer(Modifier.weight(1.7f))
            StartupBar(paper)
            Spacer(Modifier.height(16.dp))
            Label("Preparing your journey", paper, color = paper.inkSoft, size = 8.sp, tracking = 2.6.sp)
            Spacer(Modifier.height(44.dp))
        }
    }
}

/**
 * A segment of navy sweeping along a hairline track.
 *
 * Not a percentage: the app has already opened its database by the time this is on
 * screen, and the wait is the platform's, not a figure anyone here can measure.
 */
@Composable
private fun StartupBar(paper: Paper) {
    val sweep = rememberInfiniteTransition(label = "startup")
    val phase by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase",
    )
    Canvas(Modifier.width(150.dp).height(3.dp)) {
        val w = size.width
        val h = size.height
        val round = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(paper.edgeStrong.copy(alpha = 0.6f), cornerRadius = round)
        val segment = w * 0.34f
        drawRoundRect(
            color = paper.navy,
            topLeft = Offset((w - segment) * phase, 0f),
            size = Size(segment, h),
            cornerRadius = round,
        )
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

        Route.JoinTrip -> JoinPage(state, model, paper, wide)

        Route.Handoff -> HandoffPage(state, model, paper, wide)

        is Route.Frame -> FramePage(state, model, paper, wide, route.captureId)

        // The four tabs.
        Route.Home -> HomePage(state, model, paper, wide)
        Route.Days -> TimelinePage(state, model, paper, wide)
        Route.Trip -> TimelinePage(state, model, paper, wide)
        Route.Stats -> StatsPage(state, model, paper, wide)
        Route.Preferences -> PreferencesPage(state, model, paper, wide, state.failureNote)
        Route.More -> MorePage(state, model, paper, wide)
        Route.ManualEntry -> ManualEntryPage(state, model, paper, wide)
        Route.DataExport -> DataExportPage(state, model, paper, wide)
        Route.PrivacyPolicy -> LegalPage(state, paper, wide, terms = false)
        Route.Terms -> LegalPage(state, paper, wide, terms = true)

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

        is Route.CloseDay -> CloseDayPage(state, model, paper, wide)

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
 * How a stacked page describes the bar it is showing, if it shows one.
 *
 * A screen reader should hear where the bar belongs, not be told the person is on
 * a destination they are not on.
 */
private fun Route.stackedFrom(parent: Route?): String? = when (this) {
    Route.Trips -> "Trips, stacked over ${parent?.backLabel() ?: "the current page"}"
    Route.ManualEntry -> "Manual entry, stacked over ${parent?.backLabel() ?: "the current page"}"
    Route.DataExport -> "Data export, stacked over ${parent?.backLabel() ?: "the current page"}"
    Route.Preferences, Route.Settings ->
        "Preferences, stacked over ${parent?.backLabel() ?: "the current page"}"
    else -> null
}

private fun Route.needsParentBack(): Boolean = when (this) {
    Route.Home, Route.Days, Route.Trip, Route.Stats, Route.More, Route.Capture, is Route.Frame -> false
    else -> true
}

private fun Route.backLabel(): String = when (this) {
    Route.Home -> "Home"
    Route.Days, Route.Trip -> "Timeline"
    Route.Stats -> "Stats"
    Route.More -> "More"
    Route.Trips -> "the register"
    Route.Preferences, Route.Settings -> "Preferences"
    Route.Search -> "Search"
    is Route.Record -> "the record"
    is Route.CloseDay -> "Close day"
    is Route.Detail -> "the photograph"
    is Route.TripCard -> "the trip card"
    Route.NewTrip -> "new trip"
    Route.JoinTrip -> "join a trip"
    Route.Handoff -> "the codes"
    Route.ManualEntry -> "manual entry"
    Route.DataExport -> "data export"
    Route.PrivacyPolicy -> "privacy"
    Route.Terms -> "terms"
    Route.Onboarding -> "the first page"
    Route.Capture -> "the shutter"
    is Route.Frame -> "the photograph"
}

/**
 * The bar is hidden where a moment should fill the screen: first run, the
 * shutter, the ritual, and the card you keep.
 */
private fun Route.showsTabBar(): Boolean = when (this) {
    Route.Onboarding, Route.NewTrip, Route.JoinTrip, Route.Capture,
    is Route.CloseDay, is Route.TripCard, is Route.Detail, is Route.Frame -> false
    else -> true
}

/**
 * Desktop triage, exactly as promised: digits type the amount, Enter confirms,
 * J and K move through the stack, Escape leaves the pager.
 */
private fun handleKey(
    event: KeyEvent,
    model: AppModel,
    route: Route,
    state: com.ariadne.app.AppUiState,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    // Whatever is open over the page closes first, exactly as Back does. Escape is
    // the desktop's version of that gesture and should not mean something else.
    if (event.key == Key.Escape &&
        (state.shutterOpen || state.sheetCaptureId != null || state.reminderSheetOpen)
    ) {
        model.back()
        return true
    }
    if (route !is Route.CloseDay) return false
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

/**
 * The line at the foot of the page: what just happened, and the way back if there
 * is one.
 *
 * Not a Material `Snackbar`: that brings a dark rounded surface and an elevation
 * into a book made of hairlines and paper, and it floats over the bar rather than
 * sitting on the page. This is a ruled strip in the app's own language, with the
 * text on the left and the one action on the right. It is a live region for a
 * screen reader, so an undo offer is announced when it appears rather than being
 * something only sighted users get.
 */
@Composable
private fun NoticeBar(
    notice: com.ariadne.app.Notice,
    paper: Paper,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(paper.raised)) {
        Rule(paper, color = paper.edgeStrong)
        Row(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = com.ariadne.design.Bone.Margin, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(
                notice.text,
                paper,
                color = paper.inkSoft,
                size = 10.sp,
                tracking = 1.2.sp,
                modifier = Modifier.weight(1f),
            )
            if (notice.actionLabel != null) {
                QuietAction(notice.actionLabel, paper, onAction, color = paper.blue)
                Spacer(Modifier.width(12.dp))
            }
            QuietAction("close", paper, onDismiss, color = paper.inkFaint)
        }
    }
}
