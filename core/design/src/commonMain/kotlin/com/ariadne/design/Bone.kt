package com.ariadne.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ariadne's design language: Ink.
 *
 * Colour is taken from the reference palette — a cool off-white ground, a single
 * confident blue, and a slate for everything loud. The typography and the bones
 * come from the second reference: high-contrast serif headlines set large, small
 * tracked-out uppercase labels, ink-filled controls that sit *on* the page rather
 * than floating above it, and a filled tab bar under a camera button.
 *
 * The rules are short enough to hold in your head:
 *
 *   1. Blue is the only chromatic value, and it means "this is the thing to do".
 *      It appears once per surface. Everything else is ink, paper or slate.
 *   2. Headlines are serif. Labels are tracked uppercase. Numbers are tabular.
 *   3. Corners are square on control surfaces; only the shutter is a circle.
 *   4. There is no red in the palette, so there is no red in the product.
 *   5. Whitespace is the layout. When in doubt, remove something.
 */
public object Bone {

    // ── paper ────────────────────────────────────────────────────────────────
    /** Page. Sampled from the reference: a cool off-white, never cream. */
    public val Paper: Color = Color(0xFFF8FAFC)

    /** A field, a card, a raised row. */
    public val PaperRaised: Color = Color(0xFFFFFFFF)

    /** The sunk strip: tab bar, table headers, the plate ground. */
    public val PaperSunk: Color = Color(0xFFF1F4F7)

    /** Hairlines and frames. */
    public val Edge: Color = Color(0xFFE2E8F0)
    public val EdgeStrong: Color = Color(0xFFCBD5E1)

    // ── ink ──────────────────────────────────────────────────────────────────
    public val Ink: Color = Color(0xFF1F1F20)
    public val InkMid: Color = Color(0xFF475569)
    public val InkSoft: Color = Color(0xFF64748B)
    public val InkFaint: Color = Color(0xFF94A3B8)
    public val InkGhost: Color = Color(0xFFCBD5E1)

    /** The one chromatic value. A button, a selected tab, a figure you acted on. */
    public val Blue: Color = Color(0xFF2563EB)
    public val BluePressed: Color = Color(0xFF1D4ED8)
    public val BlueWash: Color = Color(0xFFEFF4FE)
    public val BlueEdge: Color = Color(0xFFBFD3FB)

    // ── night ────────────────────────────────────────────────────────────────
    /** Night is the same design on dark stock: blue lifts, ink inverts. */
    public val Night: Color = Color(0xFF111318)
    public val NightRaised: Color = Color(0xFF181B21)
    public val NightSunk: Color = Color(0xFF0C0E12)
    public val NightEdge: Color = Color(0xFF272B33)
    public val NightEdgeStrong: Color = Color(0xFF3A404A)

    public val NightInk: Color = Color(0xFFF2F5F9)
    public val NightInkMid: Color = Color(0xFFB6C0CC)
    public val NightInkSoft: Color = Color(0xFF8794A3)
    public val NightInkFaint: Color = Color(0xFF616C7A)
    public val NightInkGhost: Color = Color(0xFF3A404A)

    public val NightBlue: Color = Color(0xFF7DA2F7)
    public val NightBlueWash: Color = Color(0xFF1A2436)
    public val NightBlueEdge: Color = Color(0xFF2E4470)

    // ── rules & rhythm ───────────────────────────────────────────────────────
    public val Hairline: Dp = 1.dp
    public val Margin: Dp = 24.dp
    public val MarginWide: Dp = 56.dp
    public val Gutter: Dp = 16.dp

    /** The text column never exceeds this, however wide the window. */
    public val Measure: Dp = 620.dp

    public val FieldHeight: Dp = 52.dp
    public val ButtonHeight: Dp = 56.dp
    public val Radius: Dp = 10.dp
    public val RadiusLarge: Dp = 16.dp

    // ── type ─────────────────────────────────────────────────────────────────
    /**
     * Headline serif: Instrument Serif. High contrast, tight, and set large
     * enough that it carries the page on its own.
     */
    public val Serif: FontFamily by lazy {
        FontFamily(
            Font("fonts/InstrumentSerif-Regular.ttf", FontWeight.Normal, FontStyle.Normal),
            Font("fonts/InstrumentSerif-Italic.ttf", FontWeight.Normal, FontStyle.Italic),
        )
    }

    /** UI grotesque: Inter Tight. Everything that is read rather than admired. */
    public val Sans: FontFamily by lazy {
        FontFamily(Font("fonts/InterTight-Variable.ttf", FontWeight.Normal))
    }

    /** Numbers. Tabular so a column of figures lines up like a ledger. */
    public val Mono: FontFamily get() = Sans

    public val display: TextUnit = 40.sp
    public val title: TextUnit = 30.sp
    public val heading: TextUnit = 19.sp
    public val body: TextUnit = 15.sp
    public val monoLarge: TextUnit = 40.sp
    public val monoSmall: TextUnit = 13.sp

    /** The label: 10px, uppercase, tracked. Used on every field and tab. */
    public val label: TextUnit = 10.sp
    public val labelTracking: TextUnit = 2.2.sp
    public val labelWeight: FontWeight = FontWeight.Medium
}

/** Which stock the app is currently printed on. */
public data class Paper(
    public val isNight: Boolean,
) {
    public val bg: Color get() = if (isNight) Bone.Night else Bone.Paper
    public val raised: Color get() = if (isNight) Bone.NightRaised else Bone.PaperRaised
    public val sunk: Color get() = if (isNight) Bone.NightSunk else Bone.PaperSunk
    public val edge: Color get() = if (isNight) Bone.NightEdge else Bone.Edge
    public val edgeStrong: Color get() = if (isNight) Bone.NightEdgeStrong else Bone.EdgeStrong
    public val ink: Color get() = if (isNight) Bone.NightInk else Bone.Ink
    public val inkMid: Color get() = if (isNight) Bone.NightInkMid else Bone.InkMid
    public val inkSoft: Color get() = if (isNight) Bone.NightInkSoft else Bone.InkSoft
    public val inkFaint: Color get() = if (isNight) Bone.NightInkFaint else Bone.InkFaint
    public val inkGhost: Color get() = if (isNight) Bone.NightInkGhost else Bone.InkGhost
    public val blue: Color get() = if (isNight) Bone.NightBlue else Bone.Blue
    public val blueWash: Color get() = if (isNight) Bone.NightBlueWash else Bone.BlueWash
    public val blueEdge: Color get() = if (isNight) Bone.NightBlueEdge else Bone.BlueEdge

    /** Ink for text that sits *on* blue. */
    public val onBlue: Color get() = if (isNight) Bone.Night else Bone.PaperRaised

    /** Kept for the pages that still say `thread`; blue is the thread now. */
    public val thread: Color get() = blue

    /** A rule meant to be barely there: table rows, plate ground. */
    public val hairline: Color get() = if (isNight) Bone.NightEdge else Bone.Edge

    public companion object {
        public val Light: Paper = Paper(isNight = false)
        public val Night: Paper = Paper(isNight = true)
    }
}
