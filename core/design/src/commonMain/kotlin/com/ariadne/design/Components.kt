package com.ariadne.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The component set, in the language of the reference.
 *
 * Six things: a rule, a label, two text styles, a numeral, and four kinds of
 * control (filled button, outlined field, quiet link, ink block). Every page is
 * built from those, which is why the app reads as one printed object rather than
 * a pile of widgets.
 *
 * The one rule worth stating: **blue appears once per surface**, on the single
 * action that finishes the page. If two things are blue, one of them is wrong.
 */

private fun shape(radius: Dp) = RoundedCornerShape(radius)

/** A hairline. */
@Composable
public fun Rule(
    paper: Paper,
    modifier: Modifier = Modifier,
    color: Color = paper.edge,
    thickness: Dp = Bone.Hairline,
) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

/** A short rule that marks a section without spanning the page. */
@Composable
public fun Tick(paper: Paper, width: Dp = 28.dp, color: Color = paper.ink) {
    Box(Modifier.width(width).height(2.dp).background(color))
}

/** Uppercase, tracked, 10px. Every field name, every tab, every eyebrow. */
@Composable
public fun Label(
    text: String,
    paper: Paper,
    color: Color = paper.inkSoft,
    modifier: Modifier = Modifier,
    size: TextUnit = Bone.label,
    tracking: TextUnit = Bone.labelTracking,
) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontFamily = Bone.Sans,
            fontSize = size,
            letterSpacing = tracking,
            fontWeight = FontWeight.Medium,
            lineHeight = size * 1.4f,
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Running text. */
@Composable
public fun Body(
    text: String,
    paper: Paper,
    color: Color = paper.inkMid,
    size: TextUnit = Bone.body,
    serif: Boolean = false,
    italic: Boolean = false,
    align: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = size * 1.55f,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = if (serif) Bone.Serif else Bone.Sans,
            fontSize = size,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        color = color,
        textAlign = align,
        modifier = modifier,
    )
}

/**
 * A headline. Instrument Serif, set large, tracked tight. This is the element
 * that carries every page — if a page has no headline it has no voice.
 */
@Composable
public fun Title(
    text: String,
    paper: Paper,
    color: Color = paper.ink,
    size: TextUnit = Bone.title,
    italic: Boolean = false,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = size * 1.08f,
) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = Bone.Serif,
            fontSize = size,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            letterSpacing = (-0.3).sp,
        ),
        color = color,
        textAlign = align,
        modifier = modifier,
    )
}

/** A tracked-out italic headline, used where a page wants to whisper grandly. */
@Composable
public fun Display(
    text: String,
    paper: Paper,
    color: Color = paper.ink,
    size: TextUnit = Bone.display,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Title(text, paper, color, size, italic = true, modifier = modifier, align = align)
}

/**
 * A number. Tabular figures so a column of amounts never shifts by a pixel when
 * one of them updates — the single most important detail in a ledger.
 */
@Composable
public fun Numeral(
    text: String,
    paper: Paper,
    color: Color = paper.ink,
    size: TextUnit = Bone.monoLarge,
    weight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = Bone.Mono,
            fontSize = size,
            lineHeight = size * 1.1f,
            fontWeight = weight,
            fontFeatureSettings = "tnum",
            letterSpacing = (-0.4).sp,
        ),
        color = color,
        textAlign = align,
        maxLines = maxLines,
        modifier = modifier,
    )
}

/**
 * The primary action: a filled blue block with white type. One per page.
 * Square-cornered enough to read as a printed rule, rounded enough to feel made.
 */
@Composable
public fun Action(
    text: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    fill: Boolean = true,
) {
    val bg = when {
        !enabled -> paper.sunk
        accent || fill -> paper.blue
        else -> paper.ink
    }
    val fg = when {
        !enabled -> paper.inkFaint
        else -> paper.onBlue
    }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = Bone.ButtonHeight)
            .background(bg, shape(Bone.Radius))
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text, enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            ),
            color = fg,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** A secondary action: an outlined block in ink. Same shape, no colour. */
@Composable
public fun OutlineAction(
    text: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = Bone.ButtonHeight)
            .border(Bone.Hairline, if (enabled) paper.edgeStrong else paper.edge, shape(Bone.Radius))
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text, enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
            ),
            color = if (enabled) paper.ink else paper.inkFaint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * A quiet action: small uppercase type with a hairline under it. This is how the
 * reference marks "back", "share", "skip" — present, never competing.
 */
