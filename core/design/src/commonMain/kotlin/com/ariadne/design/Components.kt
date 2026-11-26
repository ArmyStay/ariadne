package com.ariadne.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
            // Rules are drawn lines: without this a screen reader announces nothing
            // where the eye sees a divider.
            .semantics { contentDescription = "Rule line" },
    )
}

/** A short rule that marks a section without spanning the page. */
@Composable
public fun Tick(paper: Paper, width: Dp = 28.dp, color: Color = paper.ink) {
    Box(
        Modifier
            .width(width)
            .height(2.dp)
            .background(color)
            .semantics { contentDescription = "Tick mark" },
    )
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
    lineHeight: TextUnit = size * 1.62f,
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
            letterSpacing = if (serif) Optical.headingTracking else Optical.bodyTracking,
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
    lineHeight: TextUnit = size * 1.04f,
    ) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = Bone.Serif,
            fontSize = size,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            // Optical: the bigger the serif, the tighter it must be tracked. One
            // tracking value for every size is what makes a headline look set by
            // a machine rather than by an editor.
            letterSpacing = if (size.value >= 36f) Optical.displayTracking else Optical.titleTracking,
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
            letterSpacing = if (size.value >= 34f) Optical.numeralTracking else (-0.2).sp,
        ),
        color = color,
        textAlign = align,
        maxLines = maxLines,
        softWrap = false,
        // Never ellipsised. A numeral is an amount, and "₹7,395.00 L…" is not an
        // amount — it is a number with its meaning cut off. The layout around a
        // numeral is what has to make room; when it cannot, drawing the figure
        // past its box is a visible mistake that gets fixed, where an ellipsis is
        // an invisible one that ships.
        overflow = TextOverflow.Visible,
        modifier = modifier,
    )
}

/**
 * The primary action: a blue bar with the type tracked out like a page footer.
 *
 * Square-cornered, full width, and it sits at the *edge* of its container rather
 * than floating in the middle of a padded card. Material's default is a 12dp
 * rounded filled button centred in a gutter; this is a ruled bar that belongs to
 * the page, which is why it reads as print rather than as a widget.
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
    /**
     * A glyph inside the button. The shutter is the product, and a word alone in
     * a blue block was doing the least interesting possible version of that.
     */
    icon: Glyph? = null,
) {
    // A blue action claims its surface's one blue. Disabled actions are drawn on
    // sunk paper, so they do not claim it: an inert bar is not the thing to do.
    if (accent && enabled) ClaimAccent(text)
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
            // A 2dp square radius: enough that it does not look like a broken
            // corner on a renderer without antialiasing, too little to read as a
            // card. The reference's blocks are rectangles.
            .background(bg, shape(2.dp))
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text, enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 20.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) {
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontFamily = Bone.Sans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                ),
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        } else {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, paper, size = 17.dp, color = fg)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text.uppercase(),
                    style = TextStyle(
                        fontFamily = Bone.Sans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    ),
                    color = fg,
                    maxLines = 1,
                )
            }
        }
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
            .border(Bone.Hairline, if (enabled) paper.edgeStrong else paper.edge, shape(2.dp))
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text, enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 20.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
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
    // The whole control is 44dp square-ish even though the mark is small: a quiet
    // action is quiet in weight, not in the area a thumb has to find, and at 200%
    // text the box grows with the label rather than clipping it.
    Box(
        modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .pointerInput(text) { detectTapGestures(onTap = { onClick() }) }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            // 11sp and a full-weight rule. At 10sp with a 1dp mark this read as a
            // caption rather than as something you can press, which is exactly why
            // the top of every page felt dead.
            Label(text, paper, color = c, size = 11.sp, tracking = 1.4.sp)
            if (underline) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(if (text.length > 8) 44.dp else 26.dp).height(1.5.dp).background(c))
            }
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
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { contentDescription = label }
                        .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
    ) {
        Label(label, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(7.dp))
        Text(
            text = value.ifEmpty { placeholder },
            style = TextStyle(
                fontFamily = Bone.Sans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = Optical.bodyTracking,
            ),
            color = if (value.isEmpty()) paper.inkFaint else paper.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(9.dp))
        // The rule *is* the field. A rounded 52dp box with an inset label is the
        // default shape every generated form uses; a labelled hairline is not.
        Rule(paper, color = if (emphasised) paper.edgeStrong else paper.edge)
    }
}

/**
 * A tag: "OPEN", "3 WAITING", a count.
 *
 * Square, not a capsule. Capsules are the single most over-used shape in
 * generated interfaces; a rule-bordered rectangle on paper is what a printed
 * index uses, and it sits beside hairlines instead of fighting them.
 */
@Composable
public fun Pill(
    text: String,
    paper: Paper,
    tone: PillTone = PillTone.Quiet,
) {
    val bg = when (tone) {
        PillTone.Quiet -> Color.Transparent
        PillTone.Blue -> paper.blueWash
        PillTone.Ink -> paper.ink
    }
    val edge = when (tone) {
        PillTone.Quiet -> paper.edgeStrong
        PillTone.Blue -> paper.blueEdge
        PillTone.Ink -> paper.ink
    }
    val fg = when (tone) {
        PillTone.Quiet -> paper.inkSoft
        PillTone.Blue -> paper.blue
        PillTone.Ink -> paper.onBlue
    }
    Box(
        Modifier
            .background(bg, shape(1.dp))
            .border(Bone.Hairline, edge, shape(1.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Label(text, paper, color = fg, size = 9.sp, tracking = 1.3.sp)
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
            .background(paper.raised, shape(2.dp))
            .border(Bone.Hairline, paper.edge, shape(2.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
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
            .background(paper.ink, shape(2.dp))
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
