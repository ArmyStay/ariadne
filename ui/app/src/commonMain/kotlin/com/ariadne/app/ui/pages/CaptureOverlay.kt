package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.design.Action
import com.ariadne.design.Bone
import com.ariadne.design.Body
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.LocalDay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

/**
 * The camera, open over the page you were on.
 *
 * This is the change that makes the shutter feel like a shutter. Before, tapping it
 * navigated to a viewfinder *page*, and the photograph arrived on a second page after
 * that — three screens to keep one receipt, two of which were the same nothing, and
 * the screen you actually wanted back had to be rebuilt when you returned.
 *
 * Now nothing navigates. The camera rises over Today (or Days, or the day's record),
 * the frame becomes the sheet over that same page, and putting the amount in drops
 * you back exactly where you stood: same tab, same day, same scroll, with the day's
 * figure already moved.
 *
 * The page itself is a plate in a book: paper with a wash of blue and a route drawn
 * across it ([ScannerBackdrop]), a dark frame with crop marks at its corners
 * ([PlateMarks]), the hint beneath it between two hairlines, and the shutter in the
 * middle of the library and the day's open figure.
 *
 * The glasses are one small mark in the corner, not a banner. Pairing is a thing you
 * do once, so it is behind a mark that shows the state it is in; the shutter then
 * takes the picture the glasses see, and the phone stays in the pocket.
 */
@Composable
public fun CaptureOverlay(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    onFrame: (ByteArray) -> Unit,
) {
    val shoot = shutterAction()
    val scope = rememberCoroutineScope()
    val glasses = LocalWearableCapture.current

    // The shutter takes the best camera that is actually there, in this order: the
    // glasses you are wearing, the phone's own sensor, and finally a ruled plate —
    // because a shutter that does nothing is not a shutter, and a photograph that
    // never arrived must never be filed as one.
    val glassesReady = glasses != null && glasses.connected
    val fire: () -> Unit = when {
        glassesReady && glasses != null -> ({
            scope.launch {
                val frame = glasses.capture()
                if (frame != null) model.importPhotos(listOf(frame)) else model.capturePlaceholder()
            }
        })
        cameraAvailable() -> ({ shoot() })
        else -> ({ model.capturePlaceholder() })
    }

    // The library, on the same overlay: a receipt you already have is the same
    // intent as one in front of you.
    val import = rememberPhotoImportMany(
        onBytes = { model.importPhotos(it) },
        onUnavailable = {},
        onFailure = model::photoImportFailed,
    )

    val hint = if (cameraAvailable()) {
        "Tap the frame to keep it"
    } else {
        "No camera here — tap the frame for a plate"
    }
    val waiting = if (state.tripPending.isEmpty()) {
        "Nothing waiting"
    } else {
        "${state.tripPending.size} waiting"
    }
    val onWaiting: () -> Unit = { model.closeShutter(); model.openCloseDay(state.today) }

    var showGlasses by remember { mutableStateOf(false) }
    var showDays by remember { mutableStateOf(false) }

    // The arrangement follows the window, not a flag: `MainActivity` reports `wide =
    // false` for every phone, so a phone turned on its side would otherwise get the
    // tall layout in 390dp of height and a frame two fingers wide. Anything wider than
    // it is tall takes the two-column page — the frame on the left, the controls on the
    // right — which is also what the wash and the arcs were drawn for.
    BoxWithConstraints(Modifier.fillMaxSize().background(paper.bg)) {
        val across = wide || maxWidth > maxHeight
        ScannerBackdrop(paper, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 56.dp else 22.dp, vertical = 16.dp),
        ) {
            OverlayHead(
                tripName = state.trip?.name ?: "Ariadne",
                paper = paper,
                glasses = glasses,
                onOpenGlasses = { showGlasses = true },
                onClose = { model.closeShutter() },
            )
            Spacer(Modifier.height(12.dp))

            if (across) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FrameBox(paper, Modifier.weight(1.25f).fillMaxHeight(), fire, onFrame)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        HintLine(hint, paper)
                        Spacer(Modifier.height(12.dp))
                        DayLine(state.shutterDay, state.today, paper) { showDays = true }
                        Spacer(Modifier.height(22.dp))
                        ShutterRow(
                            paper = paper,
                            fire = fire,
                            onImport = import,
                            waiting = waiting,
                            waitingQuiet = state.tripSettled,
                            onWaiting = onWaiting,
                        )
                        Spacer(Modifier.height(18.dp))
                        FigureNote(paper, state.captureError)
                    }
                }
            } else {
                FrameBox(paper, Modifier.fillMaxWidth().weight(1f), fire, onFrame)
                Spacer(Modifier.height(14.dp))
                HintLine(hint, paper)
                Spacer(Modifier.height(8.dp))
                DayLine(state.shutterDay, state.today, paper) { showDays = true }
                Spacer(Modifier.height(14.dp))
                ShutterRow(
                    paper = paper,
                    fire = fire,
                    onImport = import,
                    waiting = waiting,
                    waitingQuiet = state.tripSettled,
                    onWaiting = onWaiting,
                )
                Spacer(Modifier.height(14.dp))
                FigureNote(paper, state.captureError)
                Spacer(Modifier.height(6.dp))
            }
        }

        if (showGlasses) {
            GlassesPanel(paper = paper, wearable = glasses, onClose = { showGlasses = false })
        }
        if (showDays) {
            DaySheet(
                days = model.daysOpenForFiling(),
                selected = state.shutterDay,
                today = state.today,
                paper = paper,
                onChoose = { day ->
                    model.chooseShutterDay(day)
                    showDays = false
                },
                onClose = { showDays = false },
            )
        }
    }
}

