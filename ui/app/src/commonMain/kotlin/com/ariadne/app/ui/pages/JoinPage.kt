package com.ariadne.app.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Masthead
import com.ariadne.design.Optical
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.Title

/**
 * 01c — Joining a trip somebody else planned.
 *
 * The other half of the first instalment. One person types the plan once and reads the
 * code out; the other types it back here and has the same trip — the same dates, the
 * same currency, the same budget — with no account, no invitation and nothing that
 * arrives later.
 *
 * The page reads the code as it is typed and says what it says *before* the trip is
 * created: a wrong character should be a sentence on this page, not a trip in the
 * register with the wrong dates on it.
 *
 * The trip's name is not in the code, so it is asked for here. That is the one thing
 * about a joined trip that is yours rather than theirs.
 */
@Composable
public fun JoinPage(
    state: AppUiState,
    model: AppModel,
    paper: Paper,
    wide: Boolean,
) {
    val plan = state.joinPlan
    Page(paper = paper, wide = wide) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Masthead(
                number = "01c",
                eyebrow = "a trip, shared",
                title = "Join a trip",
                paper = paper,
                italic = true,
                wide = wide,
                lede = "Type the code they read out. Sixteen characters, in groups of four.",
            )

            Spacer(Modifier.height(26.dp))

            CodeField(
                label = "the code",
                placeholder = "AR1-K7QM-2X9B-4HDR-8TQP",
                value = state.joinCode,
                paper = paper,
                onChange = model::editJoinCode,
            )

            // What the code turns out to be, read back before anything is created.
            when {
                state.joinError != null -> {
                    Body(
                        state.joinError ?: "",
                        paper,
                        serif = true,
                        color = paper.inkSoft,
                        size = 17.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                }

                plan != null -> {
                    Label("this is the plan", paper, color = paper.blue, size = 9.sp, tracking = 1.6.sp)
                    Spacer(Modifier.height(8.dp))
                    Title(
                        "${plan.start.iso} to ${plan.end.iso}",
                        paper,
                        size = if (wide) 26.sp else 21.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Body(
                        buildString {
                            append(plan.baseCurrency.code)
                            plan.budgetMinor?.let { budget ->
                                append(" · budget ")
                                append(com.ariadne.model.Money.ofMinor(budget).format(plan.baseCurrency))
                            }
                        },
                        paper,
                        serif = true,
                        color = paper.inkMid,
                        size = 16.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            // The name is asked only once the code reads: a name asked for beside a
            // rejected code is a page that looks like it did something it did not.
            if (plan != null) {
                CodeField(
                    label = "what to call it here",
                    placeholder = "the shared trip",
                    value = state.joinName,
                    paper = paper,
                    onChange = model::editJoinName,
                )
                Action(
                    "Join this trip",
                    paper,
                    { model.joinTrip() },
                    accent = true,
                )
                Spacer(Modifier.height(14.dp))
            }

            Body(
                "A code is a copy, not a link. The plan lands on this phone and stays " +
                    "yours — nothing here updates when theirs does.",
                paper,
                serif = true,
                color = paper.inkSoft,
                size = 15.sp,
            )

            Spacer(Modifier.height(20.dp))
            QuietAction("Not now", paper, { model.back() })
        }
    }
}

/**
 * A labelled hairline for a code.
 *
 * Typed in capitals with no autocorrection: a code is not a word, and a keyboard that
 * capitalises the first letter and "fixes" the rest is a keyboard that breaks it. The
 * field is wide-tracked so the groups of four are countable by eye.
 */
@Composable
private fun CodeField(
    label: String,
    placeholder: String,
    value: String,
    paper: Paper,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Label(label, paper, color = paper.inkFaint, size = 9.sp, tracking = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Title(placeholder, paper, size = 19.sp, color = paper.inkFaint)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                textStyle = TextStyle(
                    fontFamily = Bone.Sans,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = Optical.bodyTracking,
                    color = paper.ink,
                ),
                cursorBrush = SolidColor(paper.blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
            )
        }
        Spacer(Modifier.height(9.dp))
        Rule(paper)
        Spacer(Modifier.height(18.dp))
    }
}
