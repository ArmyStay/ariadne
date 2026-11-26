# Ariadne — redesign audit (Phase 0)

**Date:** 2026-11-26 · **Branch:** `arena/0728b09d-ariadne` · **Base:** `24b2ea5` (main, after PR #6)

No product code was changed in this phase. This document is the audit the plan is
built on: what the app is actually made of, every surface and where it is reached
from, the six reported bugs traced to a line of code, the gap to the target flow, and
the decisions I need from you before Phase 1.

---

## 0. How this was checked, and what this environment can and cannot do

Everything below was read out of the repository, not from a running build.

| | |
| --- | --- |
| **Can do here** | Read anything; change anything; compile and run the pure-Kotlin domain modules (`core:model`, `core:queue`, `core:fx`, `core:time`, `core:extract`, `core:search`) with a Kotlin CLI, including unit tests. |
| **Cannot do here** | Run Gradle (no JDK, no wrapper distribution, no `mavenCentral`/`google()` access), compile any Compose file, build the APK, launch an emulator, or render a Compose preview. |
| **Network** | Only `github.com`, `registry.npmjs.org`, `pypi.org` are reachable. `api.frankfurter.app` is **not** reachable from here (verified: connection fails), so the rate client has to be written against the documented contract and tested with a fake transport. |
| **Consequence for you** | "Build, run tests, show before/after screenshots" is only partly satisfiable here: I can build and test the domain logic for real, but **the APK build and every screenshot must run on your machine or CI**. I will write the `@Preview` functions and the stills entries so that producing them is one command, and I will not pretend a render happened. |

The repo's own stills harness (`tools/run-preview.sh --shots`) needs
`$HOME/.cache/tools` (JDK 21 + Compose jars), which is not part of the checkout and
is not downloadable from the hosts I can reach. `docs/shots/` currently contains
only the explicitly-labelled pre-review archive.

---

## 1. What the app is made of

| You said | Reality in the repo | Consequence |
| --- | --- | --- |
| "Kotlin app" | **Compose Multiplatform** app (Kotlin 2.4.21, Compose 1.12.2) with four targets: Android, JVM/desktop, iOS (`iosArm64`, `iosSimulatorArm64`), Wasm. Modules: `:core:{model,time,queue,extract,fx,search,design,store}`, `:ui:app`, `:app:{android,desktop,ios,wasm}`. | Any Android-only API (Room, ML Kit, Navigation Compose, Google Fonts) has to sit behind an interface the shared UI names. |
| "Jetpack Compose or XML views?" | **Compose, no XML.** The only XML is `res/drawable/ariadne_mark.xml`, `values/colors.xml`, `values/themes.xml` (splash/activity theme). There are no layout files and no migration to plan. | Nothing to do here; the redesign is composables. |
| "Room schema" | **SQLDelight** (`Ariadne.sq` + `1.sqm`/`2.sqm`/`3.sqm`, schema derived from migrations, `PRAGMA user_version` written on create). Tables: `trip`, `capture`, `dayCard`, `rate`, `settings`, `outbox`. | A Room migration would mean replacing the shared store and losing three targets. Recommendation: **migration 4 in SQLDelight** (see §6.D1). |
| "ViewModels" | **None.** One `AppModel` (not a `ViewModel`), one `StateFlow<AppUiState>`, one `AppGraph`. Created per process. | "List every ViewModel" = one class; the screen state is `AppUiState` + page-local `remember`. Introducing `ViewModel` per screen is possible (KMP `lifecycle-viewmodel-compose` is in the version catalog) but it would fight the single-source-of-truth design the bug fixes depend on. |
| "Navigation" | **No navigation library.** `sealed interface Route` + one `when` in `AriadneApp.kt` + a `Crossfade`. Back is whatever each page's buttons do; there is no back stack at all. | Predictable back + predictive-back gesture is a **model** change, not a library swap (see §6.D2). |
| "ML Kit" | Already wired: `app/android` depends on ML Kit text recognition (Latin + Devanagari/Japanese/Korean/Chinese), behind `core:extract`'s `Extractor`; desktop uses Tesseract. | Quick-add suggestions ride the existing contract; no new dependency. |
| HTTP | `ktor` 3.6.1 is in the version catalog but **used by no module**. `coil` likewise unused. DataStore is used only by the desktop settings adapter. | The rate client is the one place Ktor would be used — justified in one line: "Ktor is already in the catalog and gives a common HTTP client for both Android and desktop". |
| Theming | **No `MaterialTheme`.** `material3` is used for exactly three things: `Text` (×5), one `Surface`, one `OutlinedButton`. Colour lives in a bespoke `Paper` token object (`core:design/…/Bone.kt`). | "Proper Material 3 colour scheme" = introducing `AriadneTheme` and mapping the existing tokens onto `ColorScheme` roles. Real work, not a rename. |
| Previews | **No `@Preview` anywhere.** Stills come from a desktop `ImageComposeScene` harness (`preview/…/Shots.kt`, 44 frames). | I will add `@Preview` composables per screen; produce-able stills stay in the harness. |

---

## 2. Screens, routes, and the navigation graph

One state holder (`AppModel`), 16 composed surfaces in 12 files, 15 routes.

| Route | Page (file in `ui/app/…/ui/pages/`) | Bottom bar | Reached from |
| --- | --- | --- | --- |
| `Onboarding` | `OnboardingPage.kt` | hidden | cold launch, no trip ever |
| `NewTrip` | `NewTripPage.kt` | hidden | Onboarding "Begin empty", register "Begin a new trip", after delete, unreadable trip |
| `Home` | `TabPages.kt:138` `HomePage` | **Home** | tab, launch with open trip, after creating a trip |
| `Timeline` | `TabPages.kt:353` `TimelinePage` | **Timeline** | tab, Home index row |
| `Stats` | `TabPages.kt:455` `StatsPage` | **Stats** | tab, Home index row |
| `Trip` | `TripPage.kt` | **Trip** | tab, Home index row, card |
| `Trips` | `TripsPage.kt` | shown, nothing selected | Home index row "Trips", launch when all trips closed |
| `Preferences` | `TabPages.kt:640` `PreferencesPage` | shown, nothing selected | Home's `PREFS` action |
| `Settings` | — (alias) | — | **dead**: mapped to `PreferencesPage`, nothing navigates to it |
| `Capture` | `CapturePage.kt` | hidden | centre shutter, Home action, Record's `CAPTURE`, Frame "take another" |
| `Frame(captureId)` | `FramePage.kt` | hidden | right after the shutter |
| `Nightfall(day)` | `NightfallPage.kt` | hidden | Home "Finish n", Capture, Record, Stats, Frame |
| `Record(day)` | `RecordPage.kt` | Timeline selected | Timeline day, Home, Search result, Trip |
| `Detail(captureId)` | `DetailPage.kt` | hidden | Record row, Nightfall `EDIT`, Frame "fill it in now" |
| `TripCard(tripId)` | `TripCardPage.kt` | hidden | Trip page, Preferences "close the trip" |
| `Search` | `SearchPage.kt` | Home selected | Home index row, Record |

### 2.1 The graph as it actually behaves

```
        launch ──► Onboarding ──► NewTrip ──┐
             └──► Home ◄──────────────────────┘
                    │
   ┌────────┬───────┼─────────┬──────────┬──────────────┐
   ▼        ▼       ▼         ▼          ▼              ▼
Capture  Timeline  Stats     Trip      Trips        Preferences
   │        │        │         │          │              │
   ▼        ▼        ▼         ▼          ▼              │
 Frame   Record   Nightfall  TripCard   (Home) ◄─────────┘
   │        │        │
   │        ▼        ▼
   └──►  Detail ◄───┘   (both edit the same capture)
            ▲
         Search
```

There is **no back stack** and **no `BackHandler`** anywhere in the tree (verified by
grep). Every "back" is a button that calls `model.go(some route)` — producing a
*new* route rather than popping. Consequences:

- `Detail` carries a `detailOrigin` field purely so its "Back" button knows where to
  go;
- the Android system back gesture **finishes the activity from every route** — from
  the register, Preferences, Search, Detail, Frame, Nightfall, and from a capture
  sheet with typed text in it. There is no route on which back does the expected
  thing;
- predictive back cannot be supported at all, because there is nothing to preview.

### 2.2 Duplication and dead weight (what the redesign removes)

| Duplicate | What overlaps | Plan |
| --- | --- | --- |
| `Frame` + `Detail` + `Nightfall` | Three screens that all attach an amount/category/note to one capture; two of them are visually "the photograph" (copy in `FramePage.kt:50` and `DetailPage.kt:57/117`). | Collapse into one capture sheet (§5) + one "Close day" pager. `Frame` and `Detail` disappear. |
| `Timeline` + `Record` | Both are "days, with figures"; `Record(day)` is reachable from Timeline *and* is its own tab-less page. | `Timeline` becomes **Days** (strip + list); `Record` becomes the day sheet behind a row, not a route with its own masthead. |
| `Home` + `Record(today)` | Today's entries rendered twice with different totals paths. | `Home` = Today; nothing else renders today's list. |
| `Trip` + `TripCard` | Two "trip as an object" pages; the card is a printable duplicate of the Trip page's summary. | One **Trip** page (dates, budget, per-currency, switcher); "share card" becomes an action on it. |
| `Onboarding` + `NewTrip` | Two first-run screens, both creating a trip. | Keep Onboarding only for the very first launch (it is the only explainer), then New Trip. |
| `Search` | Alive but only reachable by two quiet links; the nav bar has no room for it. | Becomes a search field on the top of **Days** + a query in the capture sheet, not a route. |
| `Route.Settings` | Dead alias. | Delete after one release note; the parser alias can stay in the store layer. |
| `Prefs` vs `Preferences` | The action prints `PREFS`, the route/page say `Preferences`. | Rename the action to "Settings" in the new nav. |
| Unused deps | `ktor`, `coil` (catalog only), DataStore in `ui:app`. | Ktor gets used by the rate client; `coil` stays unused — I will not add it for the picker (justified in §6.D5). |

---

## 3. The six bugs, traced to a line

Every one of these reproduced in the source. "Line" is the current `main`.

### B1 — THB 2,555 + INR 50 = ₹2,605 (currency, critical)

Three separate defects, and the third is the one that makes the number wrong:

1. **There is no automatic rate source.** Rates only exist if a human types them in
   Preferences (`AppModel.setFxRate`, `rate` table, `(base, quote, rateDate)` key).
   A fresh trip has an empty `FxTable`, so every foreign amount is unconverted.
2. **The total silently adds the unconverted amount as if it were base currency.**
   `core/fx/…/Fx.kt:131-133`:
   ```kotlin
   val conversion = fx.convert(Money.ofMinor(raw), code, baseCurrency, date)
   if (!conversion.applied && code != baseCurrency) unconverted++   // counted…
   base += conversion.amount.minorUnits                             // …and still added
   ```
   `convert()` returns the amount *unchanged* when no rate exists, so 2,555 THB is
   added to 50 INR as 2,605. `unconvertedCount` is computed and then never shown.
3. **No capture remembers the rate it was entered at.** `capture` has
   `amountMinor` + `currency`, and nothing else (`1.sqm:31-52`). Totals are
   recomputed later against whatever the rate table says *then* (or, as below,
   against three different dates).

Also: three totals for the same captures are computed with three different dates —
`refreshDay` uses the day (`AppModel.kt:1210`), `loadTripDays` uses
`captures.first().localDate` (`AppModel.kt:1192`), and `categoryTotals` uses
`trip.startDate` (`AriadneStore.kt:340,349`). With a moving rate table those three
figures legitimately disagree.

**Fix (Phase 1):** `RateSource` (Ktor → frankfurter.app) writing into the existing
`rate` table, cached last-known rate, `capture.rateScaled` + `capture.rateDate`
snapshot at save time (migration 4), conversion never adds an unconverted amount
(it is shown separately and the total says so), one conversion date per total, and
"≈" + rate date wherever a converted figure appears.

### B2 — "ALL SETTLED" vs "STILL OPEN"

Two different sources of truth, both true:

- Home's pill is `state.stack.isEmpty()` (`TabPages.kt:246`), and `stack` is
  rebuilt from **the day that was last refreshed** (`AppModel.kt:1201-1210`). It
  means "nothing is waiting *on that day*", not "nothing is waiting".
- Timeline's day label is `if (closed) "in the record" else "still open"`
  (`LedgerParts.kt:299`), where `closed` is *the day card*, not any capture.
- Record's banner counts open captures for the day it shows
  (`RecordPage.kt:140-143`), a third derivation.

So a confirmed capture on an unclosed day reads "still open" while Home reads "all
settled". **Fix:** one `EntryStatus` (Captured → HasAmount → Confirmed, plus
`NoAmount` as an explicit terminal, and `InRecord` for archived) derived from
`Capture` by a pure function in `core:model`; every label, badge, pill, tray count
and total is computed from it. The 7-value `CaptureState` stays in the database
(it is migration-compatible) but becomes internal bookkeeping.

