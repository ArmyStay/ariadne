package com.ariadne.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.Route
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Paper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The bottom bar: four destinations and the shutter.
 *
 * This replaces a bar that was drawn from scratch as a filled path with a notch cut
 * out of it for a circular shutter. It was the most distinctive thing on the
 * screen and the least useful: the notch cut the bar's own labels in half on a
 * 440dp phone, the disc overlapped the middle two destinations, and at 200% text
 * the words under the icons were clipped by the curve.
 *
 * So it is a Material 3 `NavigationBar` now — the platform's own control, which
 * already handles large text, RTL, insets and the accessibility tree — with the
 * shutter as a docked FAB in the middle slot. The slot is a real empty item, so the
 * FAB sits in space the bar reserved for it and no label is ever underneath it.
 */
public enum class Tab(public val label: String) {
    Today("Today"),
    Days("Days"),
    Trip("Trip"),
    Stats("Stats"),
}

@Composable
public fun AriadneBar(
    paper: Paper,
    /** The destination being shown, or null on a page stacked over all of them. */
    selected: Tab?,
    onSelect: (Tab) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    waiting: Int = 0,
    barDescription: String? = null,
    /** False on a closed trip: a finished record takes no photographs. */
    captureEnabled: Boolean = true,
) {
    val described = barDescription
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (described != null) Modifier.semantics { contentDescription = described } else Modifier,
            ),
    ) {
        NavigationBar(
            containerColor = paper.raised,
            tonalElevation = 0.dp,
        ) {
            BarItem(Tab.Today, selected, paper, waiting) { onSelect(Tab.Today) }
            BarItem(Tab.Days, selected, paper, 0) { onSelect(Tab.Days) }
            // The shutter's seat. An empty item rather than a notch: the bar keeps
            // its rhythm, the FAB sits in reserved space, and nothing overlaps.
            Box(Modifier.weight(1f)) {}
            BarItem(Tab.Trip, selected, paper, 0) { onSelect(Tab.Trip) }
            BarItem(Tab.Stats, selected, paper, 0) { onSelect(Tab.Stats) }
        }

        // The one control that is always one tap away, docked rather than floating
        // loose: it sits on the bar's top edge, in the middle slot, and the bar's
        // labels are below it where they cannot collide.
        FloatingActionButton(
            onClick = { if (captureEnabled) onCapture() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(56.dp)
                // Not `clearAndSetSemantics`: the FAB's role and click action are
                // what make it usable with a screen reader. Only the words change.
                .semantics {
                    contentDescription = when {
                        !captureEnabled -> "A closed trip takes no photographs"
                        waiting > 0 -> "Take a photograph. $waiting waiting for an amount"
                        else -> "Take a photograph"
                    }
                },
            shape = CircleShape,
            containerColor = if (captureEnabled) paper.blue else paper.edge,
            contentColor = if (captureEnabled) paper.onBlue else paper.inkFaint,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
        ) {
            ApertureGlyph(if (captureEnabled) paper.onBlue else paper.inkFaint)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarItem(
    tab: Tab,
    selected: Tab?,
    paper: Paper,
    waiting: Int,
    onClick: () -> Unit,
) {
    val active = tab == selected
    NavigationBarItem(
        selected = active,
        onClick = onClick,
        icon = {
            Box {
                TabGlyph(tab, if (active) paper.ink else paper.inkSoft)
                if (tab == Tab.Today && waiting > 0) {
                    // A numeral, not a dot: six photographs waiting is information,
                    // and "something is happening" is not.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-6).dp)
                            .background(paper.blue, CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Label("$waiting", paper, color = paper.onBlue, size = 9.sp)
                    }
                }
            }
        },
        label = {
            Label(
                tab.label,
                paper,
                color = if (active) paper.ink else paper.inkSoft,
                size = 11.sp,
                // Sentence case, no tracking: the brief's rule, and Material's own.
                tracking = 0.2.sp,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = paper.ink,
            selectedTextColor = paper.ink,
            unselectedIconColor = paper.inkSoft,
            unselectedTextColor = paper.inkSoft,
            indicatorColor = paper.sunk,
        ),
    )
}

/** Four glyphs cut from the same plate: today, a run of days, the trip, the bars. */
@Composable
private fun TabGlyph(tab: Tab, color: Color) {
    Canvas(Modifier.size(22.dp).clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = 1.4.dp.toPx())
        when (tab) {
            Tab.Today -> {
                // A sun over a horizon: the day you are in.
                drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.36f), style = s)
                drawLine(color, Offset(w * 0.12f, h * 0.74f), Offset(w * 0.88f, h * 0.74f), s.width)
                drawLine(color, Offset(w * 0.26f, h * 0.88f), Offset(w * 0.74f, h * 0.88f), s.width)
            }

            Tab.Days -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.28f)
                    lineTo(w * 0.92f, h * 0.28f)
                    lineTo(w * 0.92f, h * 0.88f)
                    lineTo(w * 0.08f, h * 0.88f)
                    close()
                }
                drawPath(p, color, style = s)
                drawLine(color, Offset(w * 0.08f, h * 0.44f), Offset(w * 0.92f, h * 0.44f), s.width)
                drawLine(color, Offset(w * 0.30f, h * 0.16f), Offset(w * 0.30f, h * 0.34f), s.width)
                drawLine(color, Offset(w * 0.70f, h * 0.16f), Offset(w * 0.70f, h * 0.34f), s.width)
            }

            Tab.Trip -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.36f)
                    lineTo(w * 0.92f, h * 0.36f)
                    lineTo(w * 0.92f, h * 0.84f)
                    lineTo(w * 0.08f, h * 0.84f)
                    close()
                }
                drawPath(p, color, style = s)
                // The handle.
                drawLine(color, Offset(w * 0.34f, h * 0.36f), Offset(w * 0.34f, h * 0.22f), s.width)
                drawLine(color, Offset(w * 0.34f, h * 0.22f), Offset(w * 0.66f, h * 0.22f), s.width)
                drawLine(color, Offset(w * 0.66f, h * 0.22f), Offset(w * 0.66f, h * 0.36f), s.width)
            }

            Tab.Stats -> {
                drawLine(color, Offset(w * 0.16f, h * 0.86f), Offset(w * 0.16f, h * 0.58f), s.width)
                drawLine(color, Offset(w * 0.5f, h * 0.86f), Offset(w * 0.5f, h * 0.24f), s.width)
                drawLine(color, Offset(w * 0.84f, h * 0.86f), Offset(w * 0.84f, h * 0.46f), s.width)
            }
        }
    }
}

