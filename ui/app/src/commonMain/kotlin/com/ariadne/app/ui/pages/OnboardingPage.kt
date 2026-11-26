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
 * The first page welcomes a new traveller and makes the next step explicit.
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

                Title(
                    "Welcome to\nAriadne",
                    paper,
                    size = if (wide) 64.sp else 42.sp,
                    lineHeight = if (wide) 68.sp else 46.sp,
                )

                Spacer(Modifier.height(26.dp))
                Rule(paper, color = paper.ink)

                Spacer(Modifier.height(22.dp))
                Body(
                    "Keep the moments from a trip without turning them into a chore. Take a " +
                        "photograph now, add the amount later, and keep the whole record on this device.",
                    paper,
                    serif = true,
                    color = paper.inkMid,
                    size = if (wide) 19.sp else 17.sp,
                    lineHeight = if (wide) 30.sp else 27.sp,
                )

                Spacer(Modifier.height(34.dp))
                Action(
                    "Create a trip",
                    paper,
                    { model.startNewTrip() },
                    accent = true,
                    modifier = Modifier.widthIn(max = 360.dp),
                )

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
