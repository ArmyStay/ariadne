package com.ariadne.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The page grid.
 *
 * Every screen used to be one centred 620dp column with 24dp margins and
 * everything stacked in it. That is the default shape of generated UI: a single
 * column, uniform vertical rhythm, no second axis — it reads as a form no matter
 * what type is put in it.
 *
 * This is a two-axis grid instead:
 *
 *  - a **hanging margin** (left rail) that carries dates, numbers, labels and
 *    section marks, and
 *  - a **text column** that never exceeds [Bone.Measure], so prose stays
 *    readable however wide the window gets,
 *  - with the whole thing hung from the *left* of the page rather than floated in
 *    the middle, which is what a printed book does.
 *
 * On a phone the rail collapses to zero and the grid becomes one column again —
 * that is the honest mobile answer, not a compromise.
 */
public object Grid {
    /** Width of the hanging margin on a wide page. */
    public val Rail: Dp = 132.dp

    /** Space between the rail and the text column. */
    public val RailGutter: Dp = 28.dp

    /** The widest a full spread (rail + column) is allowed to be. */
    public val Spread: Dp = 980.dp
}

/**
 * The page itself: paper, inset, and hung from the left on a wide screen.
 *
 * `rail` is drawn only when there is room for it. The content column is what
 * scrolls, or the whole page does — pages choose, because a page with a pinned
 * column and a scrolling middle is a different thing.
 */
@Composable
public fun Page(
    paper: Paper,
    wide: Boolean,
    modifier: Modifier = Modifier,
    railDocked: Boolean = true,
    onToggleRail: (() -> Unit)? = null,
    rail: (@Composable ColumnScope.() -> Unit)? = null,
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier.fillMaxSize().background(paper.bg),
        contentAlignment = Alignment.TopStart,
    ) {
        // The wash a page opens on, if it has one: drawn first, filling the page,
        // never inside the scroller. A backdrop that scrolls is a photograph of
        // paper; one that stays put is the paper itself.
        if (backdrop != null) {
            Box(Modifier.fillMaxSize()) { backdrop() }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .widthIn(max = Grid.Spread + Bone.MarginWide * 2)
                .padding(
                    start = if (wide) Bone.MarginWide else Bone.Margin,
                    end = if (wide) Bone.MarginWide else Bone.Margin,
                    top = if (wide) 34.dp else 20.dp,
                ),
        ) {
            // The fold control lives at the seam, as the first thing in the row:
            // a margin mark, not a button floating over the page. It stays put
            // when the margin folds, because it is the only way back.
            if (wide && rail != null && onToggleRail != null) {
                Column(Modifier.width(26.dp).padding(top = 4.dp)) {
                    FoldRail(
                        paper = paper,
                        docked = railDocked,
                        description = if (railDocked) "Fold the margin" else "Unfold the margin",
                        onClick = onToggleRail,
                    )
                }
            }

            // The hanging margin folds away and comes back. It is furniture, so
            // it animates with the same curve as everything else rather than
            // snapping: 180ms, width only, no bounce - the column beside it simply
            // gets the room back.
            if (wide && rail != null) {
                val shown = railDocked
                val width = androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (shown) Grid.Rail else 0.dp,
                    animationSpec = motionSpec(),
                    label = "rail",
                )
                val gutter = androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (shown) Grid.RailGutter else 0.dp,
                    animationSpec = motionSpec(),
                    label = "railGutter",
                )
                Column(
                    Modifier.width(width.value).then(
                        if (shown) Modifier else Modifier.clipToBounds(),
                    ),
                ) {
                    if (shown) {
                        Column(Modifier.width(Grid.Rail).padding(top = 26.dp)) { rail() }
                    }
                }
                Spacer(Modifier.width(gutter.value))
            }
            Column(
                Modifier
                    .widthIn(max = if (wide) Bone.Measure + 120.dp else Dp.Unspecified)
                    .fillMaxWidth(),
            ) { content() }
        }
    }
}

/**
 * A top-of-page action.
 *
 * Everything at the top of a page used to render at 9-10px in `inkFaint` with a
 * hairline under it, which is invisible at a glance on a real screen - the report
 * was "all the features on top are kinda invisible", and it was accurate. The
 * type goes up a step, the ink goes to `inkMid`, and the target gets a real hit
 * area. Restrained is not the same as faint.
 */
@Composable
public fun PageAction(
    label: String,
    paper: Paper,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    Box(
        Modifier
            .clipToBounds()
            .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = Bone.Sans,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                ),
                color = if (accent) paper.blue else paper.inkMid,
            )
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .width(if (accent) 26.dp else 16.dp)
                    .height(if (accent) 2.dp else Bone.Hairline)
                    .background(if (accent) paper.blue else paper.edgeStrong),
            )
        }
    }
}

/**
 * The fold control for the hanging margin.
 *
 * A glyph rather than a word, because "collapse sidebar" in 10px tracked uppercase
 * is the least premium thing a page can say. Two hairlines and an arrow: the
 * margin, folding.
 */
