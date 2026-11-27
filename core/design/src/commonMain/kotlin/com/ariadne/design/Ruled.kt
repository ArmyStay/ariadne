package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The two ruled objects that more than one page needs.
 *
 * Both used to be private copies: the keypad was typed twice (Nightfall and the
 * photograph detail) and the measured rule once. A copy of a control is how a
 * product acquires two slightly different heads, so they live here now, next to
 * the rules and numerals they are drawn from.
 */

/**
 * The keypad, ruled.
 *
 * A till slip rules its columns; it does not draw a calculator. The grid is
 * hairlines with no fills, the digits are tabular, and the only ink is the type.
 * Rounded keys with white fills would be the most generic object on the most
 * important page in the app.
 *
 * Rows carry a **minimum** height rather than a fixed one, so a person running a
 * large system font grows the keypad instead of clipping the digits.
 */
@Composable
public fun RuledKeypad(
    paper: Paper,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 50.dp,
    digitSize: TextUnit = 21.sp,
) {
    val rows = listOf("123", "456", "789", "0.")
    Column(modifier.fillMaxWidth()) {
        Rule(paper)
        rows.forEachIndexed { r, row ->
            if (r > 0) Rule(paper)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { c, digit ->
                    if (c > 0) KeySeparator(paper)
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = rowHeight)
                            .tap(if (digit == '.') "Type the point" else "Type $digit") { onDigit(digit) }
                            .semantics { contentDescription = if (digit == '.') "point" else digit.toString() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Numeral(digit.toString(), paper, size = digitSize, weight = FontWeight.Medium)
                    }
                }
                if (r == rows.lastIndex) {
                    KeySeparator(paper)
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = rowHeight)
                            .tap("Delete a digit") { onBackspace() }
                            .semantics { contentDescription = "Backspace" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Glyph.Backspace, paper, size = 18.dp) }
                }
            }
        }
    }
}

@Composable
private fun KeySeparator(paper: Paper) {
    Box(Modifier.width(Bone.Hairline).fillMaxHeight().background(paper.edge))
}

/**
 * A measured rule: the done portion drawn solid, the rest as a ruler.
 *
 * Used for a trip's progress — “3 of 7 days closed” — where the point is that the
 * quantity is *measured*, not scored. It has no colour of its own, no fill, no
 * rounded caps: a printer's ruler.
 */
@Composable
public fun MeasuredRule(
    done: Int,
    total: Int,
    paper: Paper,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val fraction = if (total == 0) 0f else done.toFloat() / total.toFloat()
        drawLine(
            color = paper.ink,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width * fraction, size.height / 2),
            strokeWidth = 3f,
        )
        drawLine(
            color = paper.edge,
            start = Offset(size.width * fraction, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1f,
        )
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = paper.edge,
                start = Offset(x, size.height / 2 - 3f),
                end = Offset(x, size.height / 2 + 3f),
                strokeWidth = 1f,
            )
            x += 24f
        }
    }
}
