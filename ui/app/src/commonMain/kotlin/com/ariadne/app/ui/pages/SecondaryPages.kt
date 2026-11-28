package com.ariadne.app.ui.pages

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.DataArchiveExporter
import com.ariadne.app.Route
import com.ariadne.design.Action
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.FlightSky
import com.ariadne.design.Glyph
import com.ariadne.design.Icon
import com.ariadne.design.Label
import com.ariadne.design.Leaf
import com.ariadne.design.Masthead
import com.ariadne.design.OutlineAction
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.PillButton
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.SectionHead
import com.ariadne.design.SectionMark
import com.ariadne.design.Title
import com.ariadne.design.tap
import com.ariadne.model.Category

public val LocalDataArchiveExporter = staticCompositionLocalOf<DataArchiveExporter?> { null }

@Composable
private fun MoreDestination(
    glyph: Glyph,
    title: String,
    detail: String,
    paper: Paper,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().tap("Open $title", onClick = onClick).padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(glyph, paper, size = 17.dp, color = paper.blue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Title(title, paper, size = 16.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(2.dp))
                Body(detail, paper, color = paper.inkSoft, size = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Icon(Glyph.Chevron, paper, size = 13.dp, color = paper.inkFaint)
        }
        Spacer(Modifier.height(7.dp))
        Rule(paper, color = paper.edge)
    }
}

@Composable
public fun MorePage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    Page(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
        rail = {
            Label("your copy", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
            Spacer(Modifier.height(10.dp))
            Label("on this device", paper, color = paper.inkFaint, size = 9.sp, tracking = 1.2.sp)
        },
        // The sky stays where it was drawn: arcs off the corner, a plane crossing
        // them, two sparks. It is the page's ground, not a picture that scrolls.
        // The sky keeps to the measure rather than the window. Anchored to a
        // 1080dp band — the widest this book ever prints — the arcs open from the
        // corner the reference opens from, on a phone and on a laptop alike; full
        // bleed, they would open from the monitor's corner and sweep through the
        // headline on their way across.
        backdrop = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                FlightSky(
                    paper,
                    Modifier
                        .widthIn(max = 1080.dp)
                        .fillMaxWidth()
                        .fillMaxSize(),
                )
            }
        },
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(bottom = 48.dp),
        ) {
            // No eyebrow and no plate number: this page is a door, not a chapter,
            // and the title does the work of both.
            Spacer(Modifier.height(if (wide) 14.dp else 12.dp))
            Title(
                "More",
                paper,
                size = if (wide) 56.sp else 42.sp,
                lineHeight = if (wide) 60.sp else 46.sp,
            )
            Spacer(Modifier.height(4.dp))
            Body(
                "Trips, settings, and your record.",
                paper,
                color = paper.inkSoft,
                size = if (wide) 14.sp else 13.sp,
                lineHeight = if (wide) 20.sp else 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Rule(paper, color = paper.edge)

            // ── the journeys ────────────────────────────────────────────────
            if (wide) {
                MoreColumns(state, model, paper)
            } else {
            Spacer(Modifier.height(26.dp))
            SectionMark("the journeys", paper)
            Spacer(Modifier.height(14.dp))
            MoreDestination(Glyph.Trip, "Trips", "${state.trips.size} on this device", paper) {
                model.go(Route.Trips)
            }
            Spacer(Modifier.height(10.dp))
            MoreDestination(Glyph.Budget, "Add manually", "An expense without a photograph", paper) {
                model.go(Route.ManualEntry)
            }

            // ── your copy ───────────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            SectionMark("your copy", paper)
            Spacer(Modifier.height(14.dp))
            MoreDestination(Glyph.Edit, "Settings & profile", "Name, currencies, appearance", paper) {
                model.go(Route.Preferences)
            }
            Spacer(Modifier.height(10.dp))
            MoreDestination(
                Glyph.Date,
                "Notifications",
                if (state.reminder.enabled) "Evening reminder · ${state.reminder.clock}" else "Evening reminder · off",
                paper,
            ) { model.openReminderSheet() }

            // ── about Ariadne ───────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            SectionMark("about Ariadne", paper)
            Spacer(Modifier.height(14.dp))
            MoreDestination(Glyph.Shield, "Privacy policy", "How this build handles your data", paper) {
                model.go(Route.PrivacyPolicy)
            }
            Spacer(Modifier.height(10.dp))
            MoreDestination(Glyph.Record, "Terms of use", "Draft for review", paper) {
                model.go(Route.Terms)
            }

            // ── the one ask ─────────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            FeedbackCard(state, model, paper, wide = false)
            }

            Spacer(Modifier.height(18.dp))
            Body(
                "Ariadne keeps your record here. No account or sync is part of this build.",
                paper,
                color = paper.inkFaint,
                size = 11.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}

/**
 * The one place the app asks for anything back.
 *
 * It asks for a *copy*, not a message: this build has no server and no address to
 * write to, so a button labelled "send" would be a lie with a spinner on it. What
 * it copies is the shape of a report — what were you doing, what happened — and the
 * line under the title says so before the button is pressed.
 */
@Composable
private fun MoreColumns(state: AppUiState, model: AppModel, paper: Paper) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            SectionMark("the journeys", paper)
            Spacer(Modifier.height(5.dp))
            MoreDestination(Glyph.Trip, "Trips", "${state.trips.size} on this device", paper) { model.go(Route.Trips) }
            MoreDestination(Glyph.Budget, "Add manually", "An expense without a photograph", paper) { model.go(Route.ManualEntry) }
            Spacer(Modifier.height(18.dp))
            SectionMark("your copy", paper)
            Spacer(Modifier.height(5.dp))
            MoreDestination(Glyph.Edit, "Settings & profile", "Name, currencies, appearance", paper) { model.go(Route.Preferences) }
            MoreDestination(
                Glyph.Date,
                "Notifications",
                if (state.reminder.enabled) "Evening reminder · ${state.reminder.clock}" else "Evening reminder · off",
                paper,
            ) { model.openReminderSheet() }
        }
        Column(Modifier.weight(1f)) {
            SectionMark("about Ariadne", paper)
            Spacer(Modifier.height(5.dp))
            MoreDestination(Glyph.Shield, "Privacy policy", "How this build handles your data", paper) { model.go(Route.PrivacyPolicy) }
            MoreDestination(Glyph.Record, "Terms of use", "Draft for review", paper) { model.go(Route.Terms) }
            Spacer(Modifier.height(18.dp))
            FeedbackCard(state, model, paper, wide = true)
        }
    }
}

