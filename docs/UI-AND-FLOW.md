# Ariadne — the interface, surface by surface, as built

**What this document is.** A description of the app that the source in this repository
actually renders — every surface, every block of it, and the exact words on the buttons,
labels, pills, placeholders, empty states and notes — read out of `ui/app` and
`core/design`. It replaces an earlier version of this file that described design intent
and a sequence of redesign passes. That file described a landing page, a bar and a set of
routes that are not in the code.

**Audited at** `c168fe6` (merge of PR #10), 2026-11-27. **Second revision:** every
surface now lists its blocks in order, with the exact words on every label, button, pill,
placeholder, empty state and note, and the control each one calls.

**What it is not.** Not a roadmap, not a changelog, and not a claim that any of this has
been looked at on a screen. No screenshot in this repository was produced by this build:
`docs/shots/` holds only `docs/shots/archive-2026-11-26-pre-third-review/`.

**How strings are quoted here.** Exactly as they appear in the source. Two rules of the
design system change how they appear on screen and are worth stating once:

- **`Label`, `Action`, `OutlineAction`, `QuietAction`, `PageAction` and `Pill` uppercase
  their text.** So `Label("finished today")` renders **FINISHED TODAY**, and
  `Action("Take a photo")` renders **TAKE A PHOTO**. The strings below are quoted in
  source form.
- **`Title`, `Body`, `Numeral`, `NavRow` and field placeholders do not.** A serif title
  reads as written, in sentence case.
- Text in `·` sets is a tracked `Label`; `—` is an em dash; `…` is a real ellipsis.

**Where the truth lives.**

| Question | File |
| --- | --- |
| What surfaces exist | `ui/app/src/commonMain/kotlin/com/ariadne/app/Route.kt` |
| What each route draws; the shell | `ui/app/.../ui/AriadneApp.kt` |
| The bar, the tabs, tab↔route mapping | `ui/app/.../ui/Nav.kt` |
| What the model can do | `ui/app/.../AppModel.kt` |
| What a screen can read | `AppUiState` in `Route.kt` |
| Home, Timeline, Stats, Preferences | `ui/app/.../ui/pages/TabPages.kt` |
| Everything else | `ui/app/.../ui/pages/*.kt` |
| Design primitives, palette, glyphs | `core/design/src/commonMain/kotlin/com/ariadne/design/` |

---

## 0. Glossary

| Term | Meaning in this codebase |
| --- | --- |
| **Trip** | A dated container for captures. Exactly one is *active*; photographs land in it. |
| **Capture** | One photograph plus what is known about it. `QUEUED → EXTRACTING → NEEDS_AMOUNT / NEEDS_CONFIRMATION → CONFIRMED → ARCHIVED`. |
| **Stack** | The opened day's captures that still owe something, in pager order. Counted in the bar and on Home. |
| **Sheet** | The capture editor that rises over the page after a photograph (`FramePage`, driven by `state.sheetCaptureId`). Not a route. |
| **Shutter** | The camera cover (`CaptureOverlay`, driven by `state.shutterOpen`). Not a route. |
| **The card** | The keepsake summary of a finished trip (`TripCardPage`). |
| **Fence** | The optional trip budget. Never called a target; over-spending is stated in the same voice as under-spending. |
| **Base / home currency** | Every total is stated in the base currency; the home currency is what a foreign amount is notionally converted back to. |
| **Plate number** | The `01`–`04` mark in a tab page's masthead (`Masthead(number = …)`). Pages outside the sequence pass `""` and get no mark. |

---

## 1. The shell

`AriadneApp(model, wide)` composes one `Box`, in this order:

1. the current route's page (`Destination(route, …)`);
2. the **notice bar** (undo), if `state.notice != null`;
3. the **shutter cover**, if `state.shutterOpen`;
4. the **sheet**, if `state.sheetCaptureId != null`;
5. the **bottom bar**, if `route.showsTabBar()`.

The order is load-bearing: the sheet draws after the shutter (a capture never shows a
viewfinder under it), and the bar draws last, so both overlays cover it.

### 1.1 The bottom bar (`Nav.kt`)

A Material 3 `NavigationBar`, `containerColor = paper.raised`, zero tonal elevation, four
items and an empty middle slot:

| Slot | Content |
| --- | --- |
| 1 | **TODAY** — sun-over-horizon glyph; badge when captures are waiting |
| 2 | **DAYS** — calendar-page glyph |
| 3 | *empty* — a `Box(weight(1f))`, which is where the shutter sits |
| 4 | **TRIP** — suitcase glyph |
| 5 | **STATS** — three bars |

- Item labels are 11sp, sentence case, tracking 0.2sp: `Today`, `Days`, `Trip`, `Stats`.
  Selected item: `paper.ink` icon and text on a `paper.sunk` indicator.
- The **shutter** is a `FloatingActionButton`: 56dp, `CircleShape`, `paper.blue`
  (`paper.edge` when a trip is closed), aligned top-centre and offset up 20dp, drawn
  aperture glyph (circle, inner ring, six blades).
- **Badge** on Today only: a `paper.blue` circle containing the count, positioned
  `x = 8dp, y = -6dp` from the icon's top end — a numeral, not a dot.
- **Content descriptions**: the bar as a whole is described by
  `Route.stackedFrom()` (`"Preferences, stacked over Today"`, `"Trips, stacked over
  Today"`) or nothing on a destination; the FAB says `"Take a photograph"`,
  `"Take a photograph. N waiting for an amount"`, or
  `"A closed trip takes no photographs"`.

### 1.2 Where the bar is, and where it is not

`showsTabBar()` is one `when`:

| Bar | Routes |
| --- | --- |
| **shown** | `Home`, `Days`, `Stats`, `Trip`, `Record`, `Trips`, `Preferences`, `Search` |
| **hidden** | `Onboarding`, `NewTrip`, `Capture`, `CloseDay`, `TripCard`, `Detail`, `Frame` |

`Route.tab()` maps a route to a tab for highlighting: `Home→Today`,
`Days/Record/CloseDay→Days`, `Trip/TripCard→Trip`, `Stats→Stats`, everything else `null`.
(`CloseDay` and `TripCard` are mapped *and* bar-less; the mapping has no visible effect
there.)

### 1.3 The notice bar (`NoticeBar` in `AriadneApp.kt`)

A full-width strip above whatever is at the foot: hairline, then one row —
the notice text in a tracked `Label` (10sp, `inkSoft`), an optional action
(`QuietAction`, `paper.blue`) and always `close` (`inkFaint`). The row is a
`LiveRegionMode.Polite` live region, so an undo offer is announced.

The only notice in the product today: *"Saved"* with the action **UNDO**.

### 1.4 Where the app opens

`startRoute(hasAnyTrip, hasOpenTrip)`:

| Device state | Opens on |
| --- | --- |
| No trip ever created | `Onboarding` |
| A trip is open | `Home` |
| Trips exist, all closed | `Trips` (the register) |

---

## 2. Navigation

### 2.1 The route set

| Route | Draws | Bar | Reached from |
| --- | --- | --- | --- |
| `Onboarding` | `OnboardingPage` | no | cold start with no trips |
| `NewTrip` | `NewTripPage` | no | Onboarding **BEGIN EMPTY**; register **BEGIN A NEW TRIP**; Trip **START ANOTHER TRIP**; Home no-trip **START A TRIP** (`startEmptyTrip()` = `startNewTrip()`) |
| `Home` | `HomePage` | yes | start, **TODAY**, `Done` on Preferences, register **HOME** |
| `Days` | `TimelinePage` | yes | **DAYS**, Trip **SEE THE DAYS** |
| `Trip` | `TripPage` | yes | **TRIP**, record **TRIP**, Preferences **SEE THE TRIP'S PAGE**, card **BACK TO THE TRIP** |
| `Stats` | `StatsPage` | yes | **STATS** |
| `Record(day?)` | `RecordPage` | yes | Timeline plate, record's day tabs, Home **THE RECORD** row, Timeline **RECORD**, Search **RECORD**, Close day **BACK TO THE DAY**, Detail **LEAVE IT OPEN** |
| `CloseDay(day)` | `CloseDayPage` | no | Home **FINISH N**, Stats **CLOSE TODAY**, record **CLOSE DAY**, the shutter page's **TODAY** |
| `Detail(captureId)` | `DetailPage` | no | any receipt line or photograph; Close day **EDIT**; sheet **OPEN THE WHOLE PAGE** |
| `TripCard(tripId)` | `TripCardPage` | no | Trip **THE CARD** / **CLOSE THE TRIP INTO A CARD**; Home closed **SEE THE CARD**; Preferences **CLOSE THE TRIP INTO A CARD** / **SEE THE CARD** |
| `Trips` | `TripsPage` | yes | Home **TRIPS** row, Home **THE REGISTER**, Trip **THE REGISTER**, Capture page **THE REGISTER**, register itself from `startRoute` |
| `Search` | `SearchPage` | yes | Home **SEARCH WHAT YOU KEPT** row, record **SEARCH** |
| `Preferences` | `PreferencesPage` | yes | Home **PREFS**, Trip **CHANGE THE BUDGET** / **SET A BUDGET** / **THE RATES** |
| `Capture` | `CapturePage` | no | **only** `openCapture()` with no trip or a closed trip, and Escape from the pager (desktop) — legacy (§9) |
| `Frame(captureId)` | `FramePage` as a page | no | **only** a restored route — legacy (§9) |
| `Settings` | `PreferencesPage` | yes | retired name, still resolves |

### 2.2 The stack, Back, Escape

- `go(route)`: moving **between** destinations replaces the stack; moving **into** a page
  pushes the current route onto `history`; any navigation clears both overlays.
- `back()`: closes the sheet → closes the shutter → pops `history` → returns `false`,
  which is the app's cue to leave.
- `canGoBack` = an overlay is open **or** `history` is not empty.
- Escape mirrors Back for overlays; on the pager it leaves for `Route.Capture`.

### 2.3 The two overlays

| State | Drawn by | Opened by | Closed by |
| --- | --- | --- | --- |
| `shutterOpen` | `CaptureOverlay`, full cover | `openCapture()` with an open trip; the FAB; record **CAPTURE**; record **ADD A PHOTOGRAPH**; Home **TAKE A PHOTO** | **CLOSE**, Back, Escape, taking a frame |
| `sheetCaptureId` | `FramePage`, bottom sheet at 84% height | `capturePhoto()`; `importPhoto()` | back chevron, **ADD THE AMOUNT LATER**, **SAVE …**, **OPEN THE WHOLE PAGE**, Back, Escape |

### 2.4 Desktop keys (`handleKey`)

| Key | Where | Effect |
| --- | --- | --- |
| Escape | anywhere, overlay open | closes the overlay |
| Escape | `CloseDay` | `go(Route.Capture)` |
| Enter / NumPadEnter | `CloseDay` | `confirmCurrent()` |
| J / ↓ · K / ↑ | `CloseDay` | `next()` · `previous()` |
| 0–9, `.` | `CloseDay` | type into the amount |
| Backspace | `CloseDay` | delete a digit |

No other page responds to keys.

---

## 3. The flows, with the controls named

### 3.1 First run

`Onboarding` → **BEGIN WITH THE DEMO ROLL** (`startTripDemo()`) *or* **BEGIN EMPTY**
(`startNewTrip()` → `NewTrip` with *from* = today, *to* = today + 6, currency = the
setting) → **OPEN THE TRIP** (`commitDraft()`) → `Home`.

### 3.2 The everyday loop

1. FAB (or record **CAPTURE**) → `CaptureOverlay` over the same page.
2. Tap the plate → `capturePhoto()` → camera closes, `FramePage` rises over the same page.
3. Type a figure → **SAVE ₹18.41**, or **ADD THE AMOUNT LATER**. Either closes the sheet
   and leaves the person exactly where they were.

Nothing navigates.

### 3.3 Settling a day

Home **FINISH N** / record **CLOSE DAY** / Stats **CLOSE TODAY** → `CloseDay`, one card
at a time: **YES, SAVE IT** or **NO, LET ME ENTER IT**; **CONFIRM**; **KEEP IT WITHOUT AN
AMOUNT**; **EDIT**; **EARLIER**/**LATER**; **CONFIRM N READY**; **CONFIRM REST AS NO
AMOUNT**; **CLOSE THE DAY** → the summary → **BACK TO THE DAY**.

### 3.4 Reading the record

`Days` → a `DayPlate` per day → `Record(day)`: receipt lines → `Detail`; **CLOSE DAY**;
**SEARCH**; **TRIP**.

### 3.5 The card

`Trip` → **CLOSE THE TRIP INTO A CARD** (or **THE CARD**) → the card, tappable to copy
(**SHARE THE CARD** → **COPIED**) → **BACK TO THE RECORD** or **BACK TO THE TRIP**.

### 3.6 Registers, settings, switching

`Home` → **TRIPS** row → `Trips` → tap a row (`switchTrip`) → **HOME**.
`Home` → **PREFS** → `Preferences` → **DONE**.

---

## 4. State the UI reads

| Field | Read by |
| --- | --- |
| `route`, `history`, `canGoBack` | the shell |
| `shutterOpen`, `sheetCaptureId` | the shell (overlays) |
| `theme` | `AriadneApp` → `AriadneTheme` |
| `trip` | every page |
| `today` | Home, Record, CloseDay, Preferences, Search |
| `dayCards` | Home rail, Timeline, Record, Trip, Stats' "Close today" |
| `dayCaptures`, `dayTotalMinor`, `dayCurrency`, `perCurrency`, `dayUnconverted`, `dayRateDates` | Home, Record, CloseDay, Detail, Stats |
| `stack`, `stackIndex`, `amountInput` | the pager, Home, the badge |
| `dayCapturesAll`, `dayFigures` | Timeline, Trip, Stats, the sheet's lookup |
| `categoryTotals` | Stats, Trip, Home |
| `detail`, `detailAmount/Merchant/Note/Category` | Detail, the sheet |
| `searchQuery`, `searchResults` | Search |
| `trips`, `activeTripId` | Trips, Trip, Home |
| `draft` | NewTrip |
| `railDocked` | every tab page |
| `notice` | the notice bar |
| `busy`, `rateError`, `failureNote` | Preferences, the sheet |
| `deleteArmed`, `cardCopied` | Preferences, TripCard |

---

## 5. Every surface, in full

Each tab page is framed by `TabPage`: a scrolling column, the hanging margin on a wide
window with a scroll rail, and a bottom spacer of 96–104dp so the bar never covers the
last row. Non-tab pages use `ScrollingMargin` or a bare column.

### 5.1 Onboarding — `OnboardingPage.kt`, no bar

Wide windows get a left margin column; phones get a single column.

| Element | Text |
| --- | --- |
| eyebrow (wide: a rail of labels) | `Ariadne` (tracking 3sp), `edition one`, `a trip ledger`, `no account` |
| headline, line 1 | **The photograph / is the memory.** (40sp phone, 66sp wide) |
| headline, line 2 (italic, `inkSoft`) | **The numbers wait for night.** |
| rule | `paper.ink` |
| paragraph | "Forty rupees on chai, twelve hundred on shoes, and by tonight neither is remembered. Point the camera at the receipt, the stall, the ticket. When the day is done, sit down and settle it - two taps a card." |
| primary | **BEGIN WITH THE DEMO ROLL** (`startTripDemo()`), max width 360dp |
| secondary | **BEGIN EMPTY** (`startNewTrip()`) |
| foot | `No account · no server · no sync` |

### 5.2 New trip — `NewTripPage.kt`, no bar

`Masthead("", eyebrow = "Ariadne" \| "another trip", title = "Where to", lede = "A name, the dates, and a figure to stay under. The fence is optional.")`

Fields, in order — each a labelled hairline set in the serif:

| Label | Placeholder |
| --- | --- |
| `the trip` | `Bangkok & Home` |
| `where` | `Bangkok — or a coast, or a train` |
| `from` | `2026-11-24` |
| `to` | `2026-11-30` |
| `currency` | a `CurrencyField` (see §5.16) |
| `budget, if you have one` | `25000` |

Then:

- **OPEN THE TRIP** (`commitDraft()`), the only blue control;
- the draft's error line, when there is one (it wraps; e.g. *"The trip could not be
  saved. … — nothing was lost; try again."*);
- `SectionHead("II", "or start with something in it")` with the paragraph "Five
  photographs of one Bangkok afternoon, already taken — you land on the trip with five
  photographs waiting for an amount.", then **DEMO ROLL** (`commitDraft(withDemoRoll =
  true)`) and **PREFILL FROM DEMO** (`fillDraftFromDemo()`);
- the foot `no account · no server · no sync`;
- **GO BACK** → `Home` (only when a trip already exists).

### 5.3 Home — `HomePage` in `TabPages.kt`, Today tab

**Header.** `Masthead(01, "Ariadne", title = the trip's name \| "No trips yet" \| "No trip open", italic, trailing = PageAction("Prefs") → Preferences)`.

**Rail (wide).** `today` = today's ISO or `—`; `days` = the trip's length; `closed N`;
`waiting` = `stack.size`; `photographs`.

**Open trip, top to bottom:**

1. the day's figure: currency symbol (22sp, `inkFaint`) + `Money.format` at 52sp (phone) /
   68sp (wide);
2. row: `finished today` (Label, `inkSoft`) — pill **ALL SETTLED** (`PillTone.Quiet`)
   or **N WAITING** (`PillTone.Blue`);
3. **TAKE A PHOTO** (`openCapture()`, accent, shutter glyph; weighted 1.5) and, when the
   stack is non-empty, **FINISH N** (`openCloseDay(today)`, outline; weighted 1);
4. **OR BRING ONE IN FROM THE LIBRARY** — only when `pickerAvailable()`; opens the
   platform picker and calls `importPhoto(bytes)`;
5. `SectionHead("II", "the trip", trailing = "start → end")`, then five `NavRow`s, each
   revealed with a staggered animation (delays 0…4) and separated by rules:

| Row label (serif) | Detail (right, sans) | Lands on |
| --- | --- | --- |
| `Trips` | `the register` (one trip) / `N in the register` | `Trips` |
| `Timeline` | `N of M days closed` | `Days` |
| `The record` | `N kept today` | `Record(today)` |
| `Search what you kept` | `by shop, price, day` | `Search` |
| `The trip's page` | `dates, currencies, the card` | `Trip` |

6. closing line, `inkFaint` 12sp: "No account. No sync. The record stays here."

**No trip.** Body — *"A journey, from its first day to its last. Photographs now; figures
at nightfall."* (no trips) or *"Every trip on this device is closed. Open one from the
register, or begin another."* (all closed) → **START A TRIP** (`startEmptyTrip()`) →
**BEGIN WITH THE DEMO ROLL** (`startTripDemo()`) or **THE REGISTER** (`Trips`).

**Closed trip.** Body — *"This trip is closed. Nothing new can be filed against it; the
record and the card stay exactly as they were."* → **SEE THE CARD** → **THE REGISTER**.

### 5.4 Timeline — `TimelinePage` in `TabPages.kt`, Days tab

**Header.** `Masthead(02, eyebrow = trip name \| "Timeline", title = "Timeline", lede = "Each day, and the hours it was spent in.", trailing = PageAction("Record") → Record(today))`.

**Rail.** `closed N`, `open N`, `the trip as a run of days`.

**Body.** One `DayPlate` per day of the trip — every day, empty ones included — each
tap opening `openRecord(day)`:

- spine: the day number (30sp numeral), the month (`Jan`…), the weekday (`Mon`…);
- the state word, right-aligned against the day's figure: `in the record` (`inkFaint`)
  when closed, `settled`, `1 to settle`, or `N to settle` (`paper.blue`);
- the figure (`₹1,240.00`), shown only when the day has captures;
- `DayBand` — a 54dp canvas of the 24-hour rail: one mark per capture at its hour
  (filled ink when confirmed, an outlined blue ring when still open; two captures in one
  hour stack upward), hour ticks at `00 06 12 18 23`, and the legend `Recorded` /
  `Still open`;
- a day with no captures instead draws a hairline and `nothing kept`.

**Empty.** *"A trip has no days until it has dates."* + **START A TRIP**.

### 5.5 Record — `RecordPage.kt`, stacked over Days

**Header.** `Masthead("", eyebrow = trip name \| "Record", title = "The record", trailing = PageAction("Capture") → openCapture())`.

1. the day being read as a serif `Title` at 24sp: `2026-11-27` or **No day yet**;
2. a horizontally scrolling strip of day tabs (day number + month; closed days on
   `paper.sunk`, selected tab with an ink border) — each tab opens `Record(day)`;
3. the day's receipt lines: index, photograph thumbnail, title, state label
   (`Needs an amount`, `Confirmed`, …), the figure or `—`; each → `Detail(id)`;
4. if the day is empty: *"Nothing kept on this day. An empty day is still part of the
   trip — it is the one you spent walking."* + **ADD A PHOTOGRAPH** (`openCapture()`);
5. a rule, then the day's per-currency rows (`THB ฿2,555.00`), then **Tallied** with the
   day total, prefixed `≈` when anything was converted;
6. `ConversionNote`: `≈ converted at the rate for 20 Nov` (one rate) or
   `≈ converted at the rates in force 18 Nov – 20 Nov`, and for money that could not be
   converted, `฿2,555.00 not counted — no THB rate yet`;
7. when captures are still open: *"1 card is still open. Nothing is in the total until
   you close the day — the figures above are what the photographs said, not what you have
   confirmed."* (plural: `N cards are still open. …`);
8. **CLOSE DAY** (`openCloseDay(shown)`, accent; disabled when the day or the trip is
   closed);
9. **SEARCH** → `Search`, **TRIP** → `Trip`;
10. footnotes and the second closing control:
    - trip closed: *"This trip is closed. The record is readable and its card is yours;
      nothing can be added to it."*
    - day closed: *"This day is closed. Its cards can still be edited from here."*
    - day open: **CLOSE THIS DAY** (`closeDay(shown)`).

### 5.6 Close day — `CloseDayPage.kt`, no bar

**Top.** A row: `Title("Close day", 30sp/40sp, italic)` on the left, and on the right
`spent` + the day's figure (19sp); under it the day — `15 Nov` or `today` — then the card
counter `plate 3 of 7` with **EARLIER** (`previous()`) and **LATER** (`next()`).

**The card.**

- the photograph, or the engraved plate when there is no image;
- the merchant title (`Receipt · 21:09` when nothing better is known);
- the state line: `kept without an amount` / `nothing read — type it, or leave it` /
  `read from the photograph`;
- **EDIT** → `Detail(captureId)`;
- `Amount` + the figure (or `0`) + the ruled keypad (digits, one decimal point,
  backspace; each key a `contentDescription` of its own label);
- when extraction offered a figure: *"Is it ₹1,240.00?"* → **YES, SAVE IT**
  (`confirmCurrent()`) / **NO, LET ME ENTER IT** (`rejectCurrentExtractedAmount()`);
- otherwise **CONFIRM** (`confirmCurrent()`);
- **CONFIRM N READY** (`confirmReadyInStack()`) with the note *"Keeps every photograph
  that already has an amount; the rest stay in the stack."*;
- **KEEP IT WITHOUT AN AMOUNT** (`confirmCurrent(noAmount = true)`) — reads **NO AMOUNT**
  in the narrow variant;
- **CONFIRM REST AS NO AMOUNT** (`confirmRestAsNoAmount()`);
- **CLOSE THE DAY** (`closeDay(day)`) with the note *"Closing files the day. Any card can
  still be edited from the record."*;
- footer row: `N of M` and **NEXT PHOTOGRAPH** or **CLOSE THE DAY** (`remaining > 1`).

**When nothing is left.** `Title` — **Nothing left to settle** / **N left to settle**;
the day's figure + `on 15 Nov`; the conversion note; four summary rows —
`photographs`, `confirmed` (confirmed + in-record), `no amount`, `still open`; then
**CLOSE THE DAY** (`closeDay(day)`, accent) and **BACK TO THE DAY** (`openRecord(day)`),
or, when the trip is closed, the line *"This trip is closed, so there is no day left to
file."* and **BACK TO THE DAY** alone.

### 5.7 Detail — `DetailPage.kt`, no bar

**Header.** `Masthead("", eyebrow = the capture's ISO date \| "Photograph", title = "The photograph", trailing = QuietAction("Back") → closeDetail())`.

1. the photograph, large;
2. `Amount` + the figure (or `0`) and the keypad, or, when the row has an unconfirmed
   amount, *"Is it ₹1,240.00?"* + **YES, SAVE IT** (`saveDetail()`) / **NO, LET ME ENTER
   IT** (`rejectDetailExtractedAmount()`);
3. currency chips — the shortlist, each with `contentDescription` "Currency SGD", "the
   one on this photograph", or "not changeable on a closed trip";
4. `Where` — placeholder *"the shop, if you want it named"*;
5. `What it was` with the note *"optional · nothing is sorted for you"*, then the six
   category chips in two rows (`FOOD`, `TRANSPORT`, `LODGING`, `SHOPPING`,
   `ENTERTAINMENT`, `OTHER`; a chosen chip is filled ink, an unchosen one is a bordered
   tag on raised paper);
6. `Remember it by` — placeholder *"a line for later"*, serif;
7. actions: **SAVE** (`saveDetail()`, accent), **KEEP IT WITHOUT AN AMOUNT**
   (`saveDetail(keepWithoutAmount = true)`), **LEAVE IT OPEN** (`openRecord(day)`);
8. on a closed trip the fields print instead of typing, with the paragraph *"This trip is
   closed. The photograph and its figures stay exactly as they were; nothing new can be
   filed against a finished journey."*

**Missing capture.** *"That photograph is not in the record any more."* + **BACK TO THE
RECORD** (`closeDetail()`).

### 5.8 The sheet — `FramePage.kt`, overlay (or the legacy page)

Bottom sheet at **84% height** over the photograph and a scrim, or, on a wide window, a
two-column spread: the print on the left, the fields on the right, actions under them.

**Header row.** A drawn back chevron (44dp target, `contentDescription = "Back"`, calls
`keepFrameForLater()`), then `the photograph is kept` (8sp, `inkGhost`), then a pill with
the entry status: `Needs an amount` / `Not confirmed` / `Confirmed` / `No amount` / `In
the record` / `Couldn't read it` (blue when pending, quiet otherwise).

**Body, top to bottom:**

1. **The moment** — a 62dp thumbnail; `TUESDAY 11 NOVEMBER · 05:30`; the figure as it
   stands — `S$` + `18.41` (30sp), or `0` plus `nothing typed yet` when empty.
2. **The suggestion** (only with an unconfirmed amount and nothing typed) — *"Found
   S$18.41 on it."* + **YES** (`acceptDetailSuggestion()` — saves immediately) / **NO**
   (`rejectDetailExtractedAmount()`).
3. **The pair** — a sunk, bordered block:
   - labels: `original purchase` … `base trip equivalent`;
   - figures: `S$18.41` `≈` `₹1,373.94` (19sp each; `—` where unknown);
   - one line underneath: `rate: 1 SGD ≈ 74.6298 INR`, or `already in the trip's
     currency`, or `no SGD rate yet — it will be counted in SGD`, or `type an amount and
     the pair updates`.
4. **READ THE PHOTOGRAPH AGAIN** (`reExtractCurrent()`), reading **READING THE
   PHOTOGRAPH…** and disabled while `busy`.
5. **Amount** — `amount` label; a 56×52 box with the currency symbol and a field beside
   it showing the typed figure (26sp) or the placeholder `how much`; under the field,
   `will convert to the trip's currency (INR) on save.` or `already in the trip's
   currency, INR.`; then the keypad (46dp rows, 19sp digits).
6. **Currency** — `currency` label + the shortlist chips.
7. **Category** — `category` label + the six chips.
8. **Merchant** — label `Merchant`, placeholder *"the shop, if you want it named"*.
9. **Remember it by** — placeholder *"a line for later"*.
10. **Date** — `date` label; a bordered row `11/11/2025` · `05:30` · `as photographed`.
    Printed, not editable.

**Foot, pinned.** **SAVE 18.41** (`saveDetail()`, accent) + **ADD THE AMOUNT LATER**
(`keepFrameForLater()`), or **ADD THE AMOUNT LATER** alone when nothing is typed; then
**OPEN THE WHOLE PAGE** (`openFrame()` → `Detail`).

**Missing capture.** *"That photograph is not here any more."* + **BACK TO TODAY**.

### 5.9 The shutter — `CaptureOverlay.kt`, full cover

| Element | Text |
| --- | --- |
| top-left | **CLOSE** (`closeShutter()`) |
| top-centre | the trip's name, or `Ariadne` |
| plate | live preview, or the engraved plate; `contentDescription = "Viewfinder. Tap to keep the photograph."`; tapping fires the camera |
| hint | `Tap the frame to keep it` — or `No camera here — tap the frame for a plate` when the platform supplies none |
| left of the shutter | **FROM THE LIBRARY** (`rememberPhotoImportMany` → `importPhotos`) |
| right of the shutter | `nothing waiting` / `N waiting` |
| under the shutter | `no figure here · the amount comes after` |

**No trip / closed trip.** Title — *"This trip is closed."* or *"No trip is open."*;
body — *"Its photographs are still here, and its card is still yours."* or *"A photograph
needs a journey to belong to."*; **THE REGISTER** → `Trips`.

### 5.10 The viewfinder page — `CapturePage.kt`, no bar (legacy, §9)

Top row: **HOME** (`Route.Home`), the trip's name or `Ariadne`, **TODAY**
(`openCloseDay(today)`). Same plate and shutter as the cover, hint `Tap the frame to keep
it` / `No camera here — tap for a plate`, **FROM THE LIBRARY**, `Nothing waiting` /
`N waiting`, and `no figure here · settle it later`.

No trip: *"No trip is open. A photograph needs a journey to belong to."* + **THE
REGISTER**. Closed: *"This trip is closed. Its photographs are still here, and its card is
still yours."* + **SEE THE CARD** + **THE REGISTER**.

### 5.11 Stats — `StatsPage` in `TabPages.kt`, Stats tab

**Header.** `Masthead(03, eyebrow = trip name \| "Stats", title = "What it came to")`.
**Rail:** `the trip` (its figure), `photographs` (count), `finished only`.

1. `the trip so far` + the trip's figure (50sp/64sp), animated to its value;
2. a rule at `paper.ink`, then `finished photographs only`;
3. when a budget exists: `against a budget of` + the figure + `left`/`over` + the
   difference (blue when over);
4. the note *"Only photographs that were finished. Anything still waiting tonight is not
   in this figure, and is not counted as zero."*;
5. `SectionHead("II", "what it was")` — a `Breakdown` by category (`Food`, `Transport`,
   … or `Uncategorised`; `N photographs`; the figure; `of`), a `PatternLine` sentence
   (*"Most of this trip so far went on food — 34% of it, across 6 photographs. Everything
   else is in the column above."*), and the note *"Nothing here is sorted for you. A
   category is a claim you make on the photograph, and most photographs never need
   one."*;
6. `SectionHead("III", "by currency, before conversion")` — a row per currency with a
   tick, the code and the figure, then `these are the amounts as they were paid, not
   converted`;
7. if nothing is tallied: *"Nothing has been tallied yet. The figures appear here the
   first time you finish a photograph at nightfall."*;
8. **CLOSE TODAY** (`openCloseDay(today)`, accent; enabled only when the stack is
   non-empty, today is open and the trip is open).

### 5.12 Trip — `TripPage.kt`, Trip tab

**Rail.** `spent`, `days closed` (`N of M`), `since`.

1. `trip` label; the name (34sp/44sp italic); a place glyph + the place when set; the
   dates `2026-11-24 — 2026-11-30 · 7 days`; a `closed` pill when closed;
2. `spent` + the figure + `spent`; when a budget exists, `of ₹50,000.00 · ₹8,240.00 left`
   (blue and `over` when negative); the `BudgetBar` with its pace tick; when ahead of
   pace, `ahead of the trip's own pace`; **CHANGE THE BUDGET** or **SET A BUDGET** →
   `Preferences`;
3. a rule; `how far`; **N of M days closed** (22sp); a measured rule; **SEE THE DAYS** →
   `Days`;
4. a rule; `what it was spent in`: one row per currency with its subtotal, then `all of
   it` and the total; empty: *"Nothing tallied yet. The first capture is a photograph,
   not a form."*;
5. `currency` + the paragraph *"Base INR · home INR. The original amount is never
   converted away — it stays under the figure it became."* + **THE RATES** →
   `Preferences`;
6. `also on this device` (only with more than one trip) — a row per other trip
   (`name`, `start — end`, `open`) → `switchTrip`;
7. actions: **THE CARD** (closed) or **CLOSE THE TRIP INTO A CARD**; **START ANOTHER
   TRIP** (`startNewTrip()`); **THE REGISTER**.

### 5.13 The register — `TripsPage.kt`, stacked

**Header.** `Masthead("", eyebrow = "The register", title = "Trips", lede = "Every journey this device holds. One of them is open.", trailing = PageAction("Home"))`.

- three shelf figures: `journeys`, `open`, `closed`;
- the line *"One at a time. A photograph taken now belongs to the open journey."*;
- numbered rows: month blocks, the name, `no place` when unset, `15 Oct — 22 Oct`,
  the budget or `—`, and a state word — `closed`, `open`, or `open, the one photographs
  go into` (the active trip is the page's one blue element). The row's
  `contentDescription` is `"name, dates, state"`;
- empty state: **No trips yet.** — *"A trip is the container the whole record hangs from:
  its dates, its money, and every photograph you take during it."*;
- **BEGIN A NEW TRIP** (`startNewTrip()`) and **OR OPEN THE DEMO ROLL**
  (`startTripDemo()`).

### 5.14 The card — `TripCardPage.kt`, no bar

A single column of type, the whole card a tap target (`copyCardToClipboard()`):

- `A trip, kept`; the name; the dates; the days as rows with `closed`/`open`;
- *"No figures recorded yet. The photographs are still the record."* when nothing is
  tallied, else the per-currency lines and `Total`;
- the closing sentence — *"Every day closed. This is the whole of the trip: no account, no
  sync, and nothing about it left this device."* or *"N of M days closed. The card keeps
  filling itself in until the trip is done."*;
- **SHARE THE CARD**, which becomes **COPIED** for a moment (`state.cardCopied`);
- **BACK TO THE RECORD** (`Record(today)`) and **BACK TO THE TRIP** (`Trip`).

### 5.15 Search — `SearchPage.kt`, stacked

**Header.** `Masthead("", eyebrow = "The record", title = "Remember a thing", trailing = PageAction("Record") → Record(today))`.

- an empty field showing the serif example `shoes · 1200 · THB · 21 Nov`, described as
  "Search the record";
- before any query: the paragraph *"Type a shop, a price, a currency, or a day.
  Everything you kept is in here - including the things you left without an amount."*,
  then `SectionBreak("how you can ask")` and four examples — `By the shop` "shoes", `By
  the figure` "1200", `By the currency` "THB", `By the day` "21";
- with results: `N kept` and a row per result (photograph, title, figure or `—`) →
  `Detail(id)` with `DetailOrigin.Search`;
- no results: **Nothing by that name.** — *"Try a price, the shop, or the day it
  happened. Search reads what you typed on the photograph, and what the recogniser read
  off it."*

### 5.16 Preferences — `PreferencesPage` in `TabPages.kt`, stacked

**Header.** `Masthead(04, "Ariadne", title = "Preferences", trailing = PageAction("Done") → Home)`.
**Rail:** `on device only`, `no account · no sync`.

**I · the record**

| Row | Type | Note under it |
| --- | --- | --- |
| `Base currency` | `CurrencyField` → `editTripCurrency` | `Every total is stated in this one` |
| `Home currency` | `CurrencyField` → `editTripHomeCurrency` | `What a foreign amount is converted back to` |
| `Dates` | read-out (`NavRow` with a no-op) | value: today's ISO date + `, ISO order` — e.g. `2026-11-27, ISO order`; the fallback literal in source is `2026-11-23` |
| `Paper` | three chips | `Light paper, always` · `Follows this device's light or dark setting` · `Night paper: the same design on dark stock` |

The Paper chips read `Light`, `System`, `Night` and call `setTheme(...)`; the chosen one
is drawn in `paper.blue`.

**II · the exchange.** One row per currency in use, plus `THB`, `USD`, `EUR`, `GBP`,
`JPY`. Each row, top to bottom:

- a budget glyph, the currency **code** as a serif `Title` (e.g. `SGD`), a blue tick when
  the currency is actually in use, and **CLEAR** (`clearFxRate(code)`) — only when a rate
  is on file;
- the sentence `1 SGD = 74.6298 INR`, or `no rate on file — this currency is not being
  converted` (in use) / `no rate on file` (not in use);
- **SET RATE** (no rate) or **CHANGE** → opens the one open editor: `1 SGD =` + a field
  (`rate` label beside it) and **SET** / **CANCEL**; a refused value reads `not a number`;
- the row is followed by a rule; only one editor can be open at a time.

With nothing foreign: *"Everything so far is already in the base currency."* A rejected
rate is stated under the list (`state.rateError`).

**III · what leaves this device.** `Reading` read-out: `On device only`. Then the
paragraph: *"No account, no server, no sync. Photographs are stored in the app's private
folder and the record lives in a local database. The one thing that leaves this device is
a request for today's exchange rates, and it carries nothing about the trip; the app works
without it and keeps the last rate it saw."* If anything has ever failed:
`SectionBreak("something failed")` + the last error, with *"This is the last error the app
saw. Nothing was lost; it is written here because a failure that only happens in a log is
a failure nobody fixes."*

**IV · this trip.** The trip's name (or `No trip`), the place (or `no place set`), then
icon rows: **SEE THE TRIP'S PAGE**; **CLOSE THE TRIP INTO A CARD** (`closeTripIntoCard()`)
or **SEE THE CARD** when already closed; **DELETE THIS TRIP** (`armTripDeletion()`) with
the paragraph *"Deletes its photographs and its record. Nothing is kept anywhere else, so
this is the one action in the app you cannot take back."* Armed, the block becomes
`this deletes everything, now` (blue label) and **DELETE FOR GOOD** (`deleteTripNow()`) /
**KEEP IT** (`armTripDeletion()`).

---

## 6. Shared strings and formatting

**Money.** `Money.formatWithSymbol(currency)` = symbol + `format`, e.g. `₹82,141.92`,
`S$18.41`. Grouping is Indian lakh style for `INR, NPR, LKR, PKR, BDT`, three-digit groups
elsewhere; scale follows the currency (JPY has none). Negative figures carry a `-`
sign; the app prefers `₹8,240.00 left` / `₹8,240.00 over` over a negative numeral.

**The `≈`.** Printed before a figure that contains converted money, and in the sheet's
pair between its two figures. The rate date always accompanies it:
`≈ converted at the rate for 20 Nov`.

**Capture titles.** `MerchantName.receiptTitle(merchant, "21:09")`: a cleaned merchant
name, else `Receipt · 21:09`, else `Receipt`. Never a raw OCR line, never "Untitled".

**Entry status labels.** `Needs an amount`, `Not confirmed`, `Confirmed`, `No amount`,
`In the record`, `Couldn't read it`.

**Dates.** ISO (`2026-11-27`) in fields and rows; spoken form `27 Nov` in sentences;
`TUESDAY 11 NOVEMBER` in the sheet's head; `11/11/2025` in the sheet's date field;
times as 24-hour `HH:MM`.

**Empty states (verbatim).** `No trips yet.` · `nothing kept` (a day on the timeline) ·
`Nothing kept on this day. An empty day is still part of the trip — it is the one you
spent walking.` · `Nothing by that name.` · `Nothing has been tallied yet. The figures
appear here the first time you finish a photograph at nightfall.` · `Nothing tallied yet.
The first capture is a photograph, not a form.` · `Everything so far is already in the
base currency.` · `Nothing left to settle` · `No figures recorded yet. The photographs are
still the record.`

**Error copy.** *"The trip could not be saved. … — nothing was lost; try again."* ·
*"Your trip could not be opened. …"* · *"That trip could not be opened. …"* · `not a
number` (rate field) · `no rate on file — this currency is not being converted`.

**Accessibility descriptions written in source.** `"Take a photograph"` (and
`"Take a photograph. N waiting for an amount"`, `"A closed trip takes no photographs"`) ·
`"Viewfinder. Tap to keep the photograph."` · `"Back"` (the sheet's chevron) ·
`"Search the record"` · `"Search currencies"` · `"Camera viewfinder"` ·
`"Backspace"` and every keypad digit · the currency chips' three variants
(`"Currency SGD"`, `"Currency SGD, the one on this photograph"`,
`"Currency SGD, not changeable on a closed trip"`) · register rows,
`"name, dates, state"` · the bar's `"Trips, stacked over Today"` /
`"Preferences, stacked over Today"`. There is no settings glyph on Home in this build —
settings is the `PREFS` page action in the masthead.

---

## 7. Cross-cutting behaviour

**Type.** Instrument Serif for titles (sentence case, tight tracking above 36sp); sans
for labels (tracked uppercase, 8–11sp) and running text; mono with tabular figures for
money. Money never passes through `Label`, which ellipsises.

**Colour.** Paper, ink (a five-step ladder `ink` → `inkGhost`) and exactly one blue per
surface: the primary action (`ClaimAccent` records the claim), the waiting badge, the
over-budget figure. Night paper is the same design on dark stock.

**Rules.** 1dp hairlines are the structural device. No shadows. Corners are 1–2dp; only
the FAB, the badges and the shutter's ring are round.

**Motion.** `Reveal` (Home's five rows, staggered) and `animatedLong` (Stats' figure)
respect reduced motion and settle on the first frame when it is on.

**Theme.** Light / System / Night, chosen in Preferences, persisted in settings.

**Insets and length.** The shell applies window insets; tab pages end with a 96–104dp
spacer so the bar and FAB never cover the last row.

**Not verified.** Nothing below has been seen on a screen: no layout, spacing, contrast,
motion or 200%-font check has been done for any surface in this document. See §11.

---

## 8. Platforms, as wired

| | Camera | Photo picker | OCR | Rates | Storage | Review |
| --- | --- | --- | --- | --- | --- | --- |
| **Android** | CameraX binding via `LocalCamera` | `PickMultipleVisualMedia(12)` | ML Kit on-device | `HttpURLConnection` → frankfurter.app, 5s timeouts | SQLDelight + private files | none |
| **Desktop** | none — the engraved plate | system file dialog | Tess4J | none | SQLDelight (`~/.ariadne/ariadne.db`) | none |
| **Wasm** | none | browser file input | optional `window.ariadneRecognize` hook, else a polite failure | none | in memory | none |
| **iOS** | none | photo library | Vision | none | native SQLite | StoreKit |

Visible consequences: where no camera is bound the plate reads **No camera here — tap the
frame for a plate**; on Wasm nothing survives a reload; only Android can fetch a rate.

---

## 9. Legacy, unreachable, absent

**Kept and compiling, effectively unreachable:**

- `Route.Capture` → `CapturePage` (the standalone viewfinder page). Reached only by
  `openCapture()` when there is no trip or the trip is closed, and by Escape from the
  pager on desktop.
- `Route.Frame(captureId)` → `FramePage` as a full page. `openFrame()` now closes the
  sheet and opens `Detail`, so only a restored route lands here.
- `Route.Settings` → `PreferencesPage` (retired name).

**Not implemented anywhere in the UI:** deleting a single photograph (trips can be
deleted, captures cannot); editing a capture's date; voice capture; SMS import; bill
splitting; accounts, sync, analytics; sharing beyond copying the card's text; multiple
photographs per capture; crop or rotate; a "More" tab; an in-app settings icon on Home
(settings is the `Prefs` page action); onboarding steps (it is one page with two exits).

---

## 10. Where the code and its own copy disagree

1. ~~**Preferences §III claimed "there is no network call in the product at all".**~~
   **Fixed.** It was false: `refreshRates()` GETs a public rate feed through the
   platform's `RateSource`. The paragraph now states exactly what leaves the device; the
   foot lines of Onboarding and New trip ("nothing leaves this device") now say "no sync";
   and the README's claim that the manifest declares no `INTERNET` permission is
   corrected — it does, for that one request.
2. **Stats' "by currency, before conversion" reads `state.perCurrency`**, which
   `refreshDay()` fills from *the opened day* — so under a trip-wide total it can show a
   day's money. Recorded, not fixed.
3. **Home's rail and the Today badge count different scopes** — `stack.size` (the opened
   day) versus `tripPending` (every pending capture in the trip). They disagree on a trip
   with older unsettled days. Recorded, not fixed.

---

## 11. Verification status

| Claim | Status |
| --- | --- |
| `:core:model` tests | 47 green (run without Gradle via `tools/bootstrap-toolchain.sh`) |
| `:core:fx` tests | 38 green, same way |
| The shared tree compiles (syntax, conflicting declarations, non-exhaustive `when`s) | checked with `kotlinc` over `core/*` + `ui/app` + `preview` |
| All four targets build; shared tests pass | done by CI: `jvmTest`, `:app:android:assembleDebug`, `:app:wasm:wasmJsBrowserDistribution`, desktop |
| `FlowCheck` (stack, overlays, undo, the sheet's pair) | written; **never executed** |
| Any screen rendered, measured, contrast-checked, or read at 200% | **no** |
| Stills | `preview/Shots.kt` defines them; `docs/shots/` holds only the pre-redesign archive |

The gate for every visual claim on this page is `tools/verify-ui.sh` (and
`tools/shoot.sh` for stills) on a machine with Gradle and a display.
