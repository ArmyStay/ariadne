package com.ariadne.app.ui.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.ariadne.app.AppUiState
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.OutlineAction
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * 04b — The frame just taken, held up.
 *
 * The shutter used to fire and throw the person straight back into the app, which
 * meant the photograph disappeared into a queue they could not see. Now the frame
 * stops the page and asks the only question worth asking at that moment: is this
 * everything, or is there more to it?
 *
 * Two ways out, and neither of them loses the photograph:
 *   - **Add the amount later** — nothing to type, it waits with the rest.
 *   - **Open it now** — amount, what it was, what it was for.
 *
 * The photograph is the largest thing on the page. Always.
 */
@Composable
public fun FramePage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
    captureId: String,
) {
    val capture = state.dayCapturesAll.values.flatten().firstOrNull { it.id == captureId }
        ?: state.detail?.takeIf { it.id == captureId }
        ?: state.stack.firstOrNull { it.id == captureId }

    Page(paper = paper, wide = wide) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label("taken", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
            Spacer(Modifier.weight(1f))
            QuietAction("Home", paper, { model.go(com.ariadne.app.Route.Home) })
        }
        Spacer(Modifier.height(10.dp))
        Rule(paper)

        Spacer(Modifier.height(18.dp))

        // The plate. Square, full measure, hairline frame on every width: the same
        // frame it had in the viewfinder, so the shutter and the result are visibly
        // the same object — including on a wide page, which used to stretch it.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(Bone.Hairline, paper.edge),
        ) {
            PhotoPreview(
                photoPath = capture?.photoPath.orEmpty(),
                caption = capture?.let { model.titleOf(it) } ?: "just taken",
                paper = paper,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(16.dp))

        Title(
            capture?.let { model.titleOf(it) } ?: "Kept",
            paper,
            size = if (wide) 30.sp else 26.sp,
            italic = true,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label(
                capture?.localDate?.iso ?: state.today?.iso.orEmpty(),
                paper,
                color = paper.inkFaint,
                size = 9.sp,
                tracking = 1.4.sp,
            )
            Spacer(Modifier.weight(1f))
            if (capture?.hasAmount == true) {
                Numeral(
                    capture.amount()?.formatWithSymbol(capture.currency ?: state.trip?.baseCurrency ?: com.ariadne.model.CurrencyCode.of("INR"))
                        ?: "",
                    paper,
                    size = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Rule(paper, color = paper.ink)
        Spacer(Modifier.height(18.dp))

        // The sentence that used to sit here explained the whole product for the
        // fourth time. A glyph and five words carry the same fact.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Done, paper, size = 13.dp, color = paper.inkFaint)
            Spacer(Modifier.width(8.dp))
            Label("nothing here is required", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.3.sp)
        }

        Spacer(Modifier.height(22.dp))

        // The two answers, one under the other, so neither is hidden behind a swipe.
        Action("Fill it in now", paper, { model.openFrame(captureId) }, accent = true)
        Spacer(Modifier.height(12.dp))
        OutlineAction("Add the amount later", paper, { model.keepFrameForLater() })

        Spacer(Modifier.height(18.dp))
        Label(
            "${state.tripPending.size} waiting to be finished",
            paper,
            color = paper.inkFaint,
            size = 9.sp,
            tracking = 1.4.sp,
        )

        Spacer(Modifier.height(24.dp))
        Rule(paper, color = paper.edge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuietAction("Take another", paper, { model.openCapture() })
            QuietAction("Close day", paper, { model.openCloseDay(state.today) })
        }
        Spacer(Modifier.height(20.dp))
    }
}
