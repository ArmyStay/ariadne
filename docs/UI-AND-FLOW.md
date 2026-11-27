# Ariadne — the interface, surface by surface, as built

**What this document is.** A source-level description of the Ariadne interface and
flows. The navigation and route details below reflect the current Home / Timeline /
Stats / More structure, manual entry, profile settings, and local data export. Device
layout and platform save/share behavior still need human verification.

**Audited at** `d6f6fe0` (main, after the data-export pass), 2026-11-27. **Second revision:** every
surface lists its blocks in order, with the exact words on every label, button, pill,
placeholder, empty state and note, and the control each one calls. **Third revision
(phase 8):** the timeline was rebuilt to the journey reference — globe backdrop,
`MarkHead` sections, a ledger spine of days — and §5.4 was rewritten. **Fourth
revision (phase 9):** the photograph sheet was rebuilt to the receipt-viewer reference —
a torn card holding the photograph, an ink primary — and §5.8 was rewritten.

**What it is not.** Not a roadmap, not a changelog, and not a claim that any of this has
been looked at on a screen. No screenshot in this repository was produced by this build:
`docs/shots/` holds only `docs/shots/archive-2026-11-26-pre-third-review/`.

**How strings are quoted here.** Exactly as they appear in the source. Two rules of the
design system change how they appear on screen and are worth stating once:

- **`Label`, `Action`, `OutlineAction`, `QuietAction`, `PageAction`, `Pill`, `StatusTag`
  and `MarkHead` uppercase their text.** So `Label("finished today")` renders **FINISHED TODAY**, and
  `Action("Create a trip")` renders **CREATE A TRIP**. The strings below are quoted in
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
| **Capture** | One expense record. It may have a photograph or be entered manually; photo captures move through `QUEUED → EXTRACTING → NEEDS_AMOUNT / NEEDS_CONFIRMATION → CONFIRMED → ARCHIVED`. |
| **Stack** | The opened day's captures that still owe something, in pager order. Counted on Home and settled by Close Day. |
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
5. the **bottom dock**, if the route supports it and neither capture overlay is open.

The shutter and sheet are layered over the page, not placed as full-height siblings in
its content column. The sheet draws after the shutter, and the dock is hidden under either
overlay.

### 1.1 The bottom dock (`Nav.kt`)

A Material 3 `NavigationBar`, `containerColor = paper.raised`, zero tonal elevation, four
destinations around the central shutter's reserved slot:

| Slot | Content |
| --- | --- |
| 1 | **HOME** — today's record and recent expenses |
| 2 | **TIMELINE** — trip overview and its days |
| 3 | *empty* — a reserved central seat for the shutter |
| 4 | **STATS** — spending breakdown and daily totals |
| 5 | **MORE** — Trips, manual entry, profile/settings, privacy, and terms |

- Item labels are 11sp, sentence case, tracking 0.2sp: `Home`, `Timeline`, `Stats`,
  `More`. Settings is reached from More rather than occupying its own tab.
- Selected item: `paper.ink` icon and text on a `paper.sunk` indicator.
- The **shutter** is a `FloatingActionButton`: 56dp, `CircleShape`, `paper.blue`
  (`paper.edge` when a trip is closed), aligned top-centre and offset up 20dp, drawn
  aperture glyph (circle, inner ring, six blades).
- **Badge** on Timeline only: a `paper.blue` circle containing the trip-wide waiting count, positioned
  `x = 8dp, y = -6dp` from the icon's top end — a numeral, not a dot.
- **Content descriptions**: a stacked Trips or Preferences dock includes its actual
  history parent; the FAB says `"Take a photograph"`,
  `"Take a photograph. N waiting across this trip"`, or
  `"A closed trip takes no photographs"`.

### 1.2 Where the bar is, and where it is not

`showsTabBar()` is one `when`:

| Bar | Routes |
| --- | --- |
| **shown** | `Home`, `Days`/`Timeline`, `Stats`, `More`, `Record`, `Trips`, `Preferences`, `ManualEntry`, `DataExport`, `PrivacyPolicy`, `Terms`, `Search` |
| **hidden** | `Onboarding`, `NewTrip`, `Capture`, `CloseDay`, `TripCard`, `Detail`, `Frame` |