/**
 * The head of the overlay: the way out on the left, the journey this photograph
 * belongs to on the right, and — when the platform has them — one small mark for the
 * glasses, showing whether they are paired.
 *
 * Closing is the same gesture as `Back`: `closeShutter()` puts the camera away and
 * leaves you on the page underneath, with nothing to undo.
 */
@Composable
private fun OverlayHead(
    tripName: String,
    paper: Paper,
    glasses: WearableCapture?,
    onOpenGlasses: () -> Unit,
    onClose: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QuietAction("Close", paper, onClose, color = paper.ink)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Label(tripName, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.8.sp)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(44.dp).height(1.dp).background(paper.blueEdge))
        }
        if (glasses != null) {
            Spacer(Modifier.width(10.dp))
            GlassesButton(glasses, paper, onOpenGlasses)
        }
    }
}

/**
 * The glasses, as one mark: a pair of rims on a 38dp target, blue once they are
 * paired, in the corner where a status belongs. The word for a screen reader is the
 * pairing state itself, so the mark never has to be described twice.
 */
@Composable
private fun GlassesButton(wearable: WearableCapture, paper: Paper, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (wearable.connected) paper.blueWash else Color.Transparent)
            .semantics { contentDescription = "Meta AI glasses: ${wearable.status}" }
            .rowTap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlassesMark(if (wearable.connected) paper.blue else paper.inkSoft, 18.dp)
    }
}

/**
 * The one setup that is needed: what the glasses are, what state they are in, and the
 * single thing to press. Pairing happens in Meta AI, so the panel says that plainly
 * rather than pretending to own the flow.
 */