@Composable
public fun QuietAction(
    text: String,
    paper: Paper,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    underline: Boolean = true,
) {
    val c = if (color == Color.Unspecified) paper.inkMid else color
    Column(
        modifier
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 6.dp),
    ) {
        Label(text, paper, color = c, size = 10.sp, tracking = 1.4.sp)
        if (underline) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(if (text.length > 8) 44.dp else 26.dp).height(1.dp).background(c))
        }
    }
}

/** A text-link in blue. Used for "Forgot password?"-shaped things. */
@Composable
public fun LinkAction(
    text: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            ),
            color = paper.blue,
        )
    }
}

/**
 * A field, as the reference draws one: 52dp tall, white on paper, hairline edge,
 * label inside. Not a Material text field — no floating label, no underline.
 */
@Composable
public fun Field(
    label: String,
    paper: Paper,
    value: String = "",
    placeholder: String = "",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = Bone.FieldHeight)
            .background(paper.raised, shape(Bone.Radius))
            .border(Bone.Hairline, if (emphasised) paper.edgeStrong else paper.edge, shape(Bone.Radius))
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { contentDescription = label }
                        .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = if (value.isEmpty()) paper.inkFaint else paper.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small pill: "OPEN", "WAITING", a count. Never coloured except in blue wash. */
@Composable
public fun Pill(
    text: String,
    paper: Paper,
    tone: PillTone = PillTone.Quiet,
) {
    val bg = when (tone) {
        PillTone.Quiet -> paper.sunk
        PillTone.Blue -> paper.blueWash
        PillTone.Ink -> paper.ink
    }
    val fg = when (tone) {
        PillTone.Quiet -> paper.inkSoft
        PillTone.Blue -> paper.blue
        PillTone.Ink -> paper.onBlue
    }
    Box(
        Modifier
            .background(bg, shape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Label(text, paper, color = fg, size = 9.sp, tracking = 1.2.sp)
    }
}

public enum class PillTone { Quiet, Blue, Ink }

/** A row of a matrix: a name on the left, a figure on the right, hairline under. */
@Composable
public fun FigureRow(
    label: String,
    figure: String,
    paper: Paper,
    emphasised: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = Bone.Sans,
                    fontSize = if (emphasised) 15.sp else 14.sp,
                    fontWeight = if (emphasised) FontWeight.Medium else FontWeight.Normal,
                ),
                color = if (emphasised) paper.ink else paper.inkMid,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Numeral(figure, paper, size = if (emphasised) 17.sp else 15.sp, weight = FontWeight.Medium)
        }
        Spacer(Modifier.height(13.dp))
        Rule(paper)
    }
}

/**
 * A raised panel. White on paper with a hairline and a generous radius — the
 * card the reference puts its sign-in fields inside.
 */
@Composable
public fun Panel(
    paper: Paper,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(paper.raised, shape(Bone.RadiusLarge))
            .border(Bone.Hairline, paper.edge, shape(Bone.RadiusLarge))
            .padding(20.dp),
    ) { content() }
}

/** An ink-filled square control, as the reference uses for "+". */
@Composable
public fun InkBlock(
    glyph: String,
    paper: Paper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Box(
        modifier
            .size(size)
            .background(paper.ink, shape(Bone.Radius))
            .semantics {
                contentDescription = glyph
                role = Role.Button
            }
            .pointerInput(glyph) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 26.sp,
            ),
            color = paper.onBlue,
        )
    }
}

/**
 * The page head: an eyebrow, a headline, and an optional trailing action, over a
 * hairline. Every page opens this way, which is what makes the app feel bound as
 * one book rather than a set of screens.
 */
@Composable
public fun PageHeader(
    eyebrow: String,
    title: String,
    paper: Paper,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    italicTitle: Boolean = false,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.weight(1f)) { leading() }
            } else {
                Label(eyebrow, paper, color = paper.inkSoft, modifier = Modifier.weight(1f))
            }
            if (trailing != null) trailing()
        }
        Spacer(Modifier.height(14.dp))
        Title(
            title,
            paper,
            size = Bone.display,
            italic = italicTitle,
            lineHeight = Bone.display * 1.02f,
        )
        Spacer(Modifier.height(16.dp))
        Rule(paper, color = paper.edgeStrong)
    }
}