`Route.tab()` maps `Home→Home`, `Days/Trip/Record/CloseDay→Timeline`, `Stats→Stats`,
and `More/Trips/Preferences/Settings/ManualEntry/TripCard→More`. Other stacked pages
have no selected destination.

### 1.3 The notice bar (`NoticeBar` in `AriadneApp.kt`)

A full-width strip above the dock: hairline, then one row —
the notice text in a tracked `Label` (10sp, `inkSoft`), an optional action
(`QuietAction`, `paper.blue`) and always `close` (`inkFaint`). The row is a
`LiveRegionMode.Polite` live region, so an undo offer is announced.

Notices are deferred while the shutter or photograph sheet is open so they cannot
obstruct the capture decision.

### 1.4 Where the app opens

The app holds on a quiet startup page while settings and trips are read, so a
returning person never sees the first-run welcome page flash before their saved
destination appears.

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
| `NewTrip` | `NewTripPage` | no | Onboarding **CREATE A TRIP**; Trips **BEGIN A NEW TRIP**; Home no-trip **START A TRIP** |
| `Home` | `HomePage` | yes | start, **HOME**, Trips **HOME** |
| `Days` | `TimelinePage` | yes | **TIMELINE** |
| `Trip` | `TimelinePage` | yes | legacy route redirected to Timeline |
| `Stats` | `StatsPage` | yes | **STATS** |
| `More` | `MorePage` | yes | **MORE** |
| `Record(day?)` | `RecordPage` | yes | Timeline plate, record's day tabs, Home **THE RECORD** row, Timeline **RECORD**, Search **RECORD**, Close day **BACK TO THE DAY**, Detail **LEAVE IT OPEN** |
| `CloseDay(day)` | `CloseDayPage` | no | Home **CLOSE DAY**, Stats **CLOSE TODAY**, record **CLOSE DAY**, the shutter page's **TODAY** |
| `Detail(captureId)` | `DetailPage` | no | any receipt line or photograph; Close day **EDIT**; sheet **OPEN THE WHOLE PAGE** |
| `TripCard(tripId)` | `TripCardPage` | no | Trip **THE CARD** / **CLOSE THE TRIP INTO A CARD**; Home closed **SEE THE CARD**; Preferences **CLOSE THE TRIP INTO A CARD** / **SEE THE CARD** |
| `Trips` | `TripsPage` | yes | More **TRIPS**; register itself from `startRoute` |
| `Search` | `SearchPage` | yes | Home **SEARCH WHAT YOU KEPT** row, record **SEARCH** |
| `Preferences` | `PreferencesPage` | yes | More **SETTINGS & PROFILE** |
| `ManualEntry` | `ManualEntryPage` | yes | More **ADD MANUALLY**; Home manual-entry action |
| `DataExport` | `DataExportPage` | yes | Preferences **EXPORT ALL DATA AS A ZIP ARCHIVE** |
| `PrivacyPolicy` / `Terms` | `LegalPage` | yes | More **PRIVACY POLICY** / **TERMS OF USE**; both are marked drafts |
| `Capture` | `CapturePage` | no | **only** `openCapture()` with no trip or a closed trip, and Escape from the pager (desktop) — legacy (§9) |
| `Frame(captureId)` | `FramePage` as a page | no | **only** a restored route — legacy (§9) |
| `Settings` | `PreferencesPage` | yes | retired name, still resolves |

### 2.2 The stack, Back, Escape

- `go(route)`: moving **between** destinations replaces the stack; moving **into** a page
  pushes the current route onto `history`; any navigation clears both overlays.
- `back()`: closes the sheet → closes the shutter → pops `history` → returns `false`,
  which is the app's cue to leave.
- `canGoBack` = an overlay is open **or** `history` is not empty.
- A stacked page with a history parent shows a consistent top-left **BACK TO [PARENT]**
  action; its destination is read from the same route history as system Back.
- Escape mirrors Back for overlays; on the pager it leaves for `Route.Capture`.

### 2.3 The two overlays

| State | Drawn by | Opened by | Closed by |
| --- | --- | --- | --- |
| `shutterOpen` | `CaptureOverlay`, full cover | `openCapture()` with an open trip; the FAB; record **CAPTURE**; record **ADD A PHOTOGRAPH**; Home **TAKE PHOTO** | **CLOSE**, Back, Escape, taking a frame |
| `sheetCaptureId` | `FramePage`, photograph sheet | one successful photo from camera or picker | **KEEP FOR LATER**, **OPEN THE FULL EDITOR**, Back, Escape |

