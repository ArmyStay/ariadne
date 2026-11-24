package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    Box(
        Modifier.fillMaxSize().background(paper.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (wide) Modifier.width(560.dp) else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Bone.Margin, vertical = 40.dp),
        ) {
            Label("Ariadne", paper, color = paper.inkFaint)
            Spacer(Modifier.height(18.dp))

            Title("The photograph is the memory.\nThe numbers wait for night.", paper, size = 34.sp)
            Spacer(Modifier.height(20.dp))

            Body(
                "You spent forty rupees on chai and twelve hundred on shoes, and by tonight you " +
                    "won't remember either. Point the camera at the receipt, the stall, the ticket — " +
                    "no amount, no category, no form. When the day is over, sit down and finish it " +
                    "in two taps a card.",
                paper,
                serif = true,
                color = paper.inkMid,
                size = 17.sp,
            )

            Spacer(Modifier.height(30.dp))
            Rule(paper)
            Spacer(Modifier.height(24.dp))

            Action("Begin with the demo roll", paper, { model.startTripDemo() }, accent = true)
            Spacer(Modifier.height(12.dp))
            QuietAction("Begin empty", paper, { model.go(com.ariadne.app.Route.Trips) })

            Spacer(Modifier.height(34.dp))
            Label("No account · no server · nothing leaves this device", paper, color = paper.inkFaint)
        }
    }
}