### B3 — Garbage titles ("ction View Go Run Terminal Help")

`core:extract/…/ReceiptParser.kt:206` `detectMerchant()` takes *the first line that
contains letters and few digits*, up to 48 chars, and `ExtractionService.kt:116`
writes it into `capture.merchant`. A menu-bar OCR line qualifies.

**Fix:** three layers — (a) a plausibility gate (reject UI vocabulary: `view`,
`terminal`, `help`, `file`, `edit`, `window`, `run`, `go`, plus all-caps runs,
plus lines that are mostly punctuation), (b) a confidence floor so a weak guess is
kept as a *suggestion* rather than written to the field, and (c) a **display**
fallback `Receipt · 21:09` computed from `createdAt` when `merchant == null` — the
fallback is never stored, so it can improve later without rewriting rows.

### B4 — Stats: "Not said" twice, total inconsistent

`Ariadne.sq:101-126` groups by **category and currency**, and
`AriadneStore.kt:330-353` appends one `CategoryTotal` per resulting row without
merging. Two uncategorised currencies → two "Not said" rows; a category spent in
two currencies → two "Food" rows. The page then sums `categoryTotals` for its
headline (`TabPages.kt:489`) while Home sums a *different* `Tally`
(`dayTotalMinor`) — different sets, different dates (B1.3).

