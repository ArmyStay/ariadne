package com.ariadne.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariadne.app.AppModel
import com.ariadne.app.AppUiState
import com.ariadne.app.Route
import com.ariadne.design.Body
import com.ariadne.design.Bone
import com.ariadne.design.Label
import com.ariadne.design.Numeral
import com.ariadne.design.PageHeader
import com.ariadne.design.Paper
import com.ariadne.design.QuietAction
import com.ariadne.design.Rule

/**
 * Page 9 — Settings.
 *
 * Four lines and a paragraph about privacy. If a setting is not here it does not
 * exist, which is the only way a settings page stays this short.
 */
@Composable
public fun SettingsPage(state: AppUiState, model: AppModel, paper: Paper) {
    val trip = state.trip
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Bone.Margin, vertical = 22.dp),
    ) {
        PageHeader(
            eyebrow = "Ariadne",
            title = "Settings",
            paper = paper,
            trailing = { QuietAction("Back", paper, { model.go(Route.Trips) }) },
        )

        Spacer(Modifier.height(24.dp))

        SettingLine("Base currency", trip?.baseCurrency?.code ?: "INR", paper)
        SettingLine("Home currency", trip?.homeCurrency?.code ?: "INR", paper)
        SettingLine("Date format", "2026-11-23", paper)
        SettingLine("Paper", "follows the system", paper)
        SettingLine("Reading", "on-device only", paper)

        Spacer(Modifier.height(30.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))

        Label("What leaves this device", paper, color = paper.inkFaint)
        Spacer(Modifier.height(12.dp))
        Body(
            "Nothing. There is no account to sign into and no server to sign into it from. " +
                "Photographs are stored in the app's private folder, the record lives in a local " +
                "database, and the only network call in the whole product is an optional one you " +
                "would have to configure yourself.",
            paper,
            serif = true,
            color = paper.inkMid,
            size = 15.sp,
        )

        Spacer(Modifier.height(28.dp))
        Rule(paper)
        Spacer(Modifier.height(20.dp))
        QuietAction("Delete this trip", paper, { }, color = paper.inkMid)
        Spacer(Modifier.height(6.dp))
        Body(
            "Removes its photographs and its record. There is no copy anywhere else, which is " +
                "either the point or the risk, depending on the trip.",
            paper,
            color = paper.inkFaint,
            size = 12.sp,
        )
    }
}

@Composable
private fun SettingLine(label: String, value: String, paper: Paper) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(label, paper, color = paper.inkSoft)
        Numeral(value, paper, size = 13.sp, color = paper.inkMid)
    }
    Rule(paper, color = paper.edge.copy(alpha = 0.7f))
}
