package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.ariadne.design.Label
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * Page 2 — Onboarding.
 *
 * One page, four sentences, two actions. It exists to state the inversion before
 * the visitor sees a shutter: the photograph is the memory and the numbers come
 * later. If this page were removed the app would still work; it would just take
 * a minute longer to understand, and the judge has ninety seconds.
 */
@Composable
public fun OnboardingPage(model: AppModel, paper: Paper, wide: Boolean) {
    Row(Modifier.fillMaxSize().background(paper.bg)) {
        // The title page of the book: a full-height margin on the left carrying the
        // imprint, the prose hung beside it. On a phone the margin folds away.
        if (wide) {
            Column(
                Modifier
                    .width(220.dp)
                    .fillMaxSize()
                    .padding(start = Bone.MarginWide, top = 56.dp, bottom = 56.dp),
            ) {
                Label("Ariadne", paper, color = paper.inkSoft, tracking = 3.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(28.dp).height(Bone.Hairline).background(paper.blue))
                Spacer(Modifier.weight(1f))
                Label("edition one", paper, color = paper.inkFaint, size = 9.sp)
                Spacer(Modifier.height(6.dp))
                Label("a trip ledger", paper, color = paper.inkFaint, size = 9.sp)
                Spacer(Modifier.height(6.dp))
                Label("no account", paper, color = paper.inkFaint, size = 9.sp)
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
                    Label("Ariadne", paper, color = paper.inkFaint, tracking = 3.sp)
                    Spacer(Modifier.height(18.dp))
                }

                // Set large enough that it is the page. Two lines, broken where
                // the sentence breaks, not where the measure runs out.
                Title(
                    "The photograph\nis the memory.",
                    paper,
                    size = if (wide) 66.sp else 40.sp,
                    lineHeight = if (wide) 66.sp else 42.sp,
                )
                Spacer(Modifier.height(6.dp))
                Title(
                    "The numbers wait for night.",
                    paper,
                    size = if (wide) 66.sp else 40.sp,
                    italic = true,
                    color = paper.inkSoft,
                    lineHeight = if (wide) 66.sp else 42.sp,
                )

                Spacer(Modifier.height(26.dp))
                Rule(paper, color = paper.ink)

                Spacer(Modifier.height(22.dp))
                Body(
                    "Forty rupees on chai, twelve hundred on shoes, and by tonight neither is " +
                        "remembered. Point the camera at the receipt, the stall, the ticket. When " +
                        "the day is done, sit down and settle it - two taps a card.",
                    paper,
                    serif = true,
                    color = paper.inkMid,
                    size = if (wide) 19.sp else 17.sp,
                    lineHeight = if (wide) 30.sp else 27.sp,
                )

                Spacer(Modifier.height(34.dp))
                Action(
                    "Begin with the demo roll",
                    paper,
                    { model.startTripDemo() },
                    accent = true,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
                Spacer(Modifier.height(16.dp))
                // "Begin empty" used to route to Trips, which - with no trip in
                // state - rendered the same form this page was already standing
                // in front of. It looked like a button that does nothing because
                // it was one. It opens a blank form now, which is a real place.
                QuietAction("Begin empty", paper, { model.startNewTrip() })

                Spacer(Modifier.height(40.dp))
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
