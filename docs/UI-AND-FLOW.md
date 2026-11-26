# Ariadne — the interface and the flows, as built

**What this document is.** A description of the app that the source in this repository
actually renders, written by reading every screen and every navigation call in
`ui/app` and `core/design`. It replaces an earlier version of this file that described
the design *intent* and a sequence of redesign passes. That file was out of date: it
described a landing page, a bar and a set of routes that are not in the code. This one
describes what is there.

**Audited at** `c168fe6` (merge of PR #10), 2026-11-27.

**What it is not.** It is not a roadmap, a changelog, or a claim that any of this has
been looked at on a screen. No screenshot in this repository was produced by this
build: `docs/shots/` contains only
`docs/shots/archive-2026-11-26-pre-third-review/`, which predates it. Everything below
is derived from source, and where the code has a defect, the defect is written down
(§10) rather than described as a feature.

**Where the truth lives.**

| Question | File |
| --- | --- |
| What surfaces exist | `ui/app/src/commonMain/kotlin/com/ariadne/app/Route.kt` |
| What each route draws, what the bar shows | `ui/app/.../ui/AriadneApp.kt` |
| The bar, the tabs, the tab↔route mapping | `ui/app/.../ui/Nav.kt` |
| What the model can do | `ui/app/.../AppModel.kt` (1 704 lines) |
| What a screen can read | `AppUiState` in `Route.kt` |
| Home, Timeline, Stats, Preferences | `ui/app/.../ui/pages/TabPages.kt` (1 070 lines) |
| The rest of the screens | `ui/app/.../ui/pages/*.kt` |
| Design primitives and palette | `core/design/src/commonMain/kotlin/com/ariadne/design/` |

---

## 0. Glossary

| Term | Meaning in this codebase |
| --- | --- |
| **Trip** | A dated container for captures. Exactly one is *active*; photographs land in it. |
| **Capture** | One photograph and what is known about it. State machine: `QUEUED → EXTRACTING → NEEDS_AMOUNT / NEEDS_CONFIRMATION → CONFIRMED → ARCHIVED`. |
| **Stack** | The opened day's captures that still need something, in pager order. Counted in the bar. |
| **Sheet** | The capture editor that rises over the page after a photograph (`FramePage` over `state.sheetCaptureId`). Not a route. |
| **Shutter** | The camera cover (`CaptureOverlay` over `state.shutterOpen`). Not a route. |
| **The card** | The keepsake summary of a finished trip. |
| **Fence** | The optional trip budget. Never called a target; over-spending is stated in the same voice as under-spending. |
| **Base / home currency** | Every total is stated in the base currency; the home currency is what a foreign amount is notionally converted back to. |
| **Paper / ink** | The palette object (`Paper`) and its two ladders of text colour (`ink` → `inkGhost`). |

---

## 1. The shell

`AriadneApp(model, wide)` composes one `Box`:

1. the current route's page (`Destination(route, …)`);
2. the notice bar (undo), drawn over everything but the overlays;
3. the shutter cover, if `state.shutterOpen`;
4. the sheet, if `state.sheetCaptureId != null`;
5. the bottom bar, if `route.showsTabBar()`.

Stacking order is load-bearing: the sheet draws after the shutter, so a capture never
shows a viewfinder underneath it; the bar draws last, so notice and overlays cover it.

### 1.1 The bottom bar (`Nav.kt`)

- A Material 3 `NavigationBar` with four `NavigationBarItem`s: **Today, Days, Trip,
  Stats**, and an **empty item with `weight(1f)`** in the middle.
- The shutter is a `FloatingActionButton` (56dp, `CircleShape`, `paper.blue`, drawn
  aperture glyph) aligned to the bar's top-centre and offset up by 20dp — i.e. it sits
  in the reserved middle slot, in the gap between the labels.
- The Today icon carries a numeric badge (`paper.blue` circle, count) when captures are
  waiting. Not a dot: the count is information.
- `captureEnabled = state.trip?.isClosed == false`; when false the FAB is drawn in
  `paper.edge` with the description "A closed trip takes no photographs".
- FAB content descriptions: "Take a photograph", "Take a photograph. N waiting for an
  amount", or the closed-trip sentence.
- Tab glyphs are drawn on canvas (sun over a horizon, calendar page, suitcase, bars) —
  there is no icon font in this app.

### 1.2 What the bar does on a stacked page

`Route.tab()` maps a route to the tab it belongs to, or `null`:

| Route | Tab | Bar shown |
| --- | --- | --- |
| `Home` | Today | yes |
| `Days`, `Record`, `CloseDay` | Days | yes |
| `Trip`, `TripCard` | Trip | yes (`TripCard` hides the bar — see below) |
| `Stats` | Stats | yes |
| `Trips`, `Preferences`, `Settings`, `Search`, `Detail` | none | yes, stacked, with a spoken description ("Preferences, stacked over Today") |
| `Onboarding`, `NewTrip`, `Capture`, `Frame` | none | **no bar** |
| `CloseDay`, `TripCard`, `Detail` | none | **no bar** |

`showsTabBar()` is the single source: everything except
`Onboarding, NewTrip, Capture, CloseDay, TripCard, Detail, Frame` shows the bar.

> Note the overlap in `Route.tab()`: `CloseDay` and `TripCard` are mapped to a tab *and*
> excluded from `showsTabBar()`. The mapping only affects which tab would be highlighted;
> neither route draws a bar.

### 1.3 Where the app opens

`startRoute(hasAnyTrip, hasOpenTrip)` in `Route.kt`, called from `AppModel.start()`:

| State of the device | Opens on |
| --- | --- |
| No trip ever created | `Onboarding` |
| A trip is open | `Home` |
| Trips exist, all closed | `Trips` (the register) |

---

## 2. Navigation

### 2.1 The route set

Sixteen route objects, of which three are legacy (§8).

| Route | Surface | Kind |
| --- | --- | --- |
| `Onboarding` | first-run pitch | full screen |
| `NewTrip` | the trip form | full screen |
| `Home` | Today | tab |
| `Days` | Timeline | tab |
| `Trip` | the trip page | tab |
| `Stats` | what it came to | tab |
| `Record(day?)` | one day's record | stacked over Days |
| `CloseDay(day)` | the settling pager | full screen |
| `Detail(captureId)` | one photograph, editable | full screen |
| `TripCard(tripId)` | the keepsake | full screen |
| `Trips` | the register | stacked |
| `Search` | search the kept | stacked |
| `Preferences` | settings | stacked |
| `Capture` | the standalone viewfinder | full screen, legacy (§8) |
| `Frame(captureId)` | the standalone sheet | full screen, legacy (§8) |
| `Settings` | alias of `Preferences` | retired name, still resolves |

### 2.2 The stack

- `AppModel.go(route)`: switching **between** destinations replaces the stack; going
  **into** a page pushes the current route onto `AppUiState.history`; any navigation
  clears the overlays (`shutterOpen`, `sheetCaptureId`).
- `AppModel.back()`: closes the sheet → closes the camera → pops `history`. Returns
  `false` at the bottom, which leaves the app.
- `canGoBack` is true when an overlay is open **or** `history` is non-empty.
- There is no navigation library. One `when` in `AriadneApp.kt` is the whole table.

### 2.3 Overlays

Two pieces of state are deliberately not routes:

| State | Drawn by | Opened by | Closed by |
| --- | --- | --- | --- |
| `shutterOpen: Boolean` | `CaptureOverlay` (full cover) | `openCapture()` when a trip is open; the bar's FAB; `RecordPage`'s "Capture"; "Add a photograph" | `closeShutter()`, Back, Escape, taking a frame |
| `sheetCaptureId: String?` | `FramePage` (bottom sheet, 84% height) | `capturePhoto()` after a frame; `importPhoto()` | `closeSheet()`, `keepFrameForLater()`, `saveDetail()`, `openFrame()` (goes to `Detail`), Back, Escape |

`openCapture()` falls back to `Route.Capture` when there is no trip or the trip is
closed; that fallback is the only way the standalone viewfinder page is reached.

### 2.4 Keyboard (desktop only)

`handleKey` in `AriadneApp.kt`:

| Key | Effect |
| --- | --- |
| Escape, with an overlay open | closes the overlay (same as Back) |
| Escape, on `CloseDay` | `go(Route.Capture)` — leaves the pager for the viewfinder page |
| Enter / NumPadEnter, on `CloseDay` | `confirmCurrent()` |
| J / ↓, K / ↑, on `CloseDay` | next / previous card |
| 0–9 and `.`, on `CloseDay` | type into the amount |
| Backspace, on `CloseDay` | delete a digit |

Everywhere else, keys do nothing.

---

## 3. The flows that exist

### 3.1 First run

`Onboarding` ("The photograph is the memory. / The numbers wait for night.") →
**Begin with the demo roll** (`startTripDemo()`: prefills a draft and commits it with a
demo roll of captures) or **Begin empty** (`startNewTrip()` → `NewTrip` with the draft
prefilled to today … today+6 and the currency from settings) → **Open the trip** →
`Home`.

`NewTrip` fields, in order: the trip (name), where, from, to, currency
(`CurrencyField`, an inline searchable catalogue), budget if you have one. Extras on the
page: "Demo roll", "Prefill from demo", "Go back".

### 3.2 The everyday loop

1. From `Home` (or `Days`, or `Record`, or any page with the bar): press the FAB.
2. `CaptureOverlay` covers the page. Take the frame.
3. The camera closes and `FramePage` rises over the **same** page with the frame in it
   (§5.8).
4. Type an amount, or **Add the amount later**. The sheet closes and the person is
   exactly where they were; the day's figure has already moved.
5. Repeat.

Nothing navigates in this loop.

### 3.3 Settling a day

Entry points: `Home`'s "Finish N", `Record`'s "Close day", `Pager` per card, or the
`CloseDay` route directly.

`CloseDay` pages through the day's waiting captures one card at a time: plate, figure,
keypad, **Confirm** / **No amount** / **Yes, save it** when extraction offered a figure,
**Edit** to open `Detail`, Earlier/Later to move, and a closing summary when the stack is
empty ("N left to settle", the day's figure, and the day's counts). **Back to the day**
returns to `Record`.

Closing the day itself (`closeDay(day)`) files it: unfilled captures become "no amount",
the day's card is marked closed, and the day becomes read-only.

### 3.4 Reading the record

`Days` (Timeline) → a `DayPlate` per day of the trip, each opening `Record(day)`. The
record has a horizontal strip of day tabs, the day's receipt lines (each opens `Detail`),
a per-currency block, a ruled total, and one primary action — **Close day** (inert when
the day is already closed or the trip is closed) — plus "Search", "Trip", and "Close this
day".

### 3.5 The card

`Trip` → **Close the trip into a card** (or **The card** if already closed) →
`TripCardPage`: a single column of type, tappable to copy the text to the clipboard, with
"Back to the record" and "Back to the trip".

### 3.6 Switching trips, and settings

`Home` → its **Trips** row (or the Home masthead's **PREFS** action for settings).
`Trips` lists every trip with its dates and figure; tapping one makes it active
(`switchTrip`), and the page offers "Start a new trip" and "Or open the demo roll".
`Preferences` is reachable from `Home` (PREFS), `Trip` (budget and rate links) and
`Preferences`' own rows.

---

## 4. State the UI reads

One immutable `AppUiState`, replaced wholesale by `AppModel`. The fields the screens
actually read:

| Field | Read by |
| --- | --- |
| `route`, `history`, `canGoBack` | the shell |
| `shutterOpen`, `sheetCaptureId` | the shell (overlays) |
| `theme` | `AriadneApp` → `AriadneTheme` (light / night / system) |
| `trip` | every page: name, dates, currencies, budget, closed flag |
| `today` | Home, Record, CloseDay, Preferences |
| `dayCards` | Home (rail), Days, Record, Trip |
| `dayCaptures`, `dayTotalMinor`, `dayCurrency`, `perCurrency`, `dayUnconverted`, `dayRateDates` | Home, Record, CloseDay, Stats, Detail |
| `stack`, `stackIndex`, `amountInput` | the pager and the bar badge |
| `dayCapturesAll`, `dayFigures` | Days, Trip, Stats, the sheet's lookup of a frame |
| `categoryTotals` | Stats, Trip, Home |
| `detail`, `detailAmount/Merchant/Note/Category` | Detail and the sheet |
| `searchQuery`, `searchResults` | Search |
| `trips`, `activeTripId` | Trips, Trip, Home |
| `draft` | NewTrip |
| `railDocked` | every tab page (the hanging margin) |
| `notice` | the notice bar (undo) |
| `busy`, `rateError`, `failureNote` | Preferences, the sheet's "reading…" state |
| `deleteArmed`, `cardCopied` | Preferences, TripCard |

`AppModel` exposes ~70 public functions; the UI calls navigation
(`go`/`back`/`openX`), editing (`editDetail*`, `chooseCategory`, `changeDetailCurrency`,
`saveDetail`, `undoLastEdit`), pager moves (`confirmCurrent`, `next`, `previous`,
`rejectCurrentExtractedAmount`), trip lifecycle (`startNewTrip`, `commitDraft`,
`switchTrip`, `closeTripIntoCard`, `armTripDeletion`, `deleteTripNow`), settings
(`setTheme`, `editTripCurrency`, `editTripHomeCurrency`, `setFxRate`, `clearFxRate`,
`refreshRates`), and the two the sheet added: `sheetPreview()` and `reExtractCurrent()`.

---

## 5. The surfaces, top to bottom

Every page sits inside one of two frames: `TabPage` (a scrolling column with the hanging
margin and the scroll rail) or `ScrollingMargin` / a bare column. Page headers on the tab
pages are `Masthead` — a plate number (`01`–`04`), an eyebrow, a serif title, and an
optional `PageAction` in the corner.

### 5.1 Onboarding (`OnboardingPage.kt`)

Full screen, no bar. Eyebrow "Ariadne" on a phone; a 40/66sp serif headline in two
lines ("The photograph is the memory." / "The numbers wait for night."); a rule; one
paragraph about chai, shoes and settling the day; **Begin with the demo roll** (the one
blue control) and **Begin empty**; a foot line, "No account · no server · …".

### 5.2 New trip (`NewTripPage.kt`)

Full screen, no bar. A stack of labelled hairline fields — the trip, where, from, to,
currency, budget — each set in the serif at reading size. **Open the trip** is the blue
action; the draft's parse errors appear above it. Secondary: "Demo roll", "Prefill from
demo", "Go back".

### 5.3 Home — the Today tab (`TabPages.kt`, `HomePage`)

`Masthead` 01, eyebrow "Ariadne", title = the trip's name (or "No trips yet" / "No trip
open"), trailing `PREFS` → `Preferences`.

- **No trip**: one paragraph, **Start a trip** (→ `NewTrip`), and either **Begin with the
  demo roll** or **The register**.
- **Closed trip**: one paragraph saying so, **See the card**, **The register**.
- **Open trip**:
  - the day's figure, large on the paper, `paper.symbol + amount`;
  - a row: "finished today" and a pill — "all settled" or "N waiting";
  - **Take a photo** (blue, shutter glyph) and, when anything is waiting, **Finish N** →
    `CloseDay`;
  - "or bring one in from the library" — only when the platform supplies a picker;
  - `SectionHead` II "the trip" with the date range, then five `NavRow`s, each revealed
    with a short offset animation: **Trips**, **Timeline** (→ `Days`), **The record**
    (→ `Record(today)`), **Search what you kept**, **The trip's page**;
  - the closing line "No account. No sync. The record stays here."

The rail (wide windows) shows today's date, the number of days with days closed, and the
waiting count with its label.

### 5.4 Timeline — the Days tab (`TabPages.kt`, `TimelinePage`)

`Masthead` 02, eyebrow = the trip's name, title "Timeline", trailing `Record`. Then a
`DayPlate` for every day of the trip: date, closed/open, the figures, the captures. Each
plate opens `Record(day)`. Rail: closed count, open count.

### 5.5 Record — one day (`RecordPage.kt`)

`Masthead` (no number), eyebrow = the trip's name, title "The record", trailing
`Capture`. Then the day as a large date, a horizontal strip of day tabs, and either the
day's receipt lines (number, photograph, title, figure — each opens `Detail`) or the
empty line "Nothing kept on this day…" with **Add a photograph**. Under a rule: the
day's per-currency rows, the "Tallied" figure with an `≈` when anything was converted,
the **Close day** action (blue; disabled when the day or trip is closed), "Search",
"Trip", and a "Close this day" quiet action when the day is still open.

There is no table on this page by design.

### 5.6 Close day — the pager (`CloseDayPage.kt`)

No bar. A masthead "Close day"; the card's photograph (or the ruled plate when the
platform has no camera and there is no image); the merchant and metadata; the amount —
extracted figure with **Yes, save it** / **No, let me enter it**, or the keypad with
**Confirm**; **Edit** → `Detail`; Earlier / Later; and, when nothing is left, a summary
("Nothing left to settle" / "N left to settle", the day's total, the counts) with **Back
to the day**.

### 5.7 Detail — one photograph (`DetailPage.kt`)

No bar. `Masthead` (no number), eyebrow = the capture's date, title "The photograph",
trailing "Back" → `closeDetail()` (returns to whatever opened it: record or search).
Then: the photograph; the amount field with the keypad (or the extraction offer, when
the row has an unconfirmed figure); currency chips (`CurrencyChip`); the category chips
(`CategoryChips`); "Where" and "Remember it by" (`LineField`); and the actions —
**Save** (blue), **Keep it without an amount**, **Leave it open**. On a closed trip the
fields are printed, not editable.

### 5.8 The sheet — the photograph just taken (`FramePage.kt`)

A bottom sheet at 84% of the screen over the photograph and a scrim, or — on a wide
window — a spread with the print on the left and the fields on the right.

Header row: a drawn back chevron (44dp, description "Back", `keepFrameForLater()`), the
line "the photograph is kept", and a pill with the entry status ("Needs an amount" /
"Not confirmed" / "In the record").

Body, top to bottom:

1. **The moment** — a 62dp thumbnail, the day spelled out (`TUESDAY 11 NOVEMBER ·
   05:30`), and the figure as it stands (or `0` with "nothing typed yet").
2. **The suggestion** — "Found S$18.41 on it." with `yes` (`acceptDetailSuggestion()`) and
   `no` (`rejectDetailExtractedAmount()`), only when the row has an unconfirmed amount and
   nothing is typed.
3. **The pair** — a sunk block with two labels, `ORIGINAL PURCHASE` and `BASE TRIP
   EQUIVALENT`, the two figures with `≈` between them, and one line underneath: the rate
   (`rate: 1 SGD ≈ 74.6298 INR`), "already in the trip's currency", "no SGD rate yet — it
   will be counted in SGD", or "type an amount and the pair updates". Derived by
   `model.sheetPreview()` from the typed text, the capture's currency and today's rate.
4. **Read the photograph again** — `reExtractCurrent()`; the label reads "Reading the
   photograph…" while `busy`.
5. **Amount** — the currency's symbol in its own 56×52 box, the field beside it, the
   sentence "will convert to the trip's currency (INR) on save" (or "already in the
   trip's currency, INR"), the ruled keypad, then…
6. **Currency** (the shortlist chips), **Category** (the six chips in two rows),
   **Merchant**, **Remember it by**.
7. **Date** — printed: `11/11/2025 · 05:30` and "as photographed". Not editable.

Foot, pinned: **Save &lt;amount&gt;** (blue) and "Add the amount later", or **Add the amount
later** alone when nothing is typed; then "open the whole page" → `Detail`.

### 5.9 The shutter — the camera cover (`CaptureOverlay.kt`)

A full cover over whatever page is showing: a close control, the trip's name, a rule, the
live preview (or the ruled plate when the platform has no camera), a square plate that
takes the frame on tap, the shutter row (**From the library** / the shutter / "N
waiting"), and the line "no figure here · the amount comes after". With no trip or a
closed trip it renders the "nowhere to file this" state instead.

### 5.10 Stats — the Stats tab (`TabPages.kt`, `StatsPage`)

`Masthead` 03, eyebrow = the trip's name, title "What it came to". The trip's figure
(animated to its value), ruled off, with "finished photographs only" attached to it; the
budget fence when one exists ("against a budget of … left/over"); one paragraph about
what is not counted; then `SectionHead` II "what it was" (the category `Breakdown` and
`PatternLine`), and III "by currency, before conversion" (the per-currency rows). Rail:
the trip's figure, the photograph count, "finished only".

### 5.11 Trip — the Trip tab (`TripPage.kt`)

Title = the trip's name; place and dates; **closed** pill if closed. Then: what it has
cost against the budget (`BudgetBar` with its pace tick, and "change the budget" / "set a
budget" → `Preferences`); "N of M days closed" with "see the days" → `Days`; what it was
spent in (per currency, each with a subtotal, then the one figure) with "the rates" →
`Preferences`; a switcher listing the other trips, when there is more than one; and the
actions — **The card** or **Close the trip into a card**, **Start another trip**, "the
register".

### 5.12 Trips — the register (`TripsPage.kt`)

`Masthead`, title "The register", trailing `Home`. A paper register: numbered rows, dates
in the margin, a figure on the right; the active trip is marked by the page's one blue
element. Tapping a row calls `switchTrip`. At the foot: **Start a new trip** and "Or open
the demo roll".

### 5.13 TripCard — the keepsake (`TripCardPage.kt`)

No bar. A single column of type: name, dates, the days, the currencies, one total, and a
line saying the record is yours. The card itself is a tap target that copies its text
(`copyCardToClipboard()`); "copied" is shown for a moment. "Back to the record" and
"Back to the trip".

### 5.14 Search (`SearchPage.kt`)

One field, sized like a title, ruled underneath; results as photographs with a price
beneath them, each opening `Detail` with `DetailOrigin.Search` so Back returns to the
query.

### 5.15 Preferences (`TabPages.kt`, `PreferencesPage`)

`Masthead` 04, title "Preferences", trailing `Done` → `Home`. Four sections:

- **I · the record** — base currency (`CurrencyField`), home currency, a dates read-out
  (ISO, not editable), and **Paper**: light / system / night as three chips.
- **II · the exchange** — one row per currency in use plus THB, USD, EUR, GBP, JPY: the
  current rate, an inline editor (one open at a time), and clear. Errors are stated
  under the list (`rateError`).
- **III · what leaves this device** — "Reading: on device only", a paragraph, and, if
  anything ever failed, the last error (`failureNote`) with the sentence that says
  nothing was lost.
- **IV · this trip** — see the trip's page; close the trip into a card (or see the card
  when closed); delete this trip, which is armed by the first tap and confirmed by
  "Delete for good" or disarmed by "Keep it".

---

## 6. Cross-cutting behaviour

**Type and colour.** Serif (Instrument Serif) for titles, sans for labels and running
text, mono with tabular figures for money (`Numeral`). Labels are tracked uppercase at
9–11sp. Money never passes through `Label` (it ellipsises). Colour is paper, ink and one
blue: `Blueprint`/`blue` is reserved for the single primary action on a surface
(`ClaimAccent` records it), plus the waiting badge and the over-budget figure.

**Rules.** 1dp hairlines (`Rule`, `Bone.Hairline`) are the structural device; there is
no shadow anywhere, and only the FAB is round besides the chips' 1dp corners and the
sheet's square edges.

**Motion.** `Reveal` (Home's rows) and `animatedLong` (Stats' figure) respect
`LocalMotionPreference.reduced` and settle on the first frame when reduced motion is on.

**Theme.** Light, night and system; the night palette is the same design on dark stock.
Chosen in Preferences, persisted in settings.

**Accessibility as coded.** Tap targets are `rowTap` (clickable with role Button) or
`Modifier.semantics { contentDescription = … }` on drawn controls; the shutter cover's
plate has a description; the bar and FAB have descriptions that change with state. Text
sizes are in `sp` and pages scroll, but **no page has been rendered at 200% font** (§11).

**Insets.** The shell applies `WindowInsets` padding; the camera cover and the sheet both
account for the navigation bar via the root padding.

---

## 7. Platforms, as wired

| | Camera | Photo picker | OCR | Exchange rates | Storage | Review |
| --- | --- | --- | --- | --- | --- | --- |
| **Android** (`app/android`) | CameraX binding provided via `LocalCamera` | `PickMultipleVisualMedia(12)` | ML Kit on-device | `HttpURLConnection` → frankfurter.app, 5s timeouts | SQLDelight + private files | none |
| **Desktop** (`app/desktop`) | none — the ruled plate | system file dialog | Tess4J | none provided | SQLDelight (jdbc-sqlite, `~/.ariadne`) | none |
| **Wasm** (`app/wasm`) | none | browser file input (if hosted) | optional JS hook (`window.ariadneRecognize`), otherwise fails politely | none | in memory (`InMemoryStore`) | none |
| **iOS** (`app/ios`) | none | photo library | Vision | none | native SQLite | StoreKit |

Consequences that are visible in the UI: on desktop, Wasm and iOS the camera cover shows
the engraved plate rather than a live preview; on Wasm nothing survives a reload; the
rate list on desktop/Wasm/iOS shows only what was typed in by hand.

---

## 8. Legacy, unreachable, and absent

**Kept but effectively unreachable** — both still compile, still render, and are kept so
a route persisted by an older build resolves:

- `Route.Capture` → `CapturePage`, the standalone viewfinder page. `openCapture()` only
  routes here when there is no trip or the trip is closed, and on desktop Escape from the
  pager goes here. Otherwise the camera is the overlay.
- `Route.Frame(captureId)` → `FramePage` as a full page. `openFrame()` now closes the
  sheet and opens `Detail` instead, so this route is only reachable from a restored
  route.
- `Route.Settings` → `PreferencesPage` (retired `Preferences` name).

**Not implemented anywhere in the UI** (some of these appear in design documents):

- Deleting a single photograph. Trips can be deleted; captures cannot.
- Editing a capture's date. The sheet prints it; there is no control.
- Voice capture, SMS import, bill splitting, accounts, sync, sharing/export beyond
  copying the card's text.
- Multiple photographs per capture, or crop/rotate.
- Onboarding skip/step state: it is one page with two exits.
- A "More" tab or a settings drawer: settings live on `Preferences`, reached from the
  PREFS action on Home, from Trip, and from the record's own rows.

---

## 9. Copy that the code does not support — as built, these are wrong

1. ~~**Preferences §III says "there is no network call in the product at all".**~~
   **Fixed in this change.** It was false: `refreshRates()` performs a GET to
   `frankfurter.app` through the platform's `RateSource` (Android implements it; the
   other targets do not provide one). One read-only request for rates, carrying nothing
   about the trip, and every failure swallowed — but the sentence as printed was untrue.
   Preferences now states exactly that, and the same over-claim in the foot lines of
   Onboarding and New trip ("nothing leaves this device") is now "no sync". The README
   claimed the manifest declares no `INTERNET` permission; it does, for that one request,
   and the README now says so.
2. **Stats §III, "by currency, before conversion", reads `state.perCurrency`.** That
   field is filled by `refreshDay()` from *the day currently open*, not from the trip, so
   on Stats the section under a trip-wide total can be the opened day's money. The
   section's own sentence ("these are the amounts as they were paid, not converted") is
   true of the figures; its position under the trip total is what misleads.
3. **`Home`'s rail and the `Today` badge count different things.** The rail shows
   `state.stack.size` for "waiting"; the badge counts `state.tripPending` (every pending
   capture in the trip). On a trip with older unsettled days they disagree.

These are recorded rather than fixed here. None is a design question; each is a bug with
a known cause.

---

## 10. Verification status

| Claim | Status |
| --- | --- |
| `:core:model` tests | 47 tests, run green in this environment (no Gradle) via `tools/bootstrap-toolchain.sh` |
| `:core:fx` tests | 38 tests, run green the same way |
| The whole shared tree compiles for syntax, conflicting declarations and non-exhaustive `when`s | checked with `kotlinc` against `core/*` + `ui/app` + `preview` |
| The app builds and its shared tests pass on all four targets | done by CI (`.github/workflows/ci.yml`: `jvmTest`, `:app:android:assembleDebug`, `:app:wasm:wasmJsBrowserDistribution`, desktop) |
| `FlowCheck` (stack, overlays, undo, the sheet's preview) | written; **never executed** — needs the Compose fixtures |
| Any screen has been looked at | **no**. Nothing here has been rendered, measured, contrast-checked or read at 200% |
| Stills | `preview/Shots.kt` defines the shots; `docs/shots/` holds only a pre-redesign archive |

The gate for the design claims is `tools/verify-ui.sh` (and `tools/shoot.sh` for stills)
on a machine with Gradle and a display. Until that has run, this document describes the
code, not the appearance.