@Composable
private fun FoldRail(
    paper: Paper,
    docked: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(30.dp)
            .semantics { contentDescription = description }
            .pointerInput(docked) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(17.dp)) {
            // inkSoft, not inkFaint: the point of a control is that it can be
            // found without hunting for it.
            val ink = paper.inkSoft
            // Two vertical rules: the page and its margin.
            drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * 0.18f, 0f), androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height), 1.6f)
            // The arrow points the way the margin will go.
            val from = if (docked) size.width * 0.95f else size.width * 0.45f
            val to = if (docked) size.width * 0.45f else size.width * 0.95f
            drawLine(ink, androidx.compose.ui.geometry.Offset(from, size.height / 2), androidx.compose.ui.geometry.Offset(to, size.height / 2), 1.6f)
            val tip = if (docked) to else from
            drawLine(ink, androidx.compose.ui.geometry.Offset(tip, size.height / 2), androidx.compose.ui.geometry.Offset(tip - 3.5f * (if (docked) -1f else 1f), size.height / 2 - 3.5f), 1.6f)
            drawLine(ink, androidx.compose.ui.geometry.Offset(tip, size.height / 2), androidx.compose.ui.geometry.Offset(tip - 3.5f * (if (docked) -1f else 1f), size.height / 2 + 3.5f), 1.6f)
        }
    }
}

/**
 * The masthead: an eyebrow over a headline, and a rule under it.
 *
 * The eyebrow is separated from the headline by a **numbered mark** on the left
 * (I, II, III …) rather than by colour or weight alone, which is how a printed
 * page indexes itself. Pages pass their own number so the app reads as a
 * sequence of plates rather than a set of screens.
 */
@Composable
public fun Masthead(
    number: String,
    eyebrow: String,
    title: String,
    paper: Paper,
    modifier: Modifier = Modifier,
    italic: Boolean = false,
    wide: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    lede: String? = null,
    titleSize: TextUnit? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Surfaces outside the trip's sequence (the front door and the
                // register) pass a blank number: no plate mark, just the eyebrow.
                // That keeps 01..09 meaning one thing - a page of the book - and
                // stops two pages claiming the same plate.
                if (number.isNotBlank()) {
                    Label(number, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(18.dp).height(Bone.Hairline).background(paper.edgeStrong))
                    Spacer(Modifier.width(10.dp))
                }
                Label(eyebrow, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.6.sp)
            }
            if (trailing != null) trailing()
        }

        Spacer(Modifier.height(if (wide) 22.dp else 16.dp))

        // A page whose headline *is* the page (the timeline) passes its own size;
        // everything else takes the book's display scale, so the mastheads stay
        // one family.
        val headline = titleSize ?: if (wide) (Bone.display.value * Optical.displayScale(true)).sp else Bone.display
        Title(
            title,
            paper,
            size = headline,
            italic = italic,
            lineHeight = if (wide) headline * 1.08f else headline * 1.02f,
        )

        if (lede != null) {
            Spacer(Modifier.height(14.dp))
            Body(
                lede,
                paper,
                serif = true,
                color = paper.inkSoft,
                size = if (wide) 18.sp else 16.sp,
                lineHeight = if (wide) 28.sp else 25.sp,
            )
        }

        Spacer(Modifier.height(if (wide) 22.dp else 18.dp))
        Rule(paper, color = paper.ink)
    }
}

/**
 * A section head inside a page: a numeral, a tracked label, and the rule that
 * closes the section above it.
 *
 * Sections are numbered in roman, which costs nothing and makes the app feel
 * edited rather than scrolled.
 */
