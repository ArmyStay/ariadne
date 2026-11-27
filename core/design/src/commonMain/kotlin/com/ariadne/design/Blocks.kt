package com.ariadne.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The blocks that were being retyped on every page.
 *
 * Each of these started life as a private function at the bottom of whichever
 * page needed it first, then got copied into the next page with a slightly
 * different padding value. They are here now because they were the same object
 * all along: a glyph on a hanging margin, a rule with a label on it, a framed
 * column. Pages compose them; pages no longer lay them out.
 */

/**
 * One line of an index: a glyph hanging in the margin, a title, a detail.
 *
 * The glyph sits in the margin rather than the text column so the list scans as
 * places rather than as four lines of serif that happen to differ at the end.
 */
@Composable
public fun NavRow(
    glyph: Glyph,
    label: String,
    detail: String,
    paper: Paper,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 19.sp,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .tap(label) { onClick() }
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(26.dp)) {
            Icon(glyph, paper, size = 16.dp, color = paper.inkSoft)
        }
        Title(label, paper, size = titleSize)
        Spacer(Modifier.weight(1f))
        Text(
            text = detail,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 11.sp,
                letterSpacing = Optical.bodyTracking,
            ),
            color = paper.inkFaint,
        )
    }
}

/**
 * A section break: air, a tick, an optional label, air.
 *
 * The rhythm of every long page is this object repeated. It used to be three
 * hand-placed spacers and a Tick per page, which is why the gaps never quite
 * matched between them.
 */
@Composable
public fun SectionBreak(
    label: String?,
    paper: Paper,
    modifier: Modifier = Modifier,
    top: Dp = 26.dp,
    bottom: Dp = 14.dp,
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(top))
        Tick(paper, width = 20.dp, color = paper.edgeStrong)
        if (label != null) {
            Spacer(Modifier.height(10.dp))
            Label(label, paper, color = paper.inkSoft, size = 9.sp)
        }
        Spacer(Modifier.height(bottom))
    }
}

/**
 * A framed column: the panel that holds a photo, a card, or a form.
 *
 * An edge and an inset, nothing else - no shadow, no fill beyond the raised
 * paper. The frame is a printing mark, not a card.
 */
@Composable
public fun Framed(
    paper: Paper,
    modifier: Modifier = Modifier,
    inset: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(paper.raised)
            .padding(inset),
        content = content,
    )
}

/** A row of labelled figures, spread edge to edge. */
@Composable
public fun FigureRow(
    paper: Paper,
    modifier: Modifier = Modifier,
    figures: List<Pair<String, String>>,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        figures.forEach { (label, value) ->
            Box(Modifier.weight(1f)) { RailFigure(label, value, paper) }
        }
    }
}

/**
 * A screen-wide tap that is not a button.
 *
 * Used for whole-row targets and the photo plate: things read first, tapped
 * second. There is no ripple here for exactly that reason - a ripple would tell
 * the user it is a control when the design says it is a page.
 */
@Composable
public fun TapSurface(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.tap(label) { onClick() }) {
        content()
    }
}

/** A block of air. Named so pages stop inventing their own spacer heights. */
@Composable
public fun Air(height: Dp) {
    Spacer(Modifier.height(height))
}

/** The standard inset for a page's text column. One value, used everywhere. */
public val PageInset: Dp = 22.dp
