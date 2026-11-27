package com.ariadne.app.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title
import com.ariadne.model.CurrencyCode
import com.ariadne.model.Money
import com.ariadne.model.SpokenAmount
import com.ariadne.model.SpokenExpense
import com.ariadne.app.ui.tap
import com.ariadne.design.tap

/**
 * The microphone, drawn.
 *
 * A capsule, the cradle it sits in, a stem and the line it stands on — the shape
 * everyone already knows, at one weight, in ink. Nothing animates: a mark that moves
 * on its own is a mark you stop trusting.
 */
@Composable
internal fun MicMark(color: Color, extent: Dp = 16.dp) {
    Canvas(Modifier.size(extent)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        // The capsule.
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.07f),
            size = Size(w * 0.28f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(stroke),
        )
        // The cradle, from three o'clock round the bottom to nine.
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.23f, h * 0.32f),
            size = Size(w * 0.54f, h * 0.42f),
            style = Stroke(stroke),
        )
        // The stem and the line it stands on.
        drawLine(color, Offset(w * 0.5f, h * 0.74f), Offset(w * 0.5f, h * 0.89f), stroke)
        drawLine(color, Offset(w * 0.33f, h * 0.89f), Offset(w * 0.67f, h * 0.89f), stroke)
    }
}

/**
 * The microphone as a round button.
 *
 * A disc with a hairline rim, the same weight as every other round mark in the app.
 * While it is listening the disc takes the wash and the mark takes the blue, so the
 * only thing that changes on screen is the thing that is happening.
 */
@Composable
internal fun MicDisc(
    paper: Paper,
    listening: Boolean,
    description: String,
    extent: Dp = 52.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(extent)
            .clip(CircleShape)
            .background(if (listening) paper.blueWash else paper.raised)
            .border(Bone.Hairline, if (listening) paper.blue.copy(alpha = 0.55f) else paper.edgeStrong, CircleShape)
            .semantics { contentDescription = description }
            .tap("Say the amount", enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        MicMark(
            color = when {
                !enabled -> paper.inkGhost
                listening -> paper.blue
                else -> paper.inkMid
            },
            extent = extent * 0.36f,
        )
    }
}

/**
 * The microphone as one line of the sheet: a whisper, not a third button.
 *
 * The kept photograph offers two actions and then this, quieter than both, because
 * saying the amount is optional — the entry can wait for Close Day.
 */
@Composable
internal fun MicAction(
    paper: Paper,
    listening: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(if (listening) paper.blueWash else Color.Transparent)
            .border(
                Bone.Hairline,
                if (listening) paper.blue.copy(alpha = 0.45f) else paper.edgeStrong,
                shape,
            )
            .semantics {
                contentDescription = when {
                    !enabled -> "Say the amount, not available in this build"
                    listening -> "Listening for the amount"
                    else -> "Say the amount"
                }
            }
            .tap("Say the amount", enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = when {
            !enabled -> paper.inkGhost
            listening -> paper.blue
            else -> paper.inkMid
        }
        MicMark(tint, 16.dp)
        Spacer(Modifier.width(10.dp))
        Body(
            when {
                !enabled -> "No microphone in this build"
                listening -> "Listening…"
                else -> "Say the amount"
            },
            paper,
            color = tint,
            size = 13.sp,
        )
    }
}

/**
 * The home page's microphone.
 *
 * A sheet over the dashboard with one question and no keyboard: say what you spent and
 * the amount, with the shop if you named one, is filed against today. A sentence with
 * no figure in it is not a figure — the sheet says what it heard and offers to try
 * again, and the way to the keypad is one quiet line away.
 */
