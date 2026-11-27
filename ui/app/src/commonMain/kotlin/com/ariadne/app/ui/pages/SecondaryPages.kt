package com.ariadne.app.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ariadne.design.Glyph
import com.ariadne.design.Label
import com.ariadne.design.Masthead
import com.ariadne.design.NavRow
import com.ariadne.design.OutlineAction
import com.ariadne.design.Page
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule
import com.ariadne.design.SectionHead
import com.ariadne.model.Category

public val LocalDataArchiveExporter = staticCompositionLocalOf<DataArchiveExporter?> { null }

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
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(bottom = 48.dp),
        ) {
            Masthead(
                number = "04",
                eyebrow = state.displayName.ifBlank { "Ariadne" },
                title = "More",
                paper = paper,
                wide = wide,
                lede = "Trips, settings, and the things you can do with your record.",
            )
            Spacer(Modifier.height(20.dp))
            SectionHead("I", "the journeys", paper)
            NavRow(Glyph.Trip, "Trips", "${state.trips.size} on this device", paper) {
                model.go(Route.Trips)
            }
            Rule(paper)
            NavRow(Glyph.Budget, "Add manually", "An expense without a photograph", paper) {
                model.go(Route.ManualEntry)
            }
            Rule(paper)
            Spacer(Modifier.height(18.dp))
            SectionHead("II", "your copy", paper)
            NavRow(Glyph.Edit, "Settings & profile", "Name, currencies, appearance", paper) {
                model.go(Route.Preferences)
            }
            Rule(paper)
            Spacer(Modifier.height(18.dp))
            SectionHead("III", "about Ariadne", paper)
            NavRow(Glyph.Record, "Privacy policy", "How this build handles your data", paper) {
                model.go(Route.PrivacyPolicy)
            }
            Rule(paper)
            NavRow(Glyph.Date, "Terms of use", "Draft for review", paper) {
                model.go(Route.Terms)
            }
            Rule(paper)
            Spacer(Modifier.height(22.dp))
            Body(
                "Ariadne keeps your record here. No account or sync is part of this build.",
                paper,
                color = paper.inkFaint,
                size = 12.sp,
                lineHeight = 19.sp,
            )
        }
    }
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
                "The archive includes JSON and CSV records. Photos stay on this device unless you choose where to save or share the ZIP.",
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
