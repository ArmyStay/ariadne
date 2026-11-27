package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * Page 2 — Onboarding: the first instalment.
 *
 * This is the only page in the app that has to explain itself, so it is set the way a
 * book explains itself on its first spread: the imprint in the margin, one line that
 * says what the object is, three numbered beats that say how it works, and a single
 * blue thing to press. No artwork, no carousel, no tour with arrows — a person reading
 * this has one question ("what is this?") and the page answers it in twelve seconds.
 *
 * The three beats are the product, in order, and they are deliberately not features:
 * **the shutter** (one photograph, no form), **close day** (the figures wait, and one
 * may be left out), **the record** (the day, the trip, the card — on this device).
 * That is the whole loop, and it is the same loop on all five targets.
 *
 * A sample trip sits under the primary action as a quiet line rather than beside it as
 * a second button: the first instalment's one instruction is "begin a trip", and a
 * judge or a curious visitor can look at a finished one without being asked to choose
 * between two equally loud doors.
 */
@Composable
public fun OnboardingPage(model: AppModel, paper: Paper, wide: Boolean) {
    Row(Modifier.fillMaxSize().background(paper.bg)) {
        // The title page's margin: the imprint, and the two facts that matter most
        // about the object before you have read anything else.
        if (wide) {
            Column(
                Modifier
                    .width(220.dp)
                    .fillMaxSize()
                    .padding(start = Bone.MarginWide, top = 56.dp, bottom = 56.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Glyph.Shutter, paper, size = 16.dp, color = paper.ink)
                    Spacer(Modifier.width(9.dp))
                    Label("Ariadne", paper, color = paper.inkSoft, tracking = 3.sp)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(28.dp).height(Bone.Hairline).background(paper.blue))
                Spacer(Modifier.weight(1f))
                Label("edition one", paper, color = paper.inkFaint, size = 9.sp)
                Spacer(Modifier.height(6.dp))
                Label("no account", paper, color = paper.inkFaint, size = 9.sp)
                Spacer(Modifier.height(6.dp))
                Label("no server", paper, color = paper.inkFaint, size = 9.sp)
                Spacer(Modifier.height(30.dp))
            }
        }

        Box(
            Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                Modifier
                    .then(if (wide) Modifier.width(560.dp) else Modifier.fillMaxWidth())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wide) 56.dp else Bone.Margin, vertical = 40.dp),
            ) {
                if (!wide) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Glyph.Shutter, paper, size = 15.dp, color = paper.ink)
                        Spacer(Modifier.width(9.dp))
                        Label("Ariadne", paper, color = paper.inkFaint, tracking = 3.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Label(
                    "01 — the first instalment",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.8.sp,
                )
                Spacer(Modifier.height(14.dp))

                Title(
                    "A trip, kept\nin photographs.",
                    paper,
                    size = if (wide) 64.sp else 42.sp,
                    italic = true,
                    lineHeight = if (wide) 66.sp else 45.sp,
                )

                Spacer(Modifier.height(22.dp))
                Rule(paper, color = paper.ink)
                Spacer(Modifier.height(18.dp))

                Body(
                    "One press of the shutter in the shop; the figures wait until you close " +
                        "the day. Everything stays on this device.",
                    paper,
                    serif = true,
                    color = paper.inkMid,
                    size = if (wide) 19.sp else 16.sp,
                    lineHeight = if (wide) 29.sp else 25.sp,
                )

                Spacer(Modifier.height(30.dp))

                Beat(
                    number = "01",
                    name = "the shutter",
                    line = "One photograph at the counter, under two seconds. No form, no fields, " +
                        "nothing to type while someone waits for change.",
                    paper = paper,
                    wide = wide,
                )
                Beat(
                    number = "02",
                    name = "close day",
                    line = "In the evening the day's photographs arrive as cards. Confirm a " +
                        "figure, or keep the card without one — no amount is a legal entry.",
                    paper = paper,
                    wide = wide,
                )
                Beat(
                    number = "03",
                    name = "the record",
                    line = "The day, the trip, and a card you keep at the end. No account, no " +
                        "server: nothing leaves the phone but an optional rate lookup.",
                    paper = paper,
                    wide = wide,
                )

                Spacer(Modifier.height(6.dp))
                Rule(paper, color = paper.edge)
                Spacer(Modifier.height(24.dp))

                Action(
                    "Begin a trip",
                    paper,
                    { model.startNewTrip() },
                    accent = true,
                    icon = Glyph.Trip,
                    modifier = Modifier.widthIn(max = 360.dp),
                )

                Spacer(Modifier.height(6.dp))
                QuietAction(
                    "Or look at a sample trip",
                    paper,
                    { model.startTripDemo() },
                    color = paper.inkSoft,
                )

                // The other door. Somebody on this trip typed the plan once and read
                // six groups of symbols out; this is where they are typed back. It
                // sits with the sample trip rather than beside the primary action,
                // because the first instalment's one instruction is still "begin".
                QuietAction(
                    "Somebody gave you a code?",
                    paper,
                    { model.openJoinTrip() },
                    color = paper.inkSoft,
                )

                Spacer(Modifier.height(30.dp))
                Label(
                    "No account · no server · no sync",
                    paper,
                    color = paper.inkFaint,
                    size = 9.sp,
                    tracking = 1.6.sp,
                )
            }
        }
    }
}

/**
 * One beat of the loop: a number, a rule, the name of the step in tracked capitals,
 * and one sentence under it in the serif. Three of these are the entire product.
 */
@Composable
private fun Beat(number: String, name: String, line: String, paper: Paper, wide: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label(number, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.4.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(18.dp).height(1.dp).background(paper.blueEdge))
        Spacer(Modifier.width(10.dp))
        Label(name, paper, color = paper.ink, size = 9.sp, tracking = 1.8.sp)
    }
    Spacer(Modifier.height(8.dp))
    Body(
        line,
        paper,
        serif = true,
        color = paper.inkMid,
        size = if (wide) 16.sp else 15.sp,
        lineHeight = if (wide) 25.sp else 23.sp,
    )
    Spacer(Modifier.height(22.dp))
}