@Composable
private fun FeedbackCard(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val label = if (state.feedbackCopied) "Copied" else "Share feedback"
    val detail = if (state.feedbackCopied) {
        "Copied. Paste it wherever you write to us."
    } else {
        "Copies a note you can paste into an email."
    }
    Leaf(
        paper = paper,
        tint = paper.blueWash,
        edge = paper.blueEdge.copy(alpha = 0.65f),
        inset = 15.dp,
    ) {
        if (wide) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeartMark(paper)
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(Bone.Hairline).height(34.dp).background(paper.blueEdge))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) { FeedbackWords(detail, paper) }
                Spacer(Modifier.width(12.dp))
                PillButton(label, paper) { model.copyFeedbackNote() }
            }
        } else {
            // The pill drops under the words on a phone: beside them it squeezed the
            // title into two lines and the sentence into three, on the one card whose
            // whole job is to be read before it is pressed.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                HeartMark(paper)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) { FeedbackWords(detail, paper) }
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton(label, paper) { model.copyFeedbackNote() }
            }
        }
    }
}

@Composable
private fun HeartMark(paper: Paper) {
    Box(
        Modifier
            .size(32.dp)
            .background(paper.raised, CircleShape)
            .border(Bone.Hairline, paper.blueEdge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Glyph.Heart, paper, size = 15.dp, color = paper.blue)
    }
}

@Composable
private fun FeedbackWords(detail: String, paper: Paper) {
    Title("Have feedback?", paper, size = 18.sp)
    Spacer(Modifier.height(3.dp))
    Body(detail, paper, color = paper.inkSoft, size = 12.sp, lineHeight = 17.sp)
}

@Composable
public fun ManualEntryPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    var amount by remember { mutableStateOf("") }
    var currency by remember(state.trip?.id) {
        mutableStateOf(state.trip?.baseCurrency?.code ?: "INR")
    }
    var date by remember(state.today) { mutableStateOf(state.today?.iso.orEmpty()) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Page(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) Bone.MarginWide else Bone.Margin, vertical = 22.dp),
        ) {
            Masthead(
                number = "",
                eyebrow = state.trip?.name ?: "Ariadne",
                title = "Add an expense",
                paper = paper,
                wide = wide,
                lede = "No photograph needed. This is saved as a confirmed entry.",
            )
            Spacer(Modifier.height(22.dp))
            if (state.trip == null) {
                Body("Create or open a trip before adding an expense.", paper, color = paper.inkSoft)
                Spacer(Modifier.height(18.dp))
                OutlineAction("Open trips", paper, { model.go(Route.Trips) })
                return@Column
            }
            if (state.trip.isClosed) {
                Body("This trip is closed. Open another trip to add an expense.", paper, color = paper.inkSoft)
                Spacer(Modifier.height(18.dp))
                OutlineAction("Open trips", paper, { model.go(Route.Trips) })
                return@Column
            }

            LineField(
                label = "amount",
                value = amount,
                placeholder = "0.00",
                paper = paper,
                onChange = { amount = it },
                serif = true,
            )
            CurrencyField(
                label = "currency",
                code = currency,
                paper = paper,
                onChoose = { currency = it },
            )
            LineField(
                label = "date · YYYY-MM-DD",
                value = date,
                placeholder = state.today?.iso ?: "YYYY-MM-DD",
                paper = paper,
                onChange = { date = it },
                serif = false,
            )
            LineField(
                label = "merchant or place · optional",
                value = merchant,
                placeholder = "Where it was",
                paper = paper,
                onChange = { merchant = it },
                serif = false,
            )
            Spacer(Modifier.height(16.dp))
            Label("category · optional", paper, color = paper.inkSoft)
            Spacer(Modifier.height(8.dp))
            CategoryChips(
                selected = category,
                paper = paper,
                onSelect = { category = if (category == it) null else it },
            )
            Spacer(Modifier.height(16.dp))
            LineField(
                label = "note · optional",
                value = note,
                placeholder = "Anything to remember",
                paper = paper,
                onChange = { note = it },
                serif = false,
            )
            error?.let {
                Spacer(Modifier.height(10.dp))
                Body(it, paper, color = paper.blue, size = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(22.dp))
            Action(
                if (state.busy) "Saving…" else "Save expense",
                paper,
                {
                    error = model.saveManualEntry(
                        amountText = amount,
                        currencyCode = currency,
                        dateText = date,
                        merchant = merchant,
                        category = category,
                        note = note,
                    )
                },
                accent = true,
                enabled = !state.busy,
            )
            Spacer(Modifier.height(12.dp))
            QuietAction("Cancel", paper, { model.back() }, color = paper.inkSoft)
            Spacer(Modifier.height(38.dp))
        }
    }
}