**Fix:** convert per row, then merge into one bucket per category (one
"Uncategorised"), and give both pages the same `TripTotals` object computed once.

### B5 — The amount screen has no photo

It should: `PhotoPreview` renders a real image when `LocalPhotoImageRenderer` is
provided, and Android does provide one
(`app/android/…/AndroidPhotoImageRenderer.kt`, wired in `MainActivity.kt:41`). But
in the flow you photographed, the sheet you land on after "keep" is `Nightfall` →
`Triage`, which shows a 16:9 plate in a **scrollable column above the amount**
(`NightfallPage.kt:157`) — off-screen on a phone once the keypad is pinned. In
`Detail`, the plate is inside the scroll region too.

**Fix:** the new capture sheet always shows the photo as a fixed-height thumbnail
above the keypad (never scrolls away), and the Close-day pager shows the photo
above the keypad for the entry being settled.

### B6 — Text collisions and truncation

| Symptom | Cause |
| --- | --- |
| `₹10,000.00LEFT` | `TabPages.kt:526-548`: a `Row` with `Label("against a budget of")`, the budget, a weight-1 spacer, `"left"` and a numeral, **no gap guarantees and no wrap**. When the figures are wide the spacer collapses to 0 and the label touches the number. |
| `₹7,395.00 L…` | `core/design/…/Components.kt:104-105`: `Label` is `maxLines = 1, overflow = Ellipsis`; `Field` (`:455`) and `FigureRow` (`:540`) do the same. `Numeral` goes further and sets `maxLines = 1` with **no** overflow (`:191-215`), i.e. it hard-clips. So a money string wider than its slot is either ellipsised or cut mid-glyph depending on which of the two drew it. |
| `TAP ANYWHERE TO KEEP` over the photo | `CapturePage.kt:136-146`: the hint is drawn in the same `Box` as the live viewfinder, at `BottomCenter`, over camera content. |