@Composable
internal fun HomeVoicePanel(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    onClose: () -> Unit,
) {
    val voice = LocalVoiceEntry.current
    var listening by remember { mutableStateOf(false) }
    var heard by remember { mutableStateOf<String?>(null) }
    var trouble by remember { mutableStateOf<String?>(null) }

    val currency = CurrencyCode.of(state.dayCurrency)

    fun listen() {
        val entry = voice
        if (entry == null) {
            trouble = "This build has no recogniser. Type the expense instead."
            return
        }
        trouble = null
        heard = null
        listening = true
        entry.listen { text ->
            listening = false
            if (text == null || text.isBlank()) {
                trouble = "Nothing was heard. Try again, or type it."
            } else {
                heard = text
            }
        }
    }

    // Speaking is a decision, so a sentence that parses is filed at once: the point of
    // the microphone is that the thumb never touches the keyboard.
    LaunchedEffect(heard) {
        val text = heard ?: return@LaunchedEffect
        val spoken = SpokenExpense.parse(text, state.trip?.baseCurrency)
        if (spoken == null) {
            trouble = "No amount in that. Try again, or type it."
            return@LaunchedEffect
        }
        val today = state.today
        if (today == null) {
            trouble = "There is no day to file this against."
            return@LaunchedEffect
        }
        val code = spoken.currency ?: currency
        val error = model.saveManualEntry(
            amountText = Money.ofMinor(spoken.minor).format(code, grouping = false),
            currencyCode = code.code,
            dateText = today.iso,
            merchant = spoken.remainder,
            category = null,
            note = "",
        )
        if (error != null) trouble = error else onClose()
    }

    // The sheet opens listening. Tapping a microphone and then having to tap a second
    // time to start is the kind of politeness that makes a feature feel broken.
    LaunchedEffect(Unit) { listen() }

    val said: SpokenAmount? = heard?.let { SpokenExpense.parse(it, state.trip?.baseCurrency) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(paper.scrim.copy(alpha = 0.45f))
                .semantics { contentDescription = "Close the microphone" }
                .tap("Close the microphone") { onClose() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(paper.raised)
                .border(Bone.Hairline, paper.edge, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Label("voice", paper, color = paper.inkGhost, size = 8.sp, tracking = 1.4.sp)
            Spacer(Modifier.height(10.dp))
            Title(
                when {
                    listening -> "Listening…"
                    said != null -> spentLine(said, currency)
                    else -> "What did you spend?"
                },
                paper,
                size = 23.sp,
                lineHeight = 29.sp,
            )
            Spacer(Modifier.height(8.dp))
            Body(
                when {
                    listening -> "Say the amount, and the shop if you like."
                    heard != null -> "“${heard}”"
                    else -> "Say it the way you would to a friend — “coffee, two fifty”."
                },
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
                lineHeight = 21.sp,
            )
            trouble?.let { line ->
                Spacer(Modifier.height(10.dp))
                Rule(paper, color = paper.edgeStrong)
                Spacer(Modifier.height(10.dp))
                Body(line, paper, serif = true, color = paper.inkSoft, size = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicDisc(
                    paper = paper,
                    listening = listening,
                    description = if (listening) "Listening" else "Say it again",
                    extent = 46.dp,
                    enabled = voice != null && !listening,
                ) { listen() }
                Spacer(Modifier.width(14.dp))
                Body(
                    when {
                        voice == null -> "No recogniser here."
                        listening -> "Go ahead."
                        else -> "Say it again"
                    },
                    paper,
                    color = paper.inkMid,
                    size = 13.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            QuietAction(
                "Type it instead",
                paper,
                {
                    onClose()
                    model.go(Route.ManualEntry)
                },
                color = paper.inkSoft,
            )
        }
    }
}

/** `₹397.00 · coffee`, or just the figure when no shop was named. */
private fun spentLine(spoken: SpokenAmount, fallback: CurrencyCode): String {
    val figure = Money.ofMinor(spoken.minor)
        .formatWithSymbol(spoken.currency ?: fallback)
    val shop = spoken.remainder.trim()
    return if (shop.isEmpty()) figure else "$figure · $shop"
}