@Composable
public fun DataExportPage(state: AppUiState, model: AppModel, paper: Paper, wide: Boolean) {
    val dataExporter = LocalDataArchiveExporter.current
    Page(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = { model.toggleRail() },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) Bone.MarginWide else Bone.Margin, vertical = 22.dp),
        ) {
            Masthead(
                number = "",
                eyebrow = "Your copy",
                title = "Export data",
                paper = paper,
                wide = wide,
                lede = "One local ZIP with your trips, entries, rates, and receipt photos where this build stores them.",
            )
            Spacer(Modifier.height(18.dp))
            Body(
                "This ZIP is not encrypted and contains sensitive records and photos. A cloud-backed save location may upload it. Store and share it carefully. This build cannot restore an archive.",
                paper,
                color = paper.inkSoft,
                size = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(18.dp))
            if (dataExporter == null) {
                Body(
                    "This build does not provide a file-save surface, so it cannot create an archive here.",
                    paper,
                    color = paper.inkFaint,
                    size = 12.sp,
                    lineHeight = 18.sp,
                )
            } else {
                Action(
                    if (state.busy) "Preparing archive…" else "Create ZIP archive",
                    paper,
                    { model.exportData(dataExporter) },
                    accent = true,
                    enabled = !state.busy,
                )
            }
            Spacer(Modifier.height(14.dp))
            state.notice?.let {
                Body(it.text, paper, color = paper.inkSoft, size = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
public fun LegalPage(state: AppUiState, paper: Paper, wide: Boolean, terms: Boolean) {
    Page(
        paper = paper,
        wide = wide,
        railDocked = state.railDocked,
        onToggleRail = {},
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) Bone.MarginWide else Bone.Margin, vertical = 22.dp),
        ) {
            Masthead(
                number = "",
                eyebrow = if (terms) "Draft for review" else "Privacy",
                title = if (terms) "Terms of use" else "Privacy policy",
                paper = paper,
                wide = wide,
                lede = "In-app draft · not a substitute for reviewed legal terms.",
            )
            Spacer(Modifier.height(22.dp))
            if (terms) {
                LegalSection(
                    "Using Ariadne",
                    "Ariadne stores the trips and expenses you choose to enter. You decide what to record, confirm, edit, export, or delete.",
                    paper,
                )
                LegalSection(
                    "Figures and exchange rates",
                    "Text recognition is a suggestion, not a verified amount. Check each amount before relying on it. Exchange rates may be unavailable or out of date; the app shows the rate date where one is used.",
                    paper,
                )
                LegalSection(
                    "Your copy",
                    "This build keeps its records on the device and does not provide account sync. Export an archive if you want a separate copy. Deleting a trip removes its local entries and available photographs.",
                    paper,
                )
                LegalSection(
                    "Review required",
                    "These draft terms do not identify a legal operator, jurisdiction, support contact, warranty, or dispute process. They must be reviewed and completed before publication.",
                    paper,
                )
            } else {
                LegalSection("What stays here", state.storageDisclosure, paper)
                LegalSection("Network requests", state.rateDisclosure, paper)
                LegalSection("Voice input", "On Android, voice input opens another app's speech recognizer. It may send audio to its provider for processing. You can type instead.", paper)
                LegalSection(
                    "Notifications",
                    "The evening reminder is set, computed and delivered on this device. Turning it on asks your system for permission to post notifications; no account, device token or server is involved, and turning it off cancels the alarm.",
                    paper,
                )
                LegalSection("No account or sync", "Ariadne does not ask you to create an account or sync trips to an Ariadne server in this build. If you export or share an archive, that copy goes wherever you choose.", paper)
                LegalSection("Review required", "This is a factual in-app draft, not a complete legal policy. Add the responsible organization, contact details, and jurisdiction before publication.", paper)
            }
        }
    }
}

@Composable
private fun LegalSection(title: String, text: String, paper: Paper) {
    SectionHead("", title.lowercase(), paper)
    Body(text, paper, color = paper.inkSoft, size = 14.sp, lineHeight = 21.sp)
    Spacer(Modifier.height(20.dp))
}