Picking multiple photographs files them into the current day's waiting stack and returns
to the underlying page. A single picked photograph opens the same sheet as a camera photo.

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

`Onboarding` → **CREATE A TRIP** (`startNewTrip()` → `NewTrip` with *from* = today,
*to* = today + 6, currency = the setting) → **CREATE TRIP** (`commitDraft()`) → `Home`.
The user-facing onboarding and trip-register flows do not offer the demo roll.

### 3.2 The everyday loop

1. FAB (or record **CAPTURE**) → `CaptureOverlay` over the same page.
2. Tap the plate → `capturePhoto()` → the saved photograph appears in the sheet over the
   same page.
3. **KEEP FOR LATER** is the primary action; **OPEN THE FULL EDITOR** is secondary.
   The amount is not required on the post-capture path. OCR remains unconfirmed until
   Close Day.

Nothing navigates.

### 3.3 Settling a day

Home **CLOSE DAY** / record **CLOSE DAY** / Stats **CLOSE TODAY** → `CloseDay`, one card
at a time: **YES, SAVE IT** or **NO, LET ME ENTER IT**; **CONFIRM**; **KEEP IT WITHOUT AN
AMOUNT**; **EDIT**; **EARLIER**/**LATER**; **CONFIRM N READY**; **CONFIRM REST AS NO
AMOUNT**; **CLOSE THE DAY** → the summary → **BACK TO THE DAY**.

### 3.4 Reading the record

`Days` → a day row on the timeline spine → `Record(day)`: receipt lines → `Detail`; **CLOSE DAY**;
**SEARCH**; **TRIP**.

### 3.5 The card

`Trip` → **CLOSE THE TRIP INTO A CARD** (or **THE CARD**) → **COPY TEXT** or
**SAVE PNG**. PNG export uses the platform's local save/share surface; no server is used.

### 3.6 Registers, settings, switching

`More` → **TRIPS** → tap a row (`switchTrip`); **SETTINGS & PROFILE** → `Preferences`.
Settings keeps a local display name, appearance and currencies, and provides the ZIP
export action. **ADD MANUALLY** saves a confirmed expense without a photo or OCR queue.
Privacy and terms are in-app drafts that need review before publication.

---

## 4. State the UI reads

| Field | Read by |
| --- | --- |
| `route`, `history`, `canGoBack` | the shell |
| `shutterOpen`, `sheetCaptureId` | the shell (overlays) |
| `theme`, `displayName` | `AriadneApp` → `AriadneTheme`; More, Settings |
| `trip` | every page |
| `today` | Home, Record, CloseDay, Preferences, Search |
| `dayCards` | Home, Timeline, Record, Stats' "Close today" |
| `dayCaptures`, `dayTotalMinor`, `dayCurrency`, `dayPerCurrency`, `dayUnconverted`, `dayRateDates` | Home, Record, CloseDay, Detail |
| `tripPerCurrency`, `tripTotalMinor`, `tripUnconverted` | Timeline, Stats, Trip Card |
| `stack`, `stackIndex`, `amountInput` | the current day's Close Day flow and Home |
| `tripPending` | the Timeline dock badge and shutter's trip-wide waiting action |
| `dayCapturesAll`, `dayFigures` | Home, Timeline, Stats, the sheet's lookup |
| `categoryTotals` | Stats, Timeline, Home |
| `detail`, `detailAmount/Merchant/Note/Category` | Detail, the sheet |
| `storageDisclosure`, `rateDisclosure` | Preferences |
| `searchQuery`, `searchResults` | Search |
| `trips`, `activeTripId` | Trips, Timeline, Home, More |
| `draft` | NewTrip |
| `railDocked` | every tab page |
| `notice` | the notice bar |
| `busy`, `rateError`, `failureNote` | Preferences, the sheet |
| `deleteArmed`, `cardCopied` | Preferences, TripCard |

---

## 5. Every surface, in full

The current refresh adds a photo-free manual expense form, a local ZIP data-export flow,
the four-destination dock, and a welcome screen without a demo-roll choice. Home now
summarizes the active trip/day and recent expenses; Timeline combines the trip overview
with its day list; Stats includes daily spending; More opens Trips, Settings & Profile,
manual entry, and the in-app legal drafts. Fresh settings default to Light appearance.
The detailed surface descriptions below remain subject to device-level visual review.

Each tab page is framed by `TabPage`: a scrolling column, the hanging margin on a wide
window with a scroll rail, and a bottom spacer of 96–104dp so the bar never covers the
last row. Non-tab pages use `ScrollingMargin` or a bare column.

### 5.1 Onboarding — `OnboardingPage.kt`, no bar

Wide windows get a left margin column; phones get a single column.

| Element | Text |
| --- | --- |
| eyebrow (wide: a rail of labels) | `Ariadne` (tracking 3sp), `edition one`, `no account` |
| headline | **Welcome to / Ariadne** (42sp phone, 64sp wide; no italic styling) |
| rule | `paper.ink` |
| paragraph | "Keep the moments from a trip without turning them into a chore. Take a photograph now, add the amount later, and keep the whole record on this device." |
| primary | **CREATE A TRIP** (`startNewTrip()`), max width 360dp |
| foot | `No account · no server · no sync` |

### 5.2 New trip — `NewTripPage.kt`, no bar

`Masthead("", eyebrow = "Ariadne" \| "another trip", title = "Create a trip", lede = "Give it a name and dates. A budget is optional.")`

Fields, in order — each a labelled hairline set in the serif:

| Label | Placeholder |
| --- | --- |
| `the trip` | `Bangkok & Home` |
| `where` | `Bangkok — or a coast, or a train` |
| `from` | `2026-11-24` |
| `to` | `2026-11-30` |
| `currency` | a `CurrencyField` (see §5.16) |
| `budget, if you have one` | `25000` |

Then **CREATE TRIP** (`commitDraft()`), any draft error line, and the foot
`no account · no server · no sync`. **GO BACK** returns to Home when a trip already
exists. There is no demo-roll or demo-prefill action in this flow.

### 5.3 Home — `HomePage` in `TabPages.kt`, Today tab

**Header.** `HomeHero`: the `01 — ARIADNE` plate, the current trip name in the large
Instrument Serif italic, and `PLAN · TRACK · SPEND`, with the shared globe-and-flight
artwork faded behind the title.

**Rail (wide).** `today` = today's ISO or `—`; `days` = the trip's length; `closed N`;
`waiting today` = `stack.size`; `on this day`.

**Open trip, top to bottom:**

1. a dark, rounded daily-spend panel with the correctly scoped `dayTotalMinor`,
   `Finished today` or the current day's waiting count, and **SETTLE TODAY**
   (`openCloseDay(today)`). Globe artwork is repeated at low opacity inside the panel;
2. an outlined pill action, **Add an expense manually**, opens the photo-free
   confirmed-entry form;
3. an underlined `The Trip` heading with the trip's start and end dates;
4. four rounded `HomeShortcut` rows:

| Row label (serif) | Detail (below, sans) | Lands on |
| --- | --- | --- |
| `Trips` | `the register` (one trip) / `N in the register` | `Trips` |
| `Timeline` | `N of M days closed` | `Days` |
| `The record` | `N kept today` | `Record(today)` |
| `Search what you kept` | `by shop, price, day` | `Search` |

5. a **recent records** section across the trip, with up to four newest items; manual
   entries use a `MANUAL` placeholder instead of a photograph;
6. closing line: "No account. No sync. The record stays here."

**No trip.** The Home text explains either first-run or all-trips-closed state, then
**CREATE A TRIP** (`startEmptyTrip()`). With no trips, **OPEN THE TRIP REGISTER** is
also available. There is no demo-roll option.

**Closed trip.** Body — *"This trip is closed. Nothing new can be filed against it; the
record and the card stay exactly as they were."* → **SEE THE CARD** → **THE REGISTER**.

### 5.4 Timeline — `TimelinePage` in `TabPages.kt`, Timeline tab

The page is built on the reference: a watercolour globe hung off the top-right corner,
a masthead set over it, then **THE JOURNEY** and **THE DAYS** as two bar-marked
sections. It is the one tab with a picture on it.

**Backdrop.** `GlobeSky` (`core/design/.../Sky.kt`, the asset
`core/design/src/commonMain/composeResources/drawable/globe_hero.png`) — the earth's limb
with a plane crossing it, keyed to transparency so it sits on the paper with no edge.
Size 322×434 dp on a phone, 470×633 wide; offset `x = +52 dp` (`+96` wide) and
`y = −24 dp` (`−44` wide), so it bleeds off the page edge. It is drawn by `Page`'s
`backdrop` slot — behind the content and *outside* the scroller, so it stays put while
the days move — carries `contentDescription = null`, and prints at 18 % on night stock.
It cannot be tapped.

**Header.** `Masthead(number = "02", eyebrow = trip name \| "Timeline",
title = "Timeline", titleSize = 50 sp (76 wide), lede = "The journey and every day in
it.", trailing = QuietAction("Record") → Record(today), color = paper.ink)`. Upright, not
italic. On screen:

```
02 — KOREA                                   RECORD
                                              ‾‾‾‾‾‾
Timeline
The journey and every day in it.
________________________________________________________________
```

**Rail (wide only).** `closed N`, `open N`, `the trip as a run of days`.

**I · THE JOURNEY** — `MarkHead("The journey")`: a 2 dp upright ink bar, 12 dp, then the
tracked label. Under it:

1. the trip's name as a serif `Title`, 34 sp (46 wide) — `Korea`;
2. `KOREA · 2026-11-27 — 2026-12-03` — name, start, em dash, end, 9 sp tracked,
   `paper.inkFaint`;
3. **SPENT SO FAR** (9 sp, tracking 0.4) over the trip's total in its base currency, set
   in the *serif* at 33 sp (44 wide) — `₹0.00`. It is the sum of `state.dayFigures`,
   which `loadTripDays` converts per capture, so it is one currency's money;
4. opposite it, on the same baseline: `0 OF 7 DAYS CLOSED`;
5. a full-width rule in `paper.edgeStrong`.

**II · THE DAYS** — `MarkHead("The days")`, then one row per day of the trip — every
day, empty ones included — each opening `openRecord(day)`:

- the spine: a hairline down the margin, `paper.edgeStrong`, with a 7 dp ring at each day
  filled in the page colour so the line reads as passing behind it;
- the date as a printed block: `27` in the serif at 30 sp, then `NOV`, then `FRI`
  (9 sp tracked, `paper.inkFaint`);
- an upright hairline between the date and the content, the height of the row;
- a `StatusTag` — a rounded wash, `paper.blueWash` behind `paper.blue`: **IN THE RECORD**
  (closed), **SETTLED**, or **N TO SETTLE**;
- one line under it — **NOTHING KEPT**, or `3 KEPT · ₹1,240.00` (10 sp, tracking 1.5,
  `paper.inkMid`);
- a chevron at the row's centre in `paper.inkFaint`, and a hairline under the content
  only (not under the date);
- the whole row is one tap target (`Modifier.rowTap`) and one TalkBack node, described as
  `2026-11-27, in the record, nothing kept`.

The row's minimum height is 84 dp and it grows with the text: the spine and the column
rule fill whatever height the date block takes.

**Empty.** *"A trip has no days until it has dates."* + **START A TRIP**
(`startEmptyTrip()`), with the globe and masthead still drawn behind it.

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

### 5.8 The photograph sheet — `FramePage.kt`, overlay over the shutter

Full-screen paper; the shutter is covered, not peeking. On a wide window the same
blocks are a two-column spread: the card on the left, the readout and the two answers on
the right.

**Header.** A drawn back chevron (44dp target, `contentDescription = "Back"`, calls
`keepFrameForLater()`), then the serif title at 24sp — the merchant's name if the
recogniser found one, otherwise `Receipt` — then, on the right, a hairline pill
(`CircleShape`, `paper.ink` edge) carrying the entry status: `NEEDS AN AMOUNT`,
`NOT CONFIRMED`, `CONFIRMED`, `NO AMOUNT`, `IN THE RECORD` or `COULDN'T READ IT`. Under
it, a rule inset by the page margin, in `paper.edgeStrong`.

**The card** (`ReceiptCard`, `core/design/.../Receipt.kt`) — white paper, 16dp top
corners, and a bottom edge cut into 7dp scallops, the mark a shop makes tearing a bill
off a roll. Inside:

1. the head line: `RECEIPT` (9sp, tracking 2.4) — a hairline — the time it was taken,
   `04:54`;
2. the photograph, the full width of the card less 18dp a side and 320dp tall (520dp
   on a wide page), on `paper.sunk`; whatever cannot be decoded falls back to the plate
   study, exactly as everywhere else.

**The readout**, under the card:

- `PHOTOGRAPH KEPT` (8sp, `inkGhost`);
- the state as a serif headline at 27sp — `No amount is needed now.`, `Found ₹1,240.00.`,
  `Confirmed.`, `Kept without an amount.`, or `In the record.`;
- one serif line under it, 15sp `inkSoft` — `You can finish it at Close Day.`,
  `Confirm it at Close Day.`, `It is already counted.`, `It stays in the record as it
  is.`, or `The day it belongs to is closed.`

**The two answers, pinned to the foot.**

- **KEEP FOR LATER** — an ink block (`Action(fill = false)`) with a 14dp corner, the one
  round control in the book; reads **DONE** once the entry is resolved
  (`Confirmed`, `No amount`, `In the record`);
- **OPEN THE FULL EDITOR** — a quiet underlined action (`openFrame(capture.id)`).

**Missing capture.** *"That photograph is not here any more."* + **BACK TO TODAY**.

### 5.9 The shutter — `CaptureOverlay.kt`, full cover

| Element | Text |
| --- | --- |
| top-left | **CLOSE** (`closeShutter()`) |
| top-centre | the trip's name, or `Ariadne` |
| plate | live preview, or the engraved plate; `contentDescription = "Viewfinder. Tap to keep the photograph."`; tapping fires the camera |
| hint | `Tap the frame to keep it` — or `No camera here — tap the frame for a plate` when the platform supplies none |
| left of the shutter | **FROM THE LIBRARY** (`rememberPhotoImportMany` → `importPhotos`) |
| right of the shutter | `nothing waiting across this trip` / `N waiting across this trip` |
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
**Rail:** `the trip` (its figure), `entries` (count), `finished only`.

1. `the trip so far` + the trip's figure (50sp/64sp), animated to its value;
2. a rule at `paper.ink`, then `finished entries only`;
3. when a budget exists: `against a budget of` + the figure + `left`/`over` + the
   difference (blue when over);
5. the note *"Only entries that were finished. Anything still waiting across this trip
   is not in this figure, and is not counted as zero."*;
6. `SectionHead("II", "daily spending")` — a bar chart for up to the last seven trip days;
7. `SectionHead("III", "what it was")` — a `Breakdown` by category (`Food`, `Transport`,
   … or `Uncategorised`; `N entries`; the figure; `of`), a `PatternLine` sentence
   (*"Most of this trip so far went on food — 34% of it, across 6 entries. Everything
   else is in the column above."*), and the note *"Nothing here is sorted for you. A
   category is a claim you make on an entry, and most entries never need
   one."*;
8. `SectionHead("IV", "by currency, before conversion")` — a row per currency with a
   tick, the code and the figure, then `these are the amounts as they were paid, not
   converted`;
9. if nothing is tallied: *"Nothing has been tallied yet. The figures appear here the
   first time you confirm an entry."*;
10. **CLOSE TODAY** (`openCloseDay(today)`, accent; enabled only when the stack is
   non-empty, today is open and the trip is open).

### 5.12 Trip — legacy route

`Route.Trip` is retained for old links and routes but draws `TimelinePage`. The separate
`TripPage.kt` is no longer the active-trip overview; the overview is now at the top of
Timeline (§5.4).

### 5.13 The register — `TripsPage.kt`, stacked

**Header.** `Masthead("", eyebrow = "The register", title = "Trips", lede = "Every journey this device holds. One of them is open.", trailing = PageAction("Home"))`.

- three shelf figures: `journeys`, `open`, `closed`;
- the line *"One at a time. A new entry belongs to the open journey."*;
- numbered rows: month blocks, the name, `no place` when unset, `15 Oct — 22 Oct`,
  the budget or `—`, and a state word — `closed`, `open`, or `open, the one photographs
  go into` (the active trip is the page's one blue element). The row's
  `contentDescription` is `"name, dates, state"`;
- empty state: **No trips yet.** — *"A trip is the container the whole record hangs from:
  its dates, its money, and every entry you add during it."*;
- **BEGIN A NEW TRIP** (`startNewTrip()`). There is no demo-roll entry point.

### 5.14 The card — `TripCardPage.kt`, no bar

A single column of type, with separate text-copy and PNG-export actions:

- `A trip, kept`; the name; the dates; the days as rows with `closed`/`open`;
- *"No figures recorded yet. The entries are still the record."* when nothing is
  tallied, else the per-currency lines and `Total`;
- the closing sentence — *"Every day closed. This is the whole of the trip: no account, no
  sync, and nothing about it left this device."* or *"N of M days closed. The card keeps
  filling itself in until the trip is done."*;
- **COPY TEXT**, which becomes **TEXT COPIED** for a moment (`state.cardCopied`);
- **SAVE PNG**, which exports the card locally using the platform's save/share surface;
- a shell-level **BACK TO TRIP** action when this page has a route parent.

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

### 5.16 Preferences — `PreferencesPage` in `TabPages.kt`

**Header.** `Masthead(04, "Ariadne", title = "Settings")`; it is reached from More.
Stacked entry points use the shell's **BACK TO [PARENT]**
action.
**Rail:** `on device only`, `no account · no sync`.

**I · your profile.** A display-name field (`Your name`) saved only on this device.

**II · the record**

| Row | Type | Note under it |
| --- | --- | --- |
| `Base currency` | `CurrencyField` → `editTripCurrency` | `Every total is stated in this one` |
| `Home currency` | `CurrencyField` → `editTripHomeCurrency` | `What a foreign amount is converted back to` |
| `Dates` | read-out | today's ISO date + `, ISO order`; `—, ISO order` when no day is open |
| `Paper` | three chips | `Light paper, always` · `Follows this device's light or dark setting` · `Night paper: the same design on dark stock` |

The Paper chips read `Light`, `System`, `Night` and call `setTheme(...)`; fresh settings
default to `Light`, and the chosen one is drawn in `paper.blue`.

**III · the exchange.** One row per currency in use, plus `THB`, `USD`, `EUR`, `GBP`,
`JPY`. Each row, top to bottom:

- a budget glyph, the currency **code** as a serif `Title` (e.g. `SGD`), a blue tick when
  the currency is actually in use, and **CLEAR** (`clearFxRate(code)`) — only when a rate
  is on file;
- the sentence `1 SGD = 74.6298 INR`, or `no rate on file — this currency is not being
  converted` (in use) / `no rate on file` (not in use);
- when a rate is saved with a date, `rate dated 27 Nov` beneath the pair;
- **SET RATE** (no rate) or **CHANGE** → opens the one open editor: `1 SGD =` + a field
  (`rate` label beside it) and **SET** / **CANCEL**; a refused value reads `not a number`;
- the row is followed by a rule; only one editor can be open at a time.

With nothing foreign: *"Everything so far is already in the base currency."* A rejected
rate is stated under the list (`state.rateError`).

**IV · what leaves this device.** A `Storage` read-out (`Local to this build`) followed by
platform-specific `storageDisclosure` and `rateDisclosure` text. This states where the
record and photographs are kept, whether data persists across launches, and whether an
exchange-rate request sends anything off-device. If a failure was recorded, Preferences
shows its message explicitly. **EXPORT ALL DATA AS A ZIP ARCHIVE** opens the local export
flow.

**V · this trip.** The trip's name (or `No trip`), the place (or `no place set`), then
icon rows: **SEE THE TRIP'S PAGE**; **CLOSE THE TRIP INTO A CARD** (`closeTripIntoCard()`)
or **SEE THE CARD** when already closed; **DELETE THIS TRIP** (`armTripDeletion()`) with
the paragraph *"Deletes its photographs and its record. Nothing is kept anywhere else, so
this is the one action in the app you cannot take back."* Armed, the block becomes
`this deletes everything, now` (blue label) and **DELETE FOR GOOD** (`deleteTripNow()`) /
**KEEP IT** (`armTripDeletion()`).

### 5.17 More, manual entry, export, and legal pages

**More.** **TRIPS**, **ADD MANUALLY**, and **SETTINGS & PROFILE**, followed by **PRIVACY
POLICY** and **TERMS OF USE** under About Ariadne.

**Manual entry.** A form for amount, currency, date within the open trip, optional
merchant/place, category, and note. Saving creates a `CONFIRMED` entry with no photo
(`photoPath = ""`); it is not queued for OCR. Invalid amount, currency, or out-of-trip
date is shown inline. Home and Stats label manual records as entries rather than photos.

**Data export.** Settings opens a local ZIP save/share/download surface. The archive
contains `data/ariadne.json`, `data/trips.csv`, `data/days.csv`, `data/captures.csv`,
and `README.txt`; receipt photos are included when the target has readable local files.
The JSON contains settings, trips, days, captures, exchange rates, and photo-entry paths
inside the archive. No server is involved.

**Legal.** Privacy policy and terms are in-app drafts explicitly marked for review. They
are not publication-ready: the repository does not supply an organization, jurisdiction,
or contact details.

---

## 6. Shared strings and formatting

**Money.** `Money.formatWithSymbol(currency)` = symbol + `format`, e.g. `₹82,141.92`,
`S$18.41`. Grouping is Indian lakh style for `INR, NPR, LKR, PKR, BDT`, three-digit groups
elsewhere; scale follows the currency (JPY has none). Negative figures carry a `-`
sign; the app prefers `₹8,240.00 left` / `₹8,240.00 over` over a negative numeral.

**The `≈`.** Printed before a figure that contains converted money. Where the record's
conversion note is shown, it includes the effective rate date, for example
`≈ converted at the rate for 20 Nov`.

**Capture titles.** `MerchantName.receiptTitle(merchant, "21:09")`: a cleaned merchant
name, else `Receipt · 21:09`, else `Receipt`. Never a raw OCR line, never "Untitled".

**Entry status labels.** `Needs an amount`, `Not confirmed`, `Confirmed`, `No amount`,
`In the record`, `Couldn't read it`.

**Dates.** ISO (`2026-11-27`) in fields and rows; spoken form `27 Nov` in sentences;
times as 24-hour `HH:MM`.

**Empty states (verbatim).** `No trips yet.` · `nothing kept` (a day on the timeline) ·
`Nothing kept on this day. An empty day is still part of the trip — it is the one you
spent walking.` · `Nothing by that name.` · `Nothing has been tallied yet. The figures
appear here the first time you confirm an entry.` · `Everything so far is already in the
base currency.` · `Nothing left to settle` · `No figures recorded yet. The entries are
still the record.`

**Error copy.** *"The trip could not be saved. … — nothing was lost; try again."* ·
*"Your trip could not be opened. …"* · *"That trip could not be opened. …"* · `not a
number` (rate field) · `no rate on file — this currency is not being converted`.

**Accessibility descriptions written in source.** `"Take a photograph"` (and
`"Take a photograph. N waiting across this trip"`, `"A closed trip takes no photographs"`) ·
`"Viewfinder. Tap to keep the photograph."` · `"Back"` (the sheet's chevron) ·
`"Search the record"` · `"Search currencies"` · `"Camera viewfinder"` ·
`"Backspace"` and every keypad digit · the currency chips' three variants
(`"Currency SGD"`, `"Currency SGD, the one on this photograph"`,
`"Currency SGD, not changeable on a closed trip"`) · register rows,
`"name, dates, state"` · stacked dock destination descriptions include the actual history
parent, such as `"Preferences, stacked over More"`. Settings is reached from More.

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

**Motion.** `Reveal` (Home's rows, staggered) and `animatedLong` (Stats' figure)
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

**Not implemented anywhere in the UI:** deleting a single entry (trips can be deleted,
captures cannot); editing an entry's date; voice capture; SMS import; bill splitting;
accounts, sync, analytics; multiple photographs per entry; crop or rotate; importing an
archive back into Ariadne. Local ZIP export and Trip Card PNG/text sharing are implemented.

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
| `:ui:app:jvmTest` | passed locally, including archive ZIP and manual-entry export cases |
| `:app:android:compileDebugKotlin` | passed locally |
| `:app:desktop:compileKotlinJvm` | passed locally |
| `:app:wasm:compileKotlinWasmJs` | passed locally |
| iOS compile | not run on this Windows machine |
| `FlowCheck` (stack, overlays, undo, the sheet's pair) | written; **never executed** |
| Any screen rendered, measured, contrast-checked, or read at 200% | **not yet; human/device review remains** |
| Stills | `preview/Shots.kt` defines them; `docs/shots/` holds only the pre-redesign archive |

The gate for every visual claim on this page is `tools/verify-ui.sh` (and
`tools/shoot.sh` for stills) on a machine with Gradle and a display.