**Fix:** money is never ellipsised (it is laid out with `IntrinsicSize`/wrap or a
smaller rung, never truncated); the budget row becomes a label + figure on their
own lines with the "left/over" state as a chip; the capture hint moves below the
plate.

---

## 4. Target flow vs current — the gap

| Target | Status today | Work |
| --- | --- | --- |
| Camera → shutter → sheet with keypad, currency, category, note | Frame page (bars) → Detail page (fields) → Nightfall (keypad) | **New** `CaptureSheet` (ModalBottomSheet), `Frame`/`Detail`/`Nightfall` retired |
| OCR suggests amount + merchant as chips | Extraction happens in the background and writes fields silently | Extractor returns a `Suggestion`; the sheet shows accept-chips; never auto-committed |
| Multi-select photo picker | `PickVisualMedia()` single (`AndroidPicker.kt:43`), `PhotoPicker.pick(): ByteArray?` | `pickMany(): List<ByteArray>` + tray; needs `PickMultipleVisualMedia` |
| Home = Today | Home + Record(today) + index rows | Merge; add pace + "Needs an amount (n)" tray strip |
| Days with a week strip | Timeline with a 24-hour tick chart (`LedgerParts.kt:64 DayBand`) | New strip + per-day list; the tick chart goes |
| "Close the day" review pager | "Nightfall" | Rename + reuse the stack mechanics; add a day summary at the end |
| Trip page: budget bar, per-day average, switcher | Trip page: progress count, per-currency rows | Add budget bar, per-day average, switcher |
| Stats: category chart, daily vs budget, top entries | Category rows + bars | Replace with chart + top entries; one total |
| 4 destinations + centre FAB | 4 tabs + notched bar (`TabBar.kt`) | `NavigationBar` + docked FAB, Material 3 |
| Empty/loading/error per list | Some empty states; almost no loading/error | `ListState` (Loading/Empty/Error/Content) in the design system |
| Undo for delete/edit | None | `SnackbarHost` + `UndoAction` in `AppModel` |
| Predictive back | Nothing (no back stack) | See D2 |