/** A camera aperture, drawn: circle, inner ring, six blades. */
@Composable
private fun ApertureGlyph(color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val c = Offset(w / 2f, w / 2f)
        val s = Stroke(width = 1.5.dp.toPx())
        drawCircle(color, radius = w * 0.40f, center = c, style = s)
        drawCircle(color, radius = w * 0.17f, center = c, style = s)
        for (i in 0 until 6) {
            val a = PI * i / 3 + 0.4
            drawLine(
                color,
                Offset(c.x + (cos(a) * w * 0.18f).toFloat(), c.y + (sin(a) * w * 0.18f).toFloat()),
                Offset(c.x + (cos(a) * w * 0.40f).toFloat(), c.y + (sin(a) * w * 0.40f).toFloat()),
                strokeWidth = s.width * 0.85f,
            )
        }
    }
}

/** Which destination a route belongs to, or none for a page stacked over all. */
public fun Route.tab(): Tab? = when (this) {
    Route.Home -> Tab.Today
    Route.Days, is Route.Record, is Route.CloseDay -> Tab.Days
    Route.Trip, is Route.TripCard -> Tab.Trip
    Route.Stats -> Tab.Stats
    // Stacked over every destination, or a moment with no bar at all.
    Route.Trips, Route.Preferences, Route.Settings, Route.Search, is Route.Detail,
    Route.Onboarding, Route.NewTrip, Route.Capture, is Route.Frame -> null
}

/** Where a destination leads. */
public fun Tab.route(): Route = when (this) {
    Tab.Today -> Route.Home
    Tab.Days -> Route.Days
    Tab.Trip -> Route.Trip
    Tab.Stats -> Route.Stats
}