@Composable
public fun SectionHead(mark: String, title: String, paper: Paper, trailing: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(30.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(mark, paper, color = paper.blue, size = 9.sp, tracking = 1.4.sp)
                Spacer(Modifier.width(10.dp))
                Label(title, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.8.sp)
            }
            if (trailing != null) {
                Label(trailing, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * A section head marked by a rule, not by a numeral.
 *
 * [SectionHead] numbers its sections in roman, which suits a page that is read
 * straight through. A journey page is read as a sequence of places and days, and
 * a roman numeral in front of "the journey" would be a third numbering system on
 * a page that already has a plate number and a date. The mark is a short upright
 * bar instead: the same hairline the page is ruled with, stood on end.
 */
@Composable
public fun MarkHead(
    title: String,
    paper: Paper,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(30.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(2.dp).height(13.dp).background(paper.ink))
                Spacer(Modifier.width(12.dp))
                Label(title, paper, color = paper.inkSoft, size = 9.sp, tracking = 1.8.sp)
            }
            if (trailing != null) {
                Label(trailing, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * A figure hung on the hanging margin: label above, value below, both left
 * aligned to the rail so a column of them lines up down the page.
 */
@Composable
public fun RailFigure(label: String, value: String, paper: Paper, size: TextUnit = 15.sp) {
    Column(Modifier.fillMaxWidth()) {
        Label(label, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
        Spacer(Modifier.height(5.dp))
        Numeral(value, paper, size = size, weight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

/** A vertical rule: the spine between the rail and the column on a spread. */
@Composable
public fun Spine(paper: Paper, height: Dp = 240.dp) {
    Box(Modifier.width(Bone.Hairline).height(height).background(paper.edge))
}



// ── The scroll bar ──────────────────────────────────────────────────────────

/**
 * A scroll bar in the page's own hand.
 *
 * Compose's desktop bar is a Material widget: a rounded grey thumb that floats
 * inside the content and grows when hovered. On this paper it reads as a light
 * left on in the wrong room. So the bar is drawn as a margin mark instead — a
 * hairline track down the outer margin, and a thumb that is simply a heavier
 * line moving along it.
 *
 * Two things make it feel set rather than supplied:
 *
 *  - it sits *outside* the text column, in the margin where a folio would be, so
 *    it never covers a word;
 *  - it is quiet until touched. At rest the thumb is faint; while the page is
 *    moving it is ink. Nothing fades, nothing grows, nothing rounds.
 *
 * It also answers the question a wheel cannot: *how much of this is there?* A
 * page with 40dp of remainder gets a thumb almost the full height, which says
 * "one line more" rather than "one screen more".
 *
 * Drag the thumb to scrub; drag the track to jump. Both are already in the hand.
 */
@Composable
public fun ScrollRail(
    state: ScrollState,
    paper: Paper,
    modifier: Modifier = Modifier,
) {
    if (state.maxValue <= 0) return

    BoxWithConstraints(modifier.width(RailBar.Width)) {
        val show = constraints.maxHeight > 0
        if (!show) return@BoxWithConstraints

        val density = androidx.compose.ui.platform.LocalDensity.current
        val trackPx = constraints.maxHeight.toFloat()
        val viewportPx = trackPx
        val totalPx = viewportPx + state.maxValue.toFloat()
        // Never shorter than a thumb you can actually catch, even on a long page.
        val thumbPx = (trackPx * (viewportPx / totalPx))
            .coerceIn(with(density) { RailBar.MinThumb.toPx() }, trackPx)
        val travel = (trackPx - thumbPx).coerceAtLeast(1f)
        val progress = if (state.maxValue == 0) 0f else state.value.toFloat() / state.maxValue
        val offsetPx = travel * progress.coerceIn(0f, 1f)

        var held by remember { mutableStateOf(false) }
        // Highlight while the page moves for any reason: a wheel, a key, a drag.
        val live = held || state.isScrollInProgress

        Box(
            Modifier
                .fillMaxHeight()
                .width(RailBar.Width)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (travel <= 0f) return@rememberDraggableState
                        val target = (offsetPx + delta) / travel
                        state.dispatchRawDelta(target.coerceIn(0f, 1f) * state.maxValue - state.value)
                    },
                    onDragStarted = { held = true },
                    onDragStopped = { held = false },
                ),
        ) {
            // The track: a hairline, not a groove. It says where you may go
            // without pretending to be a control in its own right.
            // The track is barely there: a suggestion of a rail, so the thumb has
            // somewhere to have come from. Drawn at 40% so it never competes with
            // the type it sits beside.
            Box(
                Modifier
                    .offset(x = RailBar.Rule)
                    .fillMaxHeight()
                    .width(Bone.Hairline)
                    .background(paper.edge.copy(alpha = 0.4f)),
            )
            // The thumb is a rule, not a pill: two points wide, square, no radius,
            // no shadow, no hover growth. It moves; that is the whole interaction.
            Box(
                Modifier
                    .offset(x = RailBar.Rule - 1.dp, y = with(density) { offsetPx.toDp() })
                    .width(if (live) 3.dp else 2.dp)
                    .height(with(density) { thumbPx.toDp() })
                    .background(if (live) paper.ink else paper.inkFaint),
            )
        }
    }
}

/** The bar's dimensions, in one place so pages never disagree about them. */
public object RailBar {
    /** Total width reserved in the margin, including the gap to the text. */
    public val Width: Dp = 18.dp

    /** Where the hairline sits inside that width. */
    public val Rule: Dp = 9.dp

    /** Shortest catchable thumb. */
    public val MinThumb: Dp = 28.dp
}

/**
 * A page that scrolls, with its bar in the outer margin.
 *
 * Every long page used to put the bar on the *inside* of its own padding, which
 * put a grey strip of browser between the text and the edge of the paper. The
 * margin is the app's oldest idea — it is where the rail, the folio and the
 * section marks already live — so the bar belongs there too, and the text keeps
 * its measure untouched.
 *
 * The bar hangs in the gutter, half the page margin wide: enough to catch, not
 * enough to be a second column.
 */
@Composable
public fun ScrollingMargin(
    paper: Paper,
    wide: Boolean,
    margin: Dp = if (wide) Bone.MarginWide else Bone.Margin,
    vertical: Dp = 22.dp,
    ground: Color? = null,
    content: @Composable ColumnScope.(ScrollState) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxSize().background(ground ?: paper.bg)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = margin, vertical = vertical),
        ) { content(scroll) }
        Box(
            Modifier
                .fillMaxHeight()
                .padding(end = (margin - 4.dp).coerceAtLeast(2.dp))
                .padding(top = vertical, bottom = vertical),
        ) {
            ScrollRail(scroll, paper, Modifier.fillMaxHeight())
        }
    }
}