---

## 5. The three-phase plan I propose

**Phase 1 — bugs, on the current screens (smallest possible diffs).**
`EntryStatus` + tests; `FxTable`/`Tally` correction + `RateSource` + migration 4
(`capture.rateScaled`, `capture.rateDate`) + tests; one `TripTotals`; merchant gate +
display fallback + parser tests; Stats merge; photo on the amount sheet; overflow
fixes in `Label`/`FigureRow`/budget row/capture hint. Commit per bug group, all
domain tests green at each commit.

**Phase 2 — flow.** `CaptureSheet`, tray, multi-pick, `Today`, `Days`,
`CloseDayPager`, `Trip`, `Stats`, `NavigationBar` + FAB, back stack + predictive
back, empty/loading/error, undo. Old screens deleted, not hidden.

**Phase 3 — visual system.** `AriadneTheme` (Material 3 `ColorScheme`, light/dark,
optional dynamic), 5-size type scale with tabular figures, one serif for titles,
`AmountText`/`EntryCard`/`CurrencyChip`/`BudgetBar`/`EmptyState`/`PrimaryButton`,
4/8dp spacing, 48dp targets, motion (shared-element thumbnail → detail, sheet slide,
animated budget bar, save haptics), copy rewrite, `@Preview`s.

---

## 6. Decisions I need from you (with a recommendation each)

**D1. Data layer: SQLDelight vs Room.**
There is no Room schema. Migrating means dropping three of the four targets or
maintaining two stores.
→ **Recommend:** keep SQLDelight, add `4.sqm` (additive, nullable) and keep the
existing `verifyMigrations` gate. Cost: zero data loss, no new dependency.

**D2. Navigation: Navigation Compose vs the sealed `Route`.**
`androidx.navigation:navigation-compose` is Android-only, and its type-safe routes
cannot be used from `commonMain`. Predictive back needs a real back stack, which is
a model change either way.
→ **Recommend:** keep `Route` (it is already type-safe: a sealed hierarchy with
data classes), add an actual `List<Route>` back stack in `AppModel`, and put
predictive back behind `expect/actual` (`PredictiveBackHandler` on Android, no-op
elsewhere). Cost: one small API, no dependency, works on all four targets.

**D3. OCR quick-add.** ML Kit is Android-only; the shared UI cannot name it.
→ **Recommend:** extend the existing `Extractor` contract with a `Suggestion`
(amount, merchant, confidence), implemented by ML Kit on Android and Tesseract on
desktop; the sheet renders chips from it. No new dependency.

**D4. Rates + permission.** frankfurter.app needs `android.permission.INTERNET`
(not in the manifest today) and is unreachable from this sandbox.
→ **Recommend:** add `INTERNET`, Ktor client (already in the catalog) +
`RateSource` interface, cache into the existing `rate` table, last-known-rate
fallback, and a fake-transport test. Cost: one permission, one dependency already
declared, no new service account.

**D5. Fonts and dynamic colour.**
`ui-text-google-fonts` is Android-only and downloads fonts at runtime, which
conflicts with offline-first and with the two vendored families already in the repo
(Instrument Serif + Inter Tight, with tabular figures).
→ **Recommend:** keep the vendored fonts and expose them through a `Typography`;
dynamic colour only as an opt-in on Android 12+ via `expect/actual`, with the
defined scheme as the default. Cost: no new dependency, no runtime download.

---

## 7. Assumptions if you don't answer

I proceed with the recommended option in each case and note it in the phase
summary. D1 and D2 are the two that would be expensive to reverse, so if you only
answer two, answer those.

---

## 8. What I will hand back per phase

- **Phase 1:** a commit per bug group; `core:fx` + `core:model` unit tests compiled
  and **run here**; a written before/after of each of the six symptoms with the
  line that caused it; the migration documented in `tools/store-check.sh` terms.
- **Phase 2:** the new routes and the deleted ones; a back-stack table; the capture
  sheet's states; tests for `EntryStatus` transitions and for the tray.
- **Phase 3:** `@Preview` functions per screen (phone + 200% + dark), the theme
  tokens with contrast ratios, and the copy table (old string → new string).

Screenshots: I will add the stills entries and the previews, and state plainly that
rendering them requires `./tools/run-preview.sh --shots` on a machine with the
Compose jars, which this environment does not have.