@Composable
private fun GlassesPanel(paper: Paper, wearable: WearableCapture?, onClose: () -> Unit) {
    val connected = wearable?.connected == true
    val state = wearable?.status ?: "Not on this device"
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(paper.scrim.copy(alpha = 0.45f))
                .rowTap(onClose),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(paper.raised)
                .border(Bone.Hairline, paper.edge, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Label("meta ai glasses", paper, color = paper.inkGhost, size = 8.sp, tracking = 1.4.sp)
            Spacer(Modifier.height(10.dp))
            Title(
                if (connected) "They are paired." else "Not paired yet.",
                paper,
                size = 24.sp,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(8.dp))
            Body(
                when {
                    wearable == null -> "This device has no glasses to reach."
                    connected -> "The shutter takes the picture the glasses see, so the phone can stay in your pocket."
                    else -> "Pair once in Meta AI. After that the shutter uses the glasses, and this panel is only a status."
                },
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(14.dp))
            Rule(paper, color = paper.edgeStrong)
            Spacer(Modifier.height(12.dp))
            Label(state, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            Spacer(Modifier.height(16.dp))
            if (wearable != null) {
                if (connected) {
                    Action(
                        "Disconnect",
                        paper,
                        { wearable.disconnect() },
                        fill = false,
                        corner = 14.dp,
                    )
                } else {
                    Action(
                        "Connect",
                        paper,
                        { wearable.connect() },
                        fill = false,
                        corner = 14.dp,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            QuietAction("Done", paper, onClose, color = paper.inkSoft)
        }
    }
}

/**
 * The frame: the camera's own picture, dark by design, with crop marks over it.
 *
 * The ground is ink rather than blank paper so the frame is a photograph the moment
 * the page opens — before the sensor has delivered its first frame, and on a device
 * with no sensor at all, where the shutter keeps a ruled plate instead.
 */
@Composable
private fun FrameBox(
    paper: Paper,
    modifier: Modifier,
    onFire: () -> Unit,
    onFrame: (ByteArray) -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(paper.ink)
            .semantics { contentDescription = "Viewfinder. Tap to keep the photograph." }
            .rowTap(onFire),
    ) {
        CameraPlate(paper, Modifier.fillMaxSize(), live = true, onFrame = onFrame)
        PlateMarks(paper.raised, Modifier.fillMaxSize())
    }
}

/** The hint, between two hairlines that fade out at the margins. */
@Composable
private fun HintLine(text: String, paper: Paper) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hairline(paper, Modifier.weight(0.32f))
        Label(
            text,
            paper,
            color = paper.inkSoft,
            size = 9.sp,
            tracking = 1.4.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Hairline(paper, Modifier.weight(0.32f))
    }
}

/**
 * Which day this photograph will belong to, said out loud.
 *
 * The shutter used to file everything into today without ever saying so, which is
 * fine until the receipt in your hand is yesterday's. It prints the day it will use
 * and it is the way to change it: a tap opens [DaySheet], which lists the trip's days
 * that have happened and nothing else. Today is named rather than dated, because
 * "today" is how a person thinks about the receipt in front of them.
 */
@Composable
private fun DayLine(day: LocalDay?, today: LocalDay?, paper: Paper, onChange: () -> Unit) {
    val text = when {
        day == null -> "No day to file into yet"
        today != null && day == today -> "This goes on today, ${spokenDay(day.iso)}"
        else -> "This goes on ${spokenDay(day.iso)}"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Glyph.Date, paper, size = 12.dp, color = paper.inkFaint)
        Spacer(Modifier.width(7.dp))
        QuietAction(text, paper, onChange, color = paper.inkSoft)
    }
}

/**
 * The choice of day, as a page of the calendar rather than a wheel.
 *
 * Only days that have happened are here — not disabled, absent — so the rule is
 * something you can see rather than something you are told after trying. The sheet
 * says the rule once, in a sentence, and then gets out of the way.
 */
@Composable
private fun DaySheet(
    days: List<LocalDay>,
    selected: LocalDay?,
    today: LocalDay?,
    paper: Paper,
    onChoose: (LocalDay) -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(paper.scrim.copy(alpha = 0.45f))
                .rowTap(onClose),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(paper.raised)
                .border(Bone.Hairline, paper.edge, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Label("the day", paper, color = paper.inkGhost, size = 8.sp, tracking = 1.4.sp)
            Spacer(Modifier.height(10.dp))
            Title("Which day is this for?", paper, size = 24.sp, lineHeight = 30.sp)
            Spacer(Modifier.height(8.dp))
            Body(
                "A photograph belongs to a day that has happened. Yesterday's receipt " +
                    "can be filed on yesterday; tomorrow's cannot be filed at all.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (days.isEmpty()) {
                Body(
                    "None of this journey's days has happened yet, so there is nothing here to file into.",
                    paper,
                    color = paper.inkFaint,
                    size = 13.sp,
                    lineHeight = 19.sp,
                )
            } else {
                Column(Modifier.heightIn(max = 232.dp).verticalScroll(rememberScrollState())) {
                    days.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { day ->
                                DayPip(
                                    day = day,
                                    selected = day == selected,
                                    isToday = today != null && day == today,
                                    paper = paper,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onChoose(day) },
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Rule(paper, color = paper.edgeStrong)
            Spacer(Modifier.height(12.dp))
            QuietAction("Done", paper, onClose, color = paper.inkSoft)
        }
    }
}

/** One day in the sheet: the weekday in small caps, the date under it. */
@Composable
private fun DayPip(
    day: LocalDay,
    selected: Boolean,
    isToday: Boolean,
    paper: Paper,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = if (selected) paper.onBlue else paper.inkMid
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) paper.blue else Color.Transparent)
            .border(
                Bone.Hairline,
                if (selected) paper.blueEdge else paper.edge,
                RoundedCornerShape(10.dp),
            )
            .semantics { contentDescription = "File on ${spokenDay(day.iso)}" }
            .rowTap(onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(
            if (isToday) "today" else weekdayName(day),
            paper,
            color = if (selected) ink else paper.inkFaint,
            size = 7.sp,
            tracking = 1.sp,
        )
        Spacer(Modifier.height(3.dp))
        Label(spokenDay(day.iso), paper, color = ink, size = 10.sp, tracking = 0.6.sp)
    }
}

/**
 * The library, the shutter, and the day's open figure — the shutter in the middle,
 * where a thumb already is, and the two others as quiet capitals beside it.
 *
 * The two side labels are one word and a count on purpose: the reference truncates
 * both, and a bar that ends in an ellipsis reads as a bug rather than as a bar.
 */
@Composable
private fun ShutterRow(
    paper: Paper,
    fire: () -> Unit,
    onImport: () -> Unit,
    waiting: String,
    waitingQuiet: Boolean,
    onWaiting: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Offered only where a picker exists: the shutter is one way in, the library
        // is the other, and a control that cannot work is not offered.
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (pickerAvailable()) {
                QuietAction("Library", paper, onImport, color = paper.ink)
            }
        }
        Shutter(paper, fire)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            QuietAction(
                waiting,
                paper,
                onWaiting,
                color = if (waitingQuiet) paper.inkFaint else paper.inkMid,
            )
        }
    }
}

/** *"no figure here · the amount comes after"*, in the reference's pale capsule. */
@Composable
private fun FigureNote(paper: Paper, error: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let {
            Body(it, paper, color = paper.inkMid, size = 12.sp)
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(paper.blueWash)
                .border(Bone.Hairline, paper.blueEdge, CircleShape)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Budget, paper, size = 12.dp, color = paper.inkSoft)
            Spacer(Modifier.width(8.dp))
            Label(
                "no figure here · the amount comes after",
                paper,
                color = paper.inkSoft,
                size = 9.sp,
                tracking = 1.3.sp,
            )
        }
    }
}

@Composable
private fun Hairline(paper: Paper, modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(paper.blueEdge.copy(alpha = 0.75f)))
}

/**
 * The empty case: a camera with no trip to file into.
 *
 * Kept as a real screen rather than an overlay, because there is nothing to overlay:
 * the person has not started a journey, and the honest answer is a page that says so
 * with the two ways forward.
 */
@Composable
public fun NoTripForCamera(model: AppModel, paper: Paper, closed: Boolean) {
    Column(Modifier.fillMaxSize().background(paper.bg).padding(Bone.Margin)) {
        Spacer(Modifier.height(30.dp))
        Title(
            if (closed) "This trip is closed." else "No trip is open.",
            paper,
            size = 30.sp,
            italic = true,
        )
        Spacer(Modifier.height(12.dp))
        Label(
            if (closed) {
                "Its photographs are still here, and its card is still yours."
            } else {
                "A photograph needs a journey to belong to."
            },
            paper,
            color = paper.inkSoft,
            size = 11.sp,
            tracking = 0.4.sp,
        )
        Spacer(Modifier.height(20.dp))
        QuietAction("the register", paper, { model.go(com.ariadne.app.Route.Trips) }, color = paper.inkMid)
    }
}
