# Ariadne — every page, what is on it, and the design in words

> **Superseded in part, as of the redesign.** This document describes the book as it
> was before Phase 2. Today, Days, Close day, Trip, Stats and Preferences have since
> been rewritten, the tab bar has been replaced, the routes `Timeline` and `Nightfall`
> no longer exist, and the plate numbers are gone. The current design is described in
> `docs/PHASE-2.md` and `docs/PHASE-3.md`; the rest of this file is still the best
> description of the pages that have not been rewritten yet (the record, search, the
> register, the new-trip form, the card, onboarding).

**What this document is.** A complete, page-by-page description of the Ariadne UI
and its flows, written from the code that actually renders it. It describes what
exists — layout, type, colour, copy, states, and where each control leads — not
what ought to exist. It is meant to be handed to a reviewer (human or AI) that
will propose improvements.

**Revised 2026-11-26, three times.** The first pass fixed navigation, first run,
the closed-trip lifecycle, plate numbering, the search destination, library
import, currency choices and several copy/contrast issues. The second pass shipped
night paper, the currency catalogue, the one-blue audit, the kept card's warmer
stock, keyboard insets, an Android photo picker and 200% stills. **The third pass
answered the P0/P1 list**: one startup rule in one function, the register bar's
contract, the closed-trip matrix across every entry path, the 200% audit (source
level — 15 stills defined, none rendered here), the IME and inset decision, one
plate geometry, one open rate editor, the ink ladder written down, and the route
names settled. §9 lists all three passes and what is still open; Appendix C answers
the review item by item.

**Source of truth.** The Compose source in `ui/app` and `core/design`. Every
screenshot in this repository predates the third review and has been archived
under an explicit label (`docs/shots/archive-2026-11-26-pre-third-review/`); none
is cited as evidence here. `docs/shots/` is empty of renders until the harness is
run against this build. `docs/DESIGN.md` and `docs/PAGES-BRIEF.md` are
older companions: the first is the visual rationale, the second is an
intent-only brief with no visual direction. This file supersedes neither, but it
is the only one that lists every surface as built.

**How to read it.**
1. §1–§3 give the thesis, the visual language and the shell.
2. §4 gives the flows as paths through the pages.
3. §5 gives all fifteen surfaces, one section each: purpose, top-to-bottom
   inventory, states, exits.
4. §6–§8 give the shared objects, edge states and responsive behaviour.
5. §9 lists what the first review changed and what is still open.
6. §10 is the brief for the reviewer.

**What would be useful back.** Prioritised, specific recommendations that respect
the product's thesis: a photograph-first trip ledger that feels like a small
printed book; one blue action per surface; nothing required, nothing scolding;
no accounts, no server, no sync. Critique against that thesis. A generic
fintech/dashboard redesign is the failure mode this design exists to avoid.

---

## 0. Glossary

| Term | Meaning in this product |
| --- | --- |
| **Trip** | A dated container for captures. Exactly one trip is *active* (open); photographs taken now land in it. |
| **Capture** | One photograph plus whatever is known about it. Has a state: `QUEUED → EXTRACTING → NEEDS_AMOUNT / NEEDS_CONFIRMATION → CONFIRMED → ARCHIVED`. |
| **Plate** | (a) the photograph, or its engraved stand-in; (b) the two-digit page number that makes the app a sequence. |
| **Waiting / stack** | Captures taken but not yet finished. Always countable, never alarming. |
| **Nightfall** | The evening ritual in which the day's waiting captures are finished, one card at a time, in about two taps each. |
| **Close the day** | Files the day. Open captures become "no amount" records rather than being dropped. |
| **Close the trip** | Ends the trip and opens its card. Not deletion. |
| **The card** | The keepsake page at the end: a printable column of type summarising the trip. |
| **Fence** | The optional budget. A fence, never a target; overspending is stated in the same tone as underspending. |
| **Base currency** | The one currency every total is stated in. |
| **Home currency** | What a foreign amount is converted back to. |
| **Rule** | A 1dp hairline. The app's main structural device. |
| **Masthead** | The standard page head: plate number, eyebrow, display headline, optional lede, then a rule. |
| **Plate number** | `01`…`09` set at the top of a page, in 9sp tracked capitals, before the eyebrow. |

---

## 1. The product, and the constraints the UI must honour

Ariadne is a **trip ledger with a camera**. You photograph what you spend on
while travelling — a receipt, a stall, a ticket, a souvenir — and type nothing in
the moment. In the evening you sit down once with the day's photographs and
finish them: each takes roughly two taps to become a figure in the record. At the
end you have a card you keep.

The central inversion, stated on the first screen: **the photograph is the
memory; the numbers wait for night.** Most money apps make you book a transaction
in the moment and then forget it. This one keeps the moment and asks about the
money later, together, once.

Constraints that show up visually:

- **No account, no server, no sync, no analytics.** Works in airplane mode
  forever. Screens say this plainly, in small type, at the bottom.
- **Nothing is required.** Every field is optional, including the amount. An
  unfinished photograph is a valid record, not an error.
- **No red, anywhere.** Errors are ink, and a failure is a sentence that names
  what happened.
- **It is a sequence, not a set of screens.** Pages carry plate numbers and read
  in order.
- **The photograph is the largest thing on any page it appears on.** It never
  competes with chrome, and text never sits over it.

Tone: plain, dry, a little wry. Short declaratives. No exclamation marks, no
"Let's get started!", no emoji. When the app cannot do something it says so
rather than dressing it up. It never flatters the user.

---

## 2. The design language, in words

### 2.1 The feeling

A small printed book that happens to be an app. A ledger with plates. The
references are a photographer's contact sheet, a museum wall label, and an
accounts book kept by hand: quiet, ruled, confident enough not to decorate
itself.

Concretely, on screen: a cool off-white page; a high-contrast serif set large and
slightly italic for the page's one headline; small capitals tracked far apart for
every label, tab and eyebrow; everything else in a neutral grotesque at 15px; one
confident blue used exactly once per surface; and a great deal of empty paper.
Structure comes from hairlines, not from boxes, shadows or rounded cards.

**What it must never look like:** a fintech dashboard, a habit tracker, a
productivity app, a Material template, anything with a floating action button,
rounded filled buttons, or white cards floating on a grey background.

### 2.2 Colour

One chromatic value. **Blue means "this is the thing to do"** and appears once per
surface — one button, one selected state, one figure you acted on. Two blues on a
page means one of them is wrong.

**Light stock (default):**

| Token | Hex | Where it is used |
| --- | --- | --- |
| Paper | `#F8FAFC` | The page. Cool off-white, never cream. |
| PaperRaised | `#FFFFFF` | The tab bar, panels, a raised row. |
| PaperSunk | `#F1F4F7` | Pills, plate ground, keypad ground, Nightfall's page ground. |
| Edge | `#E2E8F0` | Hairlines inside content, row rules, frames. |
| EdgeStrong | `#CBD5E1` | Frame edges, the tab bar's top line, outlined buttons. |
| Ink | `#1F1F20` | Headlines, figures, the shutter, major rules. |
| InkMid | `#334155` | Quiet action labels, secondary figures: a control that is not the blue one. |
| InkSoft | `#475569` | Running text that is not the point: a note, a footnote, a quiet label. |
| InkFaint | `#64748B` | Footnotes, inactive tabs, metadata. |
| InkGhost | `#CBD5E1` | Ticks, plate line work, captions that must disappear. |
| Blue | `#2563EB` | The one action. |
| BluePressed | `#1D4ED8` | Pressed ring on the shutter. |
| BlueWash | `#EFF4FE` | A count pill, a selected chip. |
| BlueEdge | `#BFD3FB` | The hairline on a blue wash. |
| TripGround | `#F5F2E7` | The keepsake page's warmer stock; the trip card is a raised sheet on it. |

**Night stock** (same design on dark paper): page `#111318`, raised `#181B21`,
sunk `#0C0E12`, edge `#272B33`, edge strong `#3A404A`, ink `#F2F5F9`, mid
`#B6C0CC`, soft `#8794A3`, faint `#616C7A`, ghost `#3A404A`, blue `#7DA2F7`,
blue wash `#1A2436`, blue edge `#2E4470`.

Night is chosen in Preferences (`Light` / `System` / `Night`) and stored with the
settings, so all four targets print on it: `System` asks the platform, and the
choice is read before the first frame so a dark-paper app does not flash white at
launch. The whole palette swaps — ink, edges, blue, the trip's warmer stock — not
just the background.

### 2.3 Typography

Two faces, both open-licensed and vendored:

- **Instrument Serif** — headlines only, always large. Regular and italic. The
  italic is used where a page wants to whisper rather than announce.
- **Inter Tight** — everything else: body, labels, buttons, figures. Bound at four
  real weights (Light 300, Regular 400, Medium 500, SemiBold 600).

**A number is never a float and never shifts.** Amounts are tabular figures
(`tnum`), so `1,200.00` and `25.00` align in one column. The numeral face is the
same as the sans; a second mono face would be a third voice on the page.

Size ladder and optical tracking (tracking is corrected per size — big type is
pulled tight, small tracked type gets air):

| Role | Face | Size | Tracking | Notes |
| --- | --- | --- | --- | --- |
| Display | Instrument Serif | 44 (56 on wide) | −0.9sp | Page headlines; italic often. |
| Title | Instrument Serif | 30 / 24 / 21 / 19 / 17 | −0.5sp at 30 | Rows, names, section titles. |
| Body | Inter Tight | 15 (line height 1.6×, set per page) | +0.05sp | Secondary body 13/12/11. |
| Numeral | Inter Tight, tnum | 68/52 / 50/64 / 44 / 19 / 17 / 15 | −0.8sp ≥34 | The largest figure on a page is 44–68px. |
| Label | Inter Tight Medium | 9–10 | +1.4 to +2.2sp | Uppercase. Every field, tab, eyebrow, button. |
| Action text | Inter Tight SemiBold | 11 | +2sp | Uppercase blue bar. |

Line lengths are capped at a **620dp measure**; prose never stretches across a
desktop window.

### 2.4 The grid and page furniture

Every page hangs from the **left** of the page, like a printed book, not centred
in a window.

- **Phone:** one column, 24dp side margins, 20–22dp top.
- **Wide (≥900px web, desktop always):** a **132dp hanging rail** carrying dates,
  figures, section marks and glyphs; a **28dp gutter**; then the text column,
  capped at 620dp (+120dp of tolerance on tab pages). The whole spread is capped
  at 980dp plus margins and stays left-aligned. The rail can be folded away with
  a small glyph at the seam (two hairlines and an arrow), 180ms width animation.
- **Page head (masthead)**, every page except Capture and Frame: a 9sp plate
  number (when the page has one), an 18dp hairline, a 9sp tracked eyebrow, then
  the display headline
  (italic when the page whispers), an optional serif lede at 16/18, and a 1dp
  **ink** rule. This object is what binds the app as one book. The plate numbers
  `01`–`09` belong to the pages of the trip: Home 01, Timeline 02, Stats 03,
  Preferences 04, Nightfall 05, Record 06, Detail 07, Search 08, Trip 09. The
  front door (New trip) and the register (Trips) are app-level furniture and
  carry no number, as do the three moments (shutter, frame, card). A number
  therefore never repeats.
- **Section heads inside a page:** a roman numeral in blue at 9sp, a tracked
  label, optional right-hand metadata, 30dp of air above.
- **Scroll bar:** not the platform's. A hairline track 18dp wide in the outer
  margin with a square 2dp thumb that thickens to 3dp only while the page moves.
  It never covers text.
- **Rules:** 1dp. Ink rules separate major blocks; `Edge` rules separate rows.

### 2.5 Shapes and surfaces

- Controls: **2dp** radius. Tags and chips: **1dp**. Panels: 2dp.
- No shadows anywhere. Depth is a hairline plus a white panel.
- The only circle in the product is the shutter (and small status dots).
- Buttons are full-width bars that sit at the edge of their container, not
  floating pills. Primary is blue with white tracked capitals; secondary is a
  hairline rectangle with ink capitals; tertiary is a small tracked word with a
  1.5dp underline.
- Fields are **labelled hairlines**, not boxes: a 9sp label, a value set in sans
  or serif, then the rule that is the field. No floating labels, no outlines, no
  two edges around one piece of text.

### 2.6 Iconography and illustration

- Icons are **drawn, not imported**: one weight of line (about 1.4dp), one
  24-unit grid, 16dp beside text, 13–14dp inline. Glyph set: shutter, trip,
  date, place, budget/fence, camera, photograph, night, search, record, stats,
  gear, waiting, remove, share, back, backspace, done, paper, ledger, edit.
- Where there is no photograph, the page shows a **single engraved line study**
  on faintly ruled paper: one subject, one line weight, generous margin, capped
  at 300px so it never blows up. Subjects: chai cup, shoe, bag, bowl,
  ticket, and an unknown mark. It never pretends to be a photo.
- The capture plate without a camera shows crop marks in the four corners and a
  register cross in the middle — a plate waiting for an image.

### 2.7 Motion

One duration, one curve, one property:

- **180ms, cubic-bezier(0.2, 0, 0, 1)** (fast away, gentle arrival, no overshoot).
- Route changes **cross-fade** with a 10dp **horizontal** drift that settles back
  (pages turn across, never up).
- Lists fade in with opacity only, staggered 34ms per row, capped at six rows.
- A changing money figure counts (140ms) rather than snapping.
- Reduced motion (Android animator scale / `prefers-reduced-motion`) sets every
  duration to 0; pages never sniff the platform themselves.

### 2.8 Voice in the UI

Every string is short, factual, and admits limitations. Examples that are in the
build:

- "The photograph is the memory. The numbers wait for night."
- "A name, the dates, and a figure to stay under. The fence is optional."
- "Nothing here is required."
- "Nothing has been tallied yet. The figures appear here the first time you
  finish a photograph at nightfall."
- "An empty day is still part of the trip — it is the one you spent walking."
- "Only photographs that were finished. Anything still waiting tonight is not in
  this figure, and is not counted as zero."
- "No account. No sync. The record stays here."
- "Deletes its photographs and its record. Nothing is kept anywhere else, so this
  is the one action in the app you cannot take back."
- "not a number" (the only error label in the exchange-rate field)

No emoji. No exclamation marks. No praise. No "Oops".

---

## 3. The shell

### 3.1 Navigation model

There is no navigation library: one sealed route type, one `when`, and a
bottom bar that belongs to the shell rather than to any page, so it cannot drift
between screens.

- **Four tab destinations:** `Home`, `Timeline`, `Trip`, `Stats`.
- **The shutter is not a tab.** It is a 62dp blue plate seated in a notch cut out
  of the bar's top edge, always one tap away.
- **The bar is hidden on surfaces that are moments rather than places:**
  onboarding, new trip, the shutter, the frame just taken, Nightfall, the
  photograph detail, and the card. A bar under the ritual would make it feel like
  an errand.
- **Stacked pages** (record for a day, detail, search, card, preferences) sit on
  top of the tabs.

The notch is the bar's one flourish: a filled path with a pair of cubic curves
dipping around the shutter, traced with the same hairline as the bar's top edge,
so the bar reads as bending around the button rather than having a hole punched
in it.

### 3.2 The bar, drawn

- 74dp tall, sitting on an 18dp paper apron, white/raised surface, `EdgeStrong`
  top line.
- Four items either side of the notch: a 16dp drawn glyph above a 9–10sp tracked
  uppercase label (`HOME`, `TIMELINE`, `TRIP`, `STATS`). Selected: ink. Unselected:
  ink soft.
- The shutter: a 62dp blue disc inside a 5dp paper ring, aperture glyph in paper
  white, hairline ring in a translucent pressed blue. A 17dp ink badge at its top
  edge carries the waiting count (8sp numeral) when anything is waiting.
- The Home tab carries a small blue dot when anything is waiting.

### 3.3 Route table

| Route | Page | Plate | Bar | In the bar? | Reached from |
| --- | --- | --- | --- | --- | --- |
| `Onboarding` | Onboarding | — | hidden | no | Launch with no trip at all: "Begin empty" → New trip, "Demo roll" → seeded trip → Home |
| `NewTrip` | New trip | — | hidden | no | "Begin empty"; "Begin a new trip"; after deletion; a trip that cannot be opened |
| `Home` | Home | 01 | shown | Home | Launch with a trip; after creating/opening one; the Home tab; "Go back"; Done in Preferences |
| `Trips` | The register | — | shown, **nothing selected** | no (Home family) | The Home page's "Trips" row; launch when every trip on disk is closed |
| `Timeline` | Timeline | 02 | shown | Timeline | The Timeline tab; the Home page |
| `Stats` | What it came to | 03 | shown | Stats | The Stats tab; the Home page index |
| `Preferences` | Preferences | 04 | shown, **nothing selected** | no (Home family) | Home's `PREFS` action; a restored `Settings` route aliases here |
| `Trip` | The trip's page | 09 | shown | Trip | The Trip tab; Home; card |
| `Capture` | The shutter | — | hidden | shutter | Shutter; Home; Record; Frame; Escape from Nightfall |
| `Frame` | The frame, held up | — | hidden | no | Immediately after the shutter fires |
| `Nightfall` | The ritual | 05 | hidden | no | Home ("Finish N"), Capture ("N waiting"), Record, Stats, Frame |
| `Record` | A day in full | 06 | shown | Timeline | Timeline day, Home, after finishing a card, Search results, Trip |
| `Detail` | One photograph | 07 | hidden | no | Record row, Nightfall "Edit", Frame "Fill it in now" |
| `Search` | Remember a thing | 08 | shown | no | Home index, Record's quiet "Search" |
| `TripCard` | The card | — | hidden | no | Trip page; Preferences' "Close the trip into a card" |

`Home` is today's page, and it is what the **Home tab** opens. `Trips` is the
register: a stacked page one row down Home's index, with its own `HOME` page
action to come back. The register does not claim the Home tab as a destination,
and no two pages answer to "Home".

**The bar contract for a stacked page** (the register and Preferences): the bar
stays visible — it is how you get back to the rest of the app — and **no
destination is selected**, because none of them is where the person is. `Route.tab()` returns `Tab?` and returns `null` for these, so
"Home is highlighted but you are not on Home" cannot happen by construction. The
bar is announced to a screen reader as "Trips register, stacked from Home" or
"Preferences, stacked from Home" rather than being read as a tab. The page keeps
its own top-right action back to Home.

`Detail` hides the bar instead: it is a sheet with a keypad pinned to its foot, and
two bottoms on one phone is one too many. What it does instead is remember where it
came from: `AppUiState.detailOrigin` is the single fact the Back button reads — to
the Record day when it was opened from a row, or to the Search results with the
query intact when it was opened from a result.

### 3.4 Wide vs phone

`wide` is true on desktop and on the web above 900px; false on Android and iOS.
The difference is more than stacking:

| Surface | Phone | Wide |
| --- | --- | --- |
| Tab pages | one column, no rail; bar hidden | 132dp hanging rail (today/days/closed/waiting), foldable |
| New trip | fields stacked, demo actions stacked | dates/currency pairs side by side, demo actions in a row |
| Nightfall | plate 16:9 scrolls; amount + both decisions + ruled keypad pinned to the bottom | a two-page spread: plate left (with "plate N of M" and Earlier/Later), triage right |
| Detail | plate 16:10, fields scroll, keypad pinned | plate 4:5 left, fields right, keypad in the right column |
| Trip card | 20dp margins | 96dp margins, wider inner padding |
| Onboarding | one column, wordmark above the headline | 220dp imprint margin on the left, 560dp text block |

---

## 4. The flows

### 4.1 First run (the critical ninety seconds)

```
Launch, no trip on disk
        │
        ▼
Onboarding — "The photograph is the memory. The numbers wait for night."
        │
        ├── "Begin with the demo roll" ──► Trip created + 5 seeded photographs ──┐
        │                                                                        │
        └── "Begin empty" ──► New trip ── name, dates prefilled today → +6,      │
                                     currency, optional fence ── "Open" ─────────┤
                                                                                 ▼
                                                          Home (01) — "0.00 / finished today",
                                                                      waiting count
                                                                         │
                                                                         ▼
                                                          Shutter ──► Frame ──► keep ──► Home
```

The first page a stranger sees is the **title page**, which states the inversion
in four sentences and offers the fastest way in (the demo roll) and the honest
one (a blank form with the dates already filled). One tap stands between it and
the shutter; the thesis is not paid for in effort. If every trip on disk is
closed, launch goes to the register instead, where those trips are still
readable.

### 4.2 The daytime loop (capture, no decisions)

```
Home ── "Take a photo" ──► Shutter ──tap anywhere──► Frame (the photograph, held up)
   ▲                                                        │
   │                                                        ├── "Keep it for nightfall" ──► Home (waiting count +1)
   │                                                        ├── "Fill it in now" ──► Detail ──► Save ──► Record for that day
   │                                                        └── "Take another" ──► Shutter
   │
   └── shutter in the bar, from anywhere
```

Zero fields on the shutter. The only decisions are *keep* and *describe now*.
Nothing is ever lost: both exits write the capture first.

### 4.3 The evening ritual (Nightfall)

```
Entry: Home "Finish N" · Capture "N waiting" · Record "Nightfall" · Stats "Close today"
        │
        ▼
Nightfall (05): one card = plate + merchant + amount + two decisions
        │
        ├── Confirm (blue) ─────────────► next card; when the stack empties → Record(day)
        ├── No amount (outlined) ───────► next card, filed without a figure
        ├── "Is it ₹X?" → Yes, save / No, let me enter it
        ├── "Confirm N ready" ──────────► every card that already has an amount
        ├── "Confirm rest as no amount" ► the quick way out
        ├── "Edit" ─────────────────────► Detail for this one photograph
        └── "Close the day" ────────────► Record(day); untouched captures become no-amount records
```

On a wide screen J/K move through the stack, Enter confirms, Escape leaves for
the shutter, digits type the amount, Backspace deletes. On a phone the amount,
both decisions and the keypad are pinned; only the plate scrolls.

### 4.4 Correcting one photograph

```
Record row ──► Detail (07) ── amount (keypad) · currency chips · where · category · note ──► Save ──► Record
Nightfall "Edit" ──┘
Frame "Fill it in now" ──┘
```

Everything on Detail is optional. "Keep it without an amount" is a first-class
exit, and so is "Leave it open".

### 4.5 Reading the trip

```
Home ──► Timeline (02) ── each day: date spine + 24-hour band ──► Record (06) for that day
Home ──► Stats (03) ── total, fence, breakdown by category, breakdown by currency
Home ──► Search (08) ── shop · figure · currency · day ──► Detail (07) ──Back──► Record
Home ──► Trip (09) ── dates, progress, currencies, close
```

### 4.6 Ending a trip, and starting another

```
Trip (09) ── "Close the trip into a card" ──► TripCard (keep, share as text)
   │                                               └── "Back to the trip" / "Back to the record"
   └── the trip is no longer active: the register stops marking it, Capture
       shows its closed state, and Home offers "See the card" / "The register"

Home ──► Trips (register) ── tap an entry ──► that trip becomes the one bound ──► Home
                            └── "Begin a new trip" ──► New trip
```

### 4.7 Deleting

```
Preferences ── "Delete this trip" ──► armed state:
   "this deletes everything, now"  [Delete for good]  [Keep it]
                                          │
                                          ▼
                                  New trip (01), state reset
```

Two taps, by design. There is no undo because there is no copy anywhere else.

---

## 5. The pages

Fifteen surfaces. Each entry gives purpose, a top-to-bottom inventory, states,
and exits. Sizes are px/sp as rendered; "label" means 9–10sp uppercase with
+1.4–2.2 tracking unless stated.

---

### 5.1 Onboarding — the title page

**Route** `Onboarding` · **Plate** none · **Bar** hidden · **Wide** imprint margin + text block

**Purpose.** State the inversion before a stranger meets a shutter: the
photograph is the memory, the numbers wait for night. Two actions, four
sentences. It is the title page of the book.

**What is on it**

1. *Wide only:* a 220dp left margin carrying `ARIADNE` in tracked caps, a 28dp
   blue dash under it, and, pinned to the bottom, three faint imprint lines:
   `EDITION ONE`, `A TRIP LEDGER`, `NO ACCOUNT`.
2. Centred vertically in the remaining width: a 560dp column.
3. The headline, two lines, broken where the sentence breaks:
   **“The photograph / is the memory.”** — 40sp phone, 66sp wide, serif, ink.
4. Immediately under it, in the serif italic and in ink-soft:
   **“The numbers wait for night.”** Same size.
5. A **black 1dp rule** across the column.
6. A serif lede at 17/19sp, ink-mid, 27/30 line height: *“Forty rupees on chai,
   twelve hundred on shoes, and by tonight neither is remembered. Point the
   camera at the receipt, the stall, the ticket. When the day is done, sit down
   and settle it — two taps a card.”*
7. A blue action, capped at 360dp wide: **BEGIN WITH THE DEMO ROLL**.
8. A quiet action under it: **Begin empty** (opens a blank form).
9. A 40dp gap, then the smallest type on the page:
   `NO ACCOUNT · NO SERVER · NOTHING LEAVES THIS DEVICE`.

**States.** None; it is static.

**Leaves to.** Demo roll → creates a trip with five seeded captures → Home.
Begin empty → New trip.

**Design notes.** This is the only full-bleed “title page” in the app: no
masthead, no plate number, no bar. Its whole job is one typographic gesture. It
is the first surface on a device that has never held a trip; once a trip exists,
launch goes straight to Home.

---

### 5.2 New trip — the front door

**Route** `NewTrip` · **Plate** none (the door before the plates) · **Bar** hidden · **Wide** paired fields

**Purpose.** Get from install to holding a camera in one screen. A trip has to
exist before a photograph can be filed against it.

**What is on it, top to bottom**

1. Masthead: the eyebrow `ARIADNE` (or `ANOTHER TRIP` when a trip exists) — no
   plate number — then the headline **“Where to”** in serif italic at 44/56sp,
   then a serif lede:
   *“A name, the dates, and a figure to stay under. The fence is optional.”*
   Then the black rule.
2. Five fields, set as a ledger form — no boxes, just labelled hairlines:
   - `THE TRIP` — serif at 26/30sp, placeholder **“Bangkok & Home”**. This is the
     only field that reads as a title, because it is one.
   - `WHERE` — sans 19sp, placeholder *“Bangkok — or a coast, or a train”*.
   - `FROM` and `TO` — sans 19sp, prefilled with today and today+6 in ISO order
     (`2026-11-24`, `2026-11-30`). **Side by side on a wide page, stacked on a
     phone**: two ISO dates at 200% type do not fit in half a measure, and a date
     clipped at the year is worse than one under the other. Each keeps its own
     label and rule, so the pair is still two facts rather than one merged field.
   - The budget field asks for the number keyboard; every other field says `Next`,
     so the IME walks down the page instead of closing after each line.
   - `CURRENCY` — a chosen value, not a typed one: the row states the code and the
     currency's name (`THB · Thai baht`) and opens a searchable index of every one
     of the sixty-seven codes the arithmetic understands, by code or by name. It
     is the same object Preferences uses, and it replaced a three-letter box that
     accepted `ABC`.
   - `BUDGET, IF YOU HAVE ONE` (placeholder `25000`), full width under it.
3. If validation fails, one sentence in **blue** (never red) under the fields:
   “A trip needs a name.” / “The start date should look like 2026-11-24.” / “The
   trip cannot end before it starts.” / “The trip could not be saved. … —
   nothing was lost; try again.”
4. A row: the page's one blue bar **OPEN THE TRIP**, with a quiet **Go back** to
   its right when a trip already exists.
5. Section head `II — OR START WITH SOMETHING IN IT`, then a faint sentence
   explaining the demo roll, then two outlined bars: **DEMO ROLL** (create the
   trip with five photographs already in it) and **PREFILL FROM DEMO** (fills the
   form and leaves you on it). Stacked on a phone, side by side when wide.
6. Footer rule and the faint line
   `NO ACCOUNT · NO SERVER · NOTHING LEAVES THIS DEVICE`.

**States.** Fresh (dates and currency prefilled, budget blank); invalid (one blue
sentence, fields preserved); saving (busy); trip-save failure (the long blue
sentence); “another trip” eyebrow when one already exists; **no trip open** (the
page still opens, so a returned `NewTrip` route cannot strand anyone).

**Leaves to.** Open the trip / Demo roll → Home with the new trip active. Go back
→ Home.

**Design notes.** The page is deliberately the least decorated form in the app:
no card, no input outlines, no labels floating inside fields. The dates are
prefilled so the fastest legitimate path is *type a name, tap the blue bar*.

---

### 5.3 Home — the day you are in

**Route** `Home` · **Plate** `01` · **Bar** shown · **Wide** hanging rail + column

**Purpose.** Answer “where am I today, and is there anything waiting for me?” and
put the shutter one tap away.

**What is on it, top to bottom**

1. Masthead: `01 — ARIADNE`, the trip's name in the display serif **italic**
   (“Bangkok & Home”), and a top-right page action `PREFS` (blue-styled when
   accented; otherwise ink-mid with a 16dp rule under it). Black rule.
2. **The figure.** The currency symbol in ink-faint at 22sp, then the day's total
   as a huge tabular numeral — 52sp phone, 68sp wide — sitting directly on the
   paper with no card, no box, no label above it.
3. A row under the figure: label `FINISHED TODAY` on the left; on the right a
   **pill** — `5 WAITING` in blue wash/blue ink, or `ALL SETTLED` in a hairline
   rectangle with soft ink.
4. **The actions**, side by side: a blue bar at 1.5 weight — **TAKE A PHOTO** with
   a 17dp aperture glyph before the words — and, only when something is waiting,
   an outlined bar at 1 weight — **FINISH 5**.
5. Where the target has a photo picker, a quiet action under them: *“or bring one
   in from the library”*. It opens the picker directly. On a target with no
   picker the line is not shown, because a link that opens nothing is worse than
   no link.
6. Section head `II — THE TRIP`, with the date range right-aligned
   (`2026-11-24 → 2026-11-30`).
7. Five index rows, each separated by a hairline, each a 16dp glyph hanging in the
   left margin, a 19sp serif title, and a small sans detail on the right:
   - `Trips` — “the register” / “4 in the register”
   - `Timeline` — “0 of 7 days closed”
   - `The record` — “5 kept today”
   - `Search what you kept` — “by shop, price, day”
   - `The trip's page` — “dates, currencies, the card”
   These rows fade in one behind the next.
8. A faint closing line: *“No account. No sync. The record stays here.”*
9. 96dp of bottom air (the bar floats below).

**Wide rail.** `TODAY 2026-11-24`, then `DAYS 7 / closed 0`, then
`WAITING 5 / photographs`, left-aligned so the figures form a column.

**States.**
- **A closed trip is bound:** the figure, the shutter and the index are replaced
  by a serif sentence — *“This trip is closed. Nothing new can be filed against
  it; the record and the card stay exactly as they were.”* — with the page's one
  blue **SEE THE CARD** and an outlined **THE REGISTER**. The bar stays, but its
  shutter loses its blue, its tap target and its `Button` role, and is announced as
  *“A closed trip takes no photographs. See the card instead.”* — so the one control
  that could quietly break the closed state is closed with it.
- **No trip, nothing on disk:** headline becomes “No trips yet”, a serif sentence
  (“A journey, from its first day to its last. Photographs now; figures at
  nightfall.”), blue **START A TRIP**, outlined **BEGIN WITH THE DEMO ROLL**.
- **No trip, but trips exist (all closed):** headline “No trip open”, a serif
  sentence saying every trip on the device is closed, blue **START A TRIP**,
  outlined **THE REGISTER**.
- **Nothing waiting:** the pill reads `ALL SETTLED`, the Finish button disappears.
- **Everything waiting, nothing finished:** total `0.00`, the whole page still
  points at the shutter.

**Leaves to.** Shutter (blue action), the picker (library link), Nightfall (Finish N),
Preferences, Trips, Timeline, Record(today), Search, Trip.

**Design notes.** This is the template the other tab pages follow: one masthead,
one figure, one blue action, a ruled index. The figure is deliberately not in a
card — a number that size carries its own emphasis, and the box around it would
be the default container of every generated layout.

---

### 5.4 Trips — the register

**Route** `Trips` · **Plate** none (the shelf the plates sit on) · **Bar** shown with **no destination selected** (stacked from Home) · **Wide** scroll rail in the margin

**Purpose.** Every journey this device holds, open and closed, ordered by start
date with the newest first. One
is active — the one a photograph taken now belongs to — and that is shown by the
page's single blue mark rather than by a badge saying “active”.

**What is on it, top to bottom**

1. Masthead: eyebrow `THE REGISTER` (no plate number), headline **“Trips”** in
   italic serif, lede *“Every journey this device holds. One of them is open.”*,
   a top-right `HOME` page action back to today, a black rule, then a second
   black rule.
2. The entries, numbered like a register — `01`, `02`, … in a 34dp left column:
   - trip name in serif 21sp (ink when active, ink-mid when closed);
   - under it, a 13dp place pin and the place in tracked caps, then the dates as
     a printed range: `24 Nov — 30 Nov`;
   - on the right, the trip's budget figure (or `—`), and under it either a 22dp
     **blue tick** (the active, open journey) or the trip's own state in tracked
     caps: `OPEN` or `CLOSED`. An open trip that simply is not the active one is
     not called closed.
   - A hairline separates entries.
3. A 30dp gap, then an outlined **BEGIN A NEW TRIP** (deliberately not blue: the
   blue belongs to the open journey).
4. When the register is empty, a quiet *“Or open the demo roll”*.
5. A rule, then three labelled shelf figures sitting on the lower rule:
   `JOURNEYS 3` · `OPEN 1` · `CLOSED 2` (26sp light numerals over 9sp labels).
6. A serif sentence: *“One at a time. A photograph taken now belongs to the open
   journey.”*
7. The scroll rail sits in the outer margin.

**States.** Empty (“No trips yet.” + demo link); one trip; many; closed entries
(no blue, name in `inkMid` — one step down, still comfortably legible, never
ghost); no budget (`—`).

**One target, one sentence.** The whole row is the tap target and the whole row is
one accessibility node: `"Bangkok & Home, 24 Nov — 30 Nov, open, the one
photographs go into"`, or `…, closed`. A screen reader should not have to assemble
a trip out of four labels, and the row should not be reachable only by its title.

**Leaves to.** Tap an entry → that trip becomes active → Home. Begin a new trip →
New trip.

**Design notes.** The register is the only list page with no photograph at all.
It earns its space with arithmetic — three counts under the list — so the page
never ends in a field of white.

---

### 5.5 Timeline — the trip as a run of days

**Route** `Timeline` · **Plate** `02` · **Bar** shown · **Wide** rail + column

**Purpose.** See the trip's rhythm: which days were heavy, which were empty, and
*when* inside a day things happened. It never scores, streaks or scolds.

**What is on it, top to bottom**

1. Masthead: `02 — BANGKOK & HOME`, headline **“Timeline”** in italic serif, lede
   *“Each day, and the hours it was spent in.”*, top-right page action `RECORD`.
   Black rule.
2. One **day plate** per day of the trip, in date order, each 18dp apart with a
   hairline under:
   - **The spine** in a 64dp left column: the day number at 30sp, then the month
     and weekday as two 9sp tracked labels (e.g. `24 / NOV / TUE`).
   - Beside it: a status label in tracked caps — `STILL OPEN` in blue, or
     `IN THE RECORD` in faint — and, right-aligned, the day's figure (15sp
     tabular) when there are photographs.
   - Then the **24-hour band**, 54dp tall: a horizontal rail with hour labels
     `00 06 12 18 23` under it, and one mark per capture at the hour it happened.
     Finished captures are small filled ink dots with a 1.4dp stem; unfinished
     ones are outlined blue rings with a fainter stem. A legend under the rail:
     `● RECORDED  ○ STILL OPEN`.
   - A day with nothing on it shows a hairline instead of a band and the words
     `NOTHING KEPT` — no gap, no guilt.
3. 96dp bottom air.

**Wide rail.** `CLOSED n`, `OPEN n`, and the line `the trip as a run of days`.

**States.** No dates (“A trip has no days until it has dates.” + **START A TRIP**);
a trip of empty days (a page of hairlines, deliberately calm); mixed days; all
closed.

**Leaves to.** Tap a day → Record for that day. Record action → Record(today).

**Design notes.** The left date spine is the whole idea: the same content stacked
in one column would read as a feed, which is the shape every generic app has.
The band is a *reading* of data, not a chart: no axes beyond five hours, no bars,
no targets.

---

### 5.6 Stats — what it came to

**Route** `Stats` · **Plate** `03` · **Bar** shown · **Wide** rail + column

**Purpose.** Answer “what did this trip cost, and what did it cost it on?” — and
say honestly that only finished photographs count.

**What is on it, top to bottom**

1. Masthead: `03 — BANGKOK & HOME`, headline **“What it came to”** in italic
   serif. (No lede.) Black rule.
2. Label `THE TRIP SO FAR`, then the trip total as the page's largest figure:
   currency symbol at 22sp in faint, then 50sp phone / 64sp wide tabular
   numeral, counting to its value when it changes. Directly on the paper.
3. Immediately under the total's black rule, a 9sp tracked qualifier:
   `FINISHED PHOTOGRAPHS ONLY`. It belongs to the figure it qualifies —
   a total is read before anything three sections below it — and the longer
   sentence in step 4 remains for the person who wants the reason.
4. A **black rule**, then, if a fence was set, a single ruled row:
   `AGAINST A BUDGET OF  ₹25,000.00` on the left; `LEFT 25,000.00` (or `OVER
   25,000.00`, in blue) on the right. Overspend uses the same type size and the
   same tone; only the colour changes.
5. A faint explanatory paragraph: *“Only photographs that were finished. Anything
   still waiting tonight is not in this figure, and is not counted as zero.”*
6. Section head `II — WHAT IT WAS` (only when there are category totals), then
   the **breakdown**: one ruled row per category —
   - a 17sp serif category name (or *“Not said”* in italic for uncategorised),
   - under it, `3 PHOTOGRAPHS` in 9sp tracked caps,
   - the amount right-aligned in 16sp tabular,
   - under both, a 3dp proportional bar: ink for a stated category, ghost for
     “not said”, on a sunk track. Proportions are relative to the largest row,
     not to the total, so small rows remain visible.
   - at the end, a 13dp row: `OF  ₹X`.
7. A serif **pattern line**, shown only when one category is at least 30% of the
   categorised spend: *“Most of this trip so far went on shopping — 75% of it,
   across 3 photographs. Everything else is in the column above.”* It never
   compares the traveller to other people, to a previous month, or to a target.
8. A faint note: *“Nothing here is sorted for you. A category is a claim you make
   on the photograph, and most photographs never need one.”*
9. Section head `III — BY CURRENCY, BEFORE CONVERSION`, then one ruled row per
   currency: a 16dp tick, the code in 18sp serif, the amount at 17sp on the
   right, and a footnote `THESE ARE THE AMOUNTS AS THEY WERE PAID, NOT CONVERTED`.
10. A blue bar **CLOSE TODAY**, disabled (sunk, faint type, no blue) when nothing
    is waiting, when the day is already filed, or when the whole trip is closed.
11. 96dp bottom air.

**Wide rail.** The trip total at 16sp, `PHOTOGRAPHS n` at 26sp, and `finished only`.

**States.**
- **Nothing tallied:** total `0.00`, the fence row still visible if set, and the
  paragraph *“Nothing has been tallied yet. The figures appear here the first
  time you finish a photograph at nightfall.”*
- **No fence:** the budget row disappears entirely.
- **No currencies:** section III disappears.
- **Nothing waiting, or the day already filed, or the trip closed:** Close today
  is disabled and renders sunk and faint, not blue. `closeDay` refuses a closed
  trip independently of the button.

**Leaves to.** Close today → Nightfall.

**Design notes.** The breakdown is a ruled column, not a donut, because a ledger
should be readable to the rupee. It is always in the trip's base currency,
converted through the same table the ritual uses, so the breakdown and the total
can never disagree. The two breakdowns are the same finished photographs seen
from two sides.

---

### 5.7 Preferences — the short, honest settings page

**Route** `Settings` (and the retired `Prefs` alias) · **Plate** `04` · **Bar** shown · **Wide** rail + column

**Purpose.** Change the few things that can be changed, and state plainly the
things that cannot. It is a corner, not a destination: reached from Home's
top-right action.

**What is on it, top to bottom**

1. Masthead: `04 — ARIADNE`, headline **“Preferences”** (upright serif), a
   top-right page action `DONE`. Black rule.
2. Section head `I — THE RECORD`:
   - **Base currency** — 17sp serif name, a 9sp note *“Every total is stated in
     this one”*, the current code as an 18sp figure at the right, a 14dp pencil
     glyph. Tapping unfolds a search line and six catalogue rows (code, name,
     sign; the chosen one carries a blue tab under its code), so the currency the
     trip is actually in can always be found and chosen back. A rule closes it.
   - **Home currency** — same object, note *“What a foreign amount is converted
     back to”*.
   - **The scope is stated, not implied.** At the foot of every unfolded
     catalogue the same two lines appear: `THE 67 CODES THE ARITHMETIC READS`, and
     *“A receipt in any other code is still read and shown as it is; it simply
     cannot be a trip's own currency.”* It is the same component in all three
     places it appears, so the three cannot drift apart.
   - **Dates** — a read-out row: today's date, `ISO order`. No control, because
     it cannot be changed.
   - **Paper** — a real control: three hairline chips, `LIGHT` · `SYSTEM` ·
     `NIGHT`, each a 44dp target. `System` follows the device's light or dark
     setting and says so under the label; the choice is stored with the settings
     and read before the first frame. This row used to be a read-out saying
     “Light paper only”, which was honest and also a description of a bug.
3. Section head `II — THE EXCHANGE`:
   - one **rate row** per currency in use, drawn as a sentence:
     `1 THB = [ rate ] INR`, with the code in 17sp serif, a blue tick if the trip
     actually holds that currency, and a `CLEAR` quiet action when a rate is on
     file. The rate field is a 104dp sunk box, not a hairline, because it is the
     one thing on the page you have to find and hit.
   - Under each row, a 9.5sp state line: the stored rate; or `NO RATE ON FILE`; or
     `NO RATE ON FILE — THIS CURRENCY IS NOT BEING CONVERTED`; or `NOT A NUMBER`.
   - **One open editor at a time.** A closed row is one line — the state
     sentence and a blue `SET RATE` (or `CHANGE`) — and tapping it opens the field,
     which asks for the keyboard. Opening another row closes the first, and
     cancelling throws the half-typed rate away rather than leaving it looking
     saved. Two live ruled boxes on one page is a form; this page is a ledger with
     one line you can edit.
   - `SET` appears in blue only once something is typed.

   The rows are keyed by currency, so a typed rate cannot drift to the row below
   when the list changes.
4. Section head `III — WHAT LEAVES THIS DEVICE`, a read-out (`READING — ON DEVICE
   ONLY`) and a paragraph stating that there is no account, no server and no
   network call in the product.
5. Section head `IV — THIS TRIP`: a 16dp trip glyph, the trip name in 17sp serif,
   the place in tracked caps on the right. Under it two icon actions:
   `SEE THE TRIP'S PAGE` and `CLOSE THE TRIP INTO A CARD` — or, when the trip is
   already closed, the same two slots with `SEE THE CARD` in place of the second:
   a finished journey cannot be finished twice.
6. Another rule, then **Delete this trip**: a 16dp cross and a tracked label, a
   12sp explanation of exactly what disappears, and — once armed — the blue
   warning-voiced line `THIS DELETES EVERYTHING, NOW` followed by an ink bar
   `DELETE FOR GOOD` and an outlined `KEEP IT`.
7. 96dp bottom air.

**Wide rail.** `on device only`, `no account · no sync`.

**States.** Open rate panel / closed; rate on file / missing / refused; delete
unarmed / armed; busy while deleting; a failure note if the app caught one.

**Leaves to.** Done → Home. Trip page. Close trip → card. Delete → New trip.

**Design notes.** Every row either changes something or states a fact. No row is
decorated to look like a control it is not. The exchange rate is surfaced because
a missing rate makes every total quietly wrong — and the page says so rather than
showing an innocent-looking zero.

---

### 5.8 Trip — the trip's own page

**Route** `Trip` · **Plate** `09` · **Bar** shown · **Wide** single scrolling column with the margin rail

**Purpose.** The trip as an object rather than a list: dates, progress, the
currencies actually touched, and the one action that closes it.

**What is on it, top to bottom**

1. Masthead: `09 — TRIP`, the trip name as the display headline (upright serif),
   top-right `BACK`. Black rule.
2. Three **date blocks** spread across the measure: `FROM 2026-11-24`,
   `TO 2026-11-30`, `DAYS 7` (9sp labels over 14sp tabular figures).
3. If a place or fence exists: a hairline, then a row with a place pin and the
   place in tracked caps on the left, and on the right a fence glyph,
   `FENCE`, the budget, and `₹X LEFT` or `₹X OVER`.
4. A rule, then `PROGRESS`, the sentence **“0 of 7 days closed”** at 22sp serif,
   and a **measured rule**: a 3dp ink line for the closed fraction, a 1dp edge
   line for the remainder, with 1dp ticks every 24px underneath. It is a ruler,
   not a progress bar.
5. `SO FAR`, then one figure row per currency touched (`THB 1,200.00`), a rule,
   and one emphasised row `In INR ₹0.00`. If nothing is tallied: *“Nothing
   tallied yet. The first capture is a photograph, not a form.”*
6. `CURRENCY`, then a paragraph: *“Base INR · home INR. The original amount is
   never converted away — it stays under the figure it became.”*
7. The page's one blue bar: **CLOSE THE TRIP INTO A CARD**. On a trip that is
   already closed it reads **SEE THE CARD**, because a trip is not closed twice.
8. A quiet action under it: *“Every day of it”* → Record.

**States.** Open / already closed; with or without place and fence; no figures
yet; per-currency rows present.

**Leaves to.** Close trip → TripCard. Back → Home. Every day of it → Record.
Preferences also offers “See the trip's page”.

**Design notes.** The progress rule is the page's signature: a printed ruler with
ticks, so “closed” reads as a measured quantity rather than a percentage.

---

### 5.9 Capture — the shutter

**Route** `Capture` · **Plate** none · **Bar** hidden · **Full-bleed**

**Purpose.** Take the photograph with zero decisions. It exists to make capture
the easiest thing in the app.

**What is on it, top to bottom**

1. A top row, no masthead: `HOME` quiet action, the trip name as a centred 10sp
   tracked label, `TODAY` quiet action (→ Nightfall). Hairline under it.
2. Generous empty paper (weight 0.6) above the plate.
3. **The plate**, square, full measure, hairline-framed: the live camera when one
   is bound, otherwise a ruled field with crop marks in the four corners and a
   register cross in the middle. Caption inside the bottom edge, in 10sp tracked
   caps: *“TAP ANYWHERE TO KEEP”* or *“NO CAMERA HERE — TAP TO KEEP A PLATE”*.
   Tapping the plate fires the shutter.
4. Empty paper (weight 0.5) below.
5. A control row: `FROM THE LIBRARY` quiet action in the left third, shown only
   on targets that have a photo picker; **the shutter** in the centre — a 66dp
   hairline ring around a 46dp ink disc, nothing else; and in the right third
   `5 WAITING` / `NOTHING WAITING` (quiet action → Nightfall), faint when empty.
   The shutter stays centred whether or not the library link exists.
6. A hairline, then a centred footer with a 12dp fence glyph:
   `NO FIGURE HERE · SETTLE IT AT NIGHTFALL`.

**States.** Camera available / not available (the caption changes, the plate
becomes ruled); waiting / nothing waiting; permission refused (the plate stays,
the caption says so, the app stays usable); **closed trip** (the plate is
replaced by *“This trip is closed. Its photographs are still here, and its card is
still yours.”*, with a blue **SEE THE CARD** and an outlined **THE REGISTER**).

**Leaves to.** Shutter → Frame. Home. Today → Nightfall. Library → the platform
picker; picked bytes take the same path as a camera photograph and land on Frame.

**Design notes.** No amount field, no category, no form, no plate number. The
inset plate and wide paper margins make it read as a plate in a book rather than
a camera app. The shutter is the only circle and the only physical-feeling
control.

---

### 5.10 Frame — the photograph, held up

**Route** `Frame(captureId)` · **Plate** none · **Bar** hidden · **Wide** 1.4:1 plate

**Purpose.** The shutter used to throw the person back into the app and the
photograph vanished into a queue they could not see. Now the frame stops the page
and asks the only question worth asking: is this everything, or is there more to
it? Neither answer loses the photograph.

**What is on it, top to bottom**

1. A top row: `TAKEN` in 9sp tracked caps on the left, `HOME` quiet action on the
   right; hairline.
2. The **plate**: square on a phone, 1.4:1 wide, hairline-framed. The real
   photograph, or its engraved study (e.g. the chai cup) if none is bound.
3. The merchant (or **“Kept”**) in serif italic at 26/30sp.
4. A row: the date in 9sp tracked caps on the left; the amount, if one was read,
   right-aligned at 15sp tabular.
5. A **black rule**, then a small reassurance: a 13dp check glyph and
   `NOTHING HERE IS REQUIRED`.
6. The two answers, stacked, neither behind a swipe: the blue **FILL IT IN NOW**
   and the outlined **KEEP IT FOR NIGHTFALL**.
7. `5 WAITING TO BE FINISHED` in 9sp tracked caps.
8. A hairline, then two quiet actions side by side: `Take another`, `Nightfall`.

**States.** Extraction still running (no amount shown); amount read (figure on the
right); photograph missing (engraved study, caption); kept without an amount
(`nothing here is required` + the stack count).

**Leaves to.** Fill it in now → Detail. Keep it for nightfall → Home. Take another
→ Capture. Nightfall → the ritual.

**Design notes.** The photograph is the largest thing on the page, and both exits
are full-width bars rather than a swipe, so the choice is legible at a glance.

---

### 5.11 Nightfall — the ritual

**Route** `Nightfall(day)` · **Plate** `05` · **Bar** hidden · **Wide** a two-page spread

**Purpose.** The heart of the product: turn a day of photographs into a day of
figures in minutes, one card at a time, two taps each. Nothing here warns,
scolds, or suggests a target. The only judgement on the page is the total, and it
is just a number.

**The ground.** This is the one page that is not a record of something; it is a
calm commit. It sits on the **sunk** paper (light mode), not the page white.

**What is on it, top to bottom (phone)**

1. Masthead: `05 — 2026-11-24`, headline **“Nightfall”** (upright serif). On the
   right, instead of a page action: `TODAY` in 9sp caps over the day's total at
   19sp tabular.
2. The scrollable middle (only this scrolls):
   - the **plate** at 1:1, hairline-framed, with the photograph or engraved study
     (the same plate geometry as every other surface — it used to be 16:9 here);
   - the merchant name at 24sp serif, or `Untitled`;
   - under it a 9sp line that says where the figure came from: *“read from the
     photograph”* / *“nothing read — type it, or leave it”* / *“kept without an
     amount”*;
   - on the right, the currency code and a quiet `EDIT` that opens Detail.
3. Pinned: label `AMOUNT`, then the currency symbol at 20sp and the typed amount
   as a 44sp tabular numeral (`0` when empty).
4. Pinned: the decisions, **stacked rather than side by side** — blue
   **CONFIRM** first, outlined **NO AMOUNT** under it. If an amount was extracted
   the pair becomes blue **YES, SAVE** and outlined **NO, ENTER IT**, with a serif
   question above: *“Is it ₹40.00?”*. Two bars sharing one phone row fit at 100%
   text and clip at 200%; the phone is where 200% actually happens, so they get a
   line each, in the same order as the wide layout.
5. When several cards already have amounts: an outlined **CONFIRM 3 READY**
   above the row. With more than one card: **CONFIRM REST AS NO AMOUNT** below it.
6. A footer **column**, not a row: `1 OF 5` (tapping goes back) and under it
   `NEXT PHOTOGRAPH` — `CLOSE THE DAY` on the last card. At 200% the two labels do
   not fit across a phone, and a footer that reflows beats one that truncates.
7. The **ruled keypad**: four rows — `123 / 456 / 789 / 0 . ⌫` — drawn as a grid
   of 1dp hairlines, 50dp per row, no fills, 21sp tabular digits, the backspace
   glyph in the last cell. It is a till slip ruling columns, not a calculator.

**Wide layout.** A spread, 44dp between pages:

- **Left page:** the plate fills the available height (not a fixed 4:5 box — that
  overran laptops); under it `PLATE 2 OF 5` and quiet `EARLIER` / `LATER`.
- **Right page:** merchant header, rule, amount, rule, then:
  blue **CONFIRM** (or `YES, SAVE IT` / outlined `NO, LET ME ENTER IT` when an
  amount was read, with the serif question above), outlined **CONFIRM 3 READY**
  with a caption, quiet **NO AMOUNT**, outlined **CONFIRM REST AS NO AMOUNT**,
  a rule, outlined **CLOSE THE DAY**, and the caption *“Closing files the day.
  Any card can still be edited from the record.”*

**Empty state (nothing waiting).** Headline still Nightfall, then:
**“The day is finished.”** at 28sp, the serif sentence *“Everything you kept today
is tallied. Nothing is waiting, and nothing is scolding you for it.”*, a rule, a
blue **CLOSE THE DAY**, and a quiet **Back to the record**.

**Empty state on a closed trip.** The same page with no blue at all: *“This trip
is closed, so there is no day left to file.”* and a single quiet **Back to the
record**. `closeDay` refuses a closed trip even if the bar were reachable, so the
sentence and the guard say the same thing.

**Keyboard (desktop).** Digits and `.` type the amount; Backspace deletes; Enter
confirms; J / Down and K / Up move through the stack; Escape leaves for the
shutter.

**Leaves to.** Confirm → next card, or Record when the stack empties. No amount →
next card. Edit → Detail. Close the day → Record. Close rest → Record.

**Design notes.** The tightest constraint in the app: **two taps finish a card**,
so nothing that finishes a card is ever below the fold. On a phone only the plate
scrolls. The keypad is the clearest example of the language — hairlines instead of
filled keys, because rounded white keys would be the most generic object on the
most important page.

---

### 5.12 Record — a day, in full

**Route** `Record(day?)` · **Plate** `06` · **Bar** shown · **Wide** scrolling margin

**Purpose.** A day, in the order it happened, with the total ruled off at the
bottom the way a till slip is ruled off. There is no table anywhere on this page,
and that is deliberate.

**What is on it, top to bottom**

1. Masthead: `06 — BANGKOK & HOME`, headline **“The record”** (upright serif),
   top-right page action `CAPTURE`. Black rule.
2. The day being read, as type: `2026-11-24` at 24sp serif.
3. The **day strip**: a horizontally scrolling row of 64dp boxes, one per day —
   day number at 17sp, month label in 9sp caps under it. The selected day has a
   1dp ink border; closed days sit on sunk paper with a 14dp ink underline inside;
   open days sit on page paper with a hairline border.
4. The day's captures as **receipt lines**, one per row:
   - a 54dp square thumbnail (the photo or an engraved study), hairline-framed;
   - the merchant in 16sp serif, or `Untitled`;
   - under it a 9sp marker: the currency code, or `NO AMOUNT`, or `NOT FILLED IN`;
   - the amount right-aligned in 16sp tabular, or a faint `—`;
   - a soft hairline under each.
5. A rule, then the total block: one figure row per currency touched, a rule, and
   an emphasised **`Tallied`** row with the day total.
6. If anything is waiting: a faint sentence — *“5 cards are still open. Nothing is
   in the total until nightfall — the figures above are what the photographs
   said, not what you have confirmed.”*
7. The page's one blue bar: **NIGHTFALL**. On a closed day — or when the whole
   trip is closed — it is deliberately inert (sunk fill, faint type): there is
   nothing left to settle. A closed trip also gets one sentence under the day's
   heading:
   *“This trip is closed. The record is readable and its card is yours; nothing can
   be added to it.”*
8. Two quiet actions in a row: `Search`, `Trip`.
9. A closing note, depending on the day: *“This day is closed. Its cards can still
   be edited from here.”* or a quiet **Close this day**.

**States.** Empty day (*“Nothing kept on this day. An empty day is still part of
the trip — it is the one you spent walking.”* + **ADD A PHOTOGRAPH**); day with
captures, some unfinished; all finished; closed day; open day.

**Leaves to.** Row → Detail. Nightfall → ritual. Capture. Search. Trip. Close this
day → stays on Record.

**Design notes.** The strip is the trip in miniature; the rows are the day in
order. Both are ruled, neither is tabular. The total only ever holds finished
cards, and the page says so — that is the difference between a number you trust
and a number that looks broken.

---

### 5.13 Detail — one photograph, open in the hand

**Route** `Detail(captureId)` · **Plate** `07` · **Bar** hidden · **Wide** two-page spread

**Purpose.** The place to correct or complete a single record: a shop name, a
currency, a category, a line to remember it by. It exists because a night of
triage cannot be the only editing surface. Everything here is optional.

**What is on it, top to bottom (phone)**

1. Masthead: `07 — 2026-11-24`, headline **“The photograph”** in italic serif,
   top-right quiet `BACK`. Where Back goes is decided by `detailOrigin`: to Record
   for that date when the page was opened from a row, or to the Search results —
   query and results intact — when it was opened from a result.
2. The **plate** at 1:1 on every width, hairline-framed, with the photo or engraved
   study; under it, the file-state name in tracked caps (`needs amount`,
   `confirmed` …).
3. **Amount**: label, then the currency symbol at 20sp and a 44sp tabular numeral.
   If an amount was extracted: the serif question *“Is it ₹40.00?”*, a blue **YES,
   SAVE IT**, and an outlined **NO, LET ME ENTER IT**.
4. A rule, then the **keypad** (phone: pinned to the bottom edge; wide: inside the
   right column). **The ruled keypad is the only amount input on this page**: the
   amount has no text field, so the system keyboard never opens over it and the
   two cannot disagree about what was typed. The IME is for the merchant, note and
   category fields above, and the root's inset handling keeps the pinned keypad and
   the blue Save above it.
5. **Currency chips**: the current currency as an ink-filled chip with paper-white
   type, then up to three outlined alternatives, drawn from the catalogue's
   shortlist — this capture's currency first, then the trip's base and home
   currencies, then THB / USD / EUR — so a trip created in JPY can always be
   switched back to JPY. Tapping one changes the currency for this capture. The
   full catalogue (with search) is on New trip and Preferences, not here: this row
   is a shortcut, and four chips is the width of a phone.
6. `WHERE` — a line field in sans 18sp over a strong hairline, placeholder *“the
   shop, if you want it named”*.
7. `WHAT IT WAS` — six category chips in two rows of three:
   `FOOD · TRANSPORT · LODGING` / `SHOPPING · ENTERTAINMENT · OTHER`. Unselected:
   white panel, hairline, soft ink. Selected: blue wash, blue edge, blue type.
   Tapping the selected one again clears it. Under them: `OPTIONAL · NOTHING IS
   SORTED FOR YOU`.
8. `REMEMBER IT BY` — a serif 22sp line field over a hairline, placeholder *“a
   line for later”*.
9. The page's one blue bar: **SAVE**. Under it an outlined **KEEP IT WITHOUT AN
   AMOUNT**, and under that a quiet **Leave it open**.

**Wide layout.** The plate on the left page (1:1, state label beneath), the fields
and keypad on the right page, 44dp between them.

**States.** Capture missing (*“That photograph is not in the record any more.”* +
**BACK TO THE RECORD**); extracted amount awaiting a yes/no; no amount yet; already
confirmed; typed amount buffer being edited; **the trip is closed**.

**A closed trip is read-only here.** The page keeps its plate, its figures, its
currency and its categories **as printed**, and loses every input: no keypad, no
extracted Yes/No, no Save, no "Keep it without an amount", no "Leave it open". The
merchant and note fields render as type rather than as fields (a cursor promises a
change), the currency and category chips stay legible and stop being targets, and
one sentence says why. `AppModel.saveDetail` refuses as well, so a stale route or a
restored process cannot write to a finished record — the page says it, and the
model enforces it.

**Leaves to.** Save → Record for that day. Keep it without an amount → Record. Leave
it open → Record. Back → Record, or Search when that is where it came from. (Frame's
"Fill it in now" also lands here.)

**Design notes.** The page reads as the back of a print: one photograph, a ruled
column of facts, no boxed inputs. The category is a claim, never a guess.

---

### 5.14 Search — remember a thing

**Route** `Search` · **Plate** `08` · **Bar** shown · **Wide** scrolling margin + rail

**Purpose.** Retrieve a specific memory by what you remember about it: the shop,
the figure, the currency, or the day. Results are ruled rows because you
recognise the shoe, not the string “Bata Footwear”.

**What is on it, top to bottom**

1. Masthead: `08 — THE RECORD`, headline **“Remember a thing”** (upright serif),
   top-right page action `RECORD`. Black rule.
2. The **query**, set as type rather than as a form field: a 26sp serif line with
   the placeholder *“shoes · 1200 · THB · 21 Nov”*, focused on arrival, cursor in
   ink, over a **black rule**. It never scrolls away.
3. The answer area:
   - **Empty query:** a serif paragraph (*“Type a shop, a price, a currency, or a
     day. Everything you kept is in here — including the things you left without
     an amount.”*), a section break labelled `HOW YOU CAN ASK`, then four index
     rows that each run a real search when tapped:
     `By the shop — shoes`, `By the figure — 1200`, `By the currency — THB`,
     `By the day — 21`. Each has a glyph in the left margin.
   - **No results:** *“Nothing by that name. Try a price, or the day it happened.”*
   - **Results:** `5 KEPT` in 9sp caps, then rows: a 58dp square plate thumbnail
     (engraved study keyed to the merchant), the merchant in 16sp serif, the ISO
     date in 9sp caps, and the amount right-aligned at 16sp. A result opens the
     photograph, because that is what it looks like.
4. The results scroll; the query stays fixed; a scroll rail sits in the margin.

**States.** Empty query (an index, not a blank page); no results; results.

**Leaves to.** A result → Detail for that photograph (the day is one quiet Back
away, from Detail). Record action → Record(today).

**Design notes.** The empty state is deliberately an index of four ways in: one
sentence under a search box reads as unfinished; four ruled rows read as a
catalogue.

---

### 5.15 Trip card — the thing you keep

**Route** `TripCard(tripId)` · **Plate** none · **Bar** hidden · **Wide** 96dp margins

**Purpose.** The payoff and the end of the product. Everything the app has been
collecting arrives as one column of type you could print and keep. A trip that
has not finished still shows this page — it just says so.

**What is on it, top to bottom**

1. A top row: quiet `BACK TO THE RECORD` on the left; on the right a 14dp share
   glyph and `SHARE`, which becomes `COPIED` in blue for a moment after copying.
2. **The card itself**, paper within paper: page-coloured, hairline-framed, 26dp
   inner padding on a phone and 56dp when wide, 40dp vertical.
   - `A TRIP, KEPT` in 9sp tracked caps.
   - The trip name as the display headline, 38sp upright serif.
   - The dates as a tracked line: `2026-11-24 — 2026-11-30`.
   - A **black rule**.
   - Every day of the trip as a ruled row: the ISO date in 13sp tabular on the
     left, `OPEN` / `CLOSED` in 9sp caps on the right.
   - A hairline; then, if there are figures, one figure row per currency, a black
     rule, and an emphasised **`Total`** row in the base currency. If not:
     *“No figures recorded yet. The photographs are still the record.”*
   - A rule, then one closing sentence: every day closed →
     *“Every day closed. This is the whole of the trip: no account, no sync, and
     nothing about it left this device.”*; otherwise → *“3 of 7 days closed. The
     card keeps filling itself in until the trip is done.”*
3. Under the card: one quiet **Back to the trip**.

**One blue action.** `SHARE THE CARD` in the top-right corner is the page's single
blue thing and the only way to copy it: an icon, tracked caps, a 44dp target, and
after a copy it becomes a tick and `COPIED` in `inkMid`. There used to be two —
that corner control *and* a blue `COPY AS TEXT` bar at the foot — which made a
keepsake look like a dialog with two buttons. The text that is copied is built from
the same state the page is printing, so the shared numbers cannot disagree with the
ones on the card.

**States.** Complete trip / incomplete trip; no figures; copied confirmation.

**Leaves to.** Share the card (clipboard). Back to the trip. Back to the record.

**Design notes.** This is the only page whose content is a single object with a
frame around it — but the frame is a hairline on the page's own paper, not a card
floating on grey. The page is printed on `TripGround`, the warmer stock, and the
card itself is the one raised sheet on it: the keepsake is a different paper from
the rest of the book.

---

## 6. Recurring objects (the component vocabulary)

| Object | What it is, in words |
| --- | --- |
| **Masthead** | Plate number, hairline, eyebrow, display headline, optional serif lede, black rule. Binds every page. |
| **Rule** | 1dp hairline. Ink for major breaks, Edge for rows. |
| **Primary action** | 56dp blue bar, 2dp corners, 11sp semibold tracked capitals, optional 17dp glyph. One per surface. |
| **Outline action** | 56dp hairline rectangle, ink capitals. “Available, not now.” |
| **Quiet action** | 11sp tracked uppercase with a 1.5dp underline, in a box that is **at least 44×44dp** whatever the length of the words. Quiet in weight, not in the area a thumb has to find; it grows with the label at 200% rather than clipping it. Secondary navigation. |
| **Page action** | 10sp tracked caps with a short rule under it, top-right of a masthead. |
| **Pill** | Rule-bordered rectangle, 1dp corners, 9sp tracked caps. Blue wash when it counts something waiting. |
| **Nav row** | Glyph in the left margin, serif title, small sans detail at the right, hairline under. |
| **Figure row** | Name left, tabular figure right, hairline under; emphasised variant for a total. |
| **Rail figure** | Label above, figure below, hung in the 132dp wide-page rail. |
| **Field** | 9sp label, value, then the rule that is the field. No box. |
| **Line field** | Same, but the value is typed directly on the page in serif or sans. |
| **Plate** | A framed photograph, or a ruled ground with an engraved line study and a caption. Always the largest object on its page. |
| **Day band** | 54dp, 24-hour rail, filled marks for finished, outlined for waiting, five hour labels, two-word legend. |
| **Breakdown** | Ruled category rows with a 3dp proportional bar, plus an “OF ₹X” line. |
| **Measured rule** | Solid ink for the done fraction, ticks at 24px, edge line for the rest. |
| **Ruled keypad** | 4×3 grid of hairlines, no fills, tabular digits, backspace in the last cell. One shared component (`RuledKeypad`), with minimum row heights so a large system font grows the keypad instead of clipping the digits. |
| **Currency field** | A row stating code and name, opening a search line and catalogue rows. The only way to choose a currency anywhere in the app. |
| **Scroll rail** | Hairline track in the outer margin, square 2–3dp thumb, no rounding, no hover growth. |
| **Shutter (bar)** | 62dp blue disc in a notch cut from the tab bar, paper ring, aperture glyph, and an ink **pill** badge — `defaultMinSize(17dp)`, horizontal padding — because a fixed 17dp disc clipped a two-digit count at 200% text. When no trip is open the disc keeps its place and loses its blue, its tap and its role. |
| **Waiting count** | A pill on Home, a badge on the shutter, a dot on the Home tab, a line on the shutter page. |
| **Plate (a photograph)** | **1:1 on every surface, hairline frame, no rounding.** One geometry for the viewfinder, the frame, the record row, the triage card, Detail and the card — the same photograph used to be 4:5, 16:10, 16:9 and 1.4:1 in four different places, which made one object look like four. |
| **Bar (tab bar)** | Four destinations and a notch. Height follows the system font scale up to 1.6×. `selected` is nullable: a stacked page keeps the bar and marks nothing. |
| **Icons** | Drawn at one line weight on a 24-unit grid. |
| **Engraved study** | One-subject line drawing on faintly ruled paper. Never photo-real. |

---

## 7. Cross-cutting states and honest failures

| Situation | What the UI does |
| --- | --- |
| No trip at all | Onboarding. It is the only surface reachable from a cold launch with an empty store, and its two acts are “Begin a trip” (New trip) and “Begin empty” (New trip with the draft untouched). This is the one startup rule in the document — §4.1 and §5.1 say the same thing, and a cold launch asks one function, `startRoute(hasAnyTrip, hasOpenTrip)`, for the answer. Opening a trip from the register afterwards is a different, explicit act and lands on that trip's Home. |
| Trips exist, all closed | The Trips register. No open trip means no Home to land on: capture, Record and Stats all describe a trip, and inventing one would be inventing a day boundary. The register's top-right action opens a new trip; Home is one row away. |
| Deep link into a surface with no trip behind it | The same rule, applied again: the shell resolves `startRoute()` and shows that, rather than the linked page in an empty state. A link to a closed trip's capture page lands on the register with one line saying the trip is closed. |
| Photo taken, extraction still running | Frame shows the plate with no figure; Nightfall says “nothing read — type it, or leave it”. |
| Extraction failed | Same card, one faint line; the amount can still be typed or skipped. No error colour. |
| Amount extracted, unconfirmed | A serif question — “Is it ₹40.00?” — with Yes / No. |
| Nothing waiting | Pills read `ALL SETTLED`; Finish and Close-today disable; Nightfall shows “The day is finished.” |
| Empty day | A calm sentence; an empty day is part of the trip. |
| Missing exchange rate | Stated in the rate row and in words: “this currency is not being converted”. |
| Invalid input | One sentence, in blue, naming the field. No red, no shake, no toast. |
| Save/extraction failure | Written down where a person can find it (Preferences carries a failure note); the copy says nothing was lost. |
| Delete | Two taps: armed state names exactly what will go; the only way out that is not deletion is the quieter control. |
| Offline | Nothing changes. There is no offline banner because there is nothing to be offline from. |
| Reduced motion | Every animation duration becomes 0; no cross-fade, no stagger, no count. |
| Night paper | A full dark palette, chosen in Preferences (Light / System / Night) and stored with the settings; `System` follows the device. |
| Long press | No longer claimed anywhere. Closed days are edited from the record, which is what the copy now says. |

---

## 8. Responsive and accessibility notes

- **Breakpoint:** `wide = true` on desktop, and on the web above 900px; phones and
  tablets-in-hand always get the one-column layout. There is no intermediate
  layout.
- **Rail:** foldable, remembered for the session; when folded the column reclaims
  the room over 180ms.
- **Touch targets:** actions are 56dp; the shutter 66dp; rows 44–56dp; quiet
  actions are 44dp square-ish by default (they used to be a 26dp underline and a
  thumb's worth of luck); the category and currency chips keep their small printed
  look inside a 44dp tap target, so density and reach are separate decisions.
- **Semantics:** most controls carry a `contentDescription` and `Role.Button`;
  the shutter announces the waiting count (“Capture a photograph. 5 waiting for
  tonight.”).
- **Contrast:** ink on paper is very high; the risk areas are `inkFaint` on paper
  (metadata at 9sp) and `inkGhost` captions, which are deliberately near-invisible
  and would fail a strict AA check.
- **Type scaling (the 200% audit).** The rule is: reflow, never shrink. What was
  changed for it, surface by surface — the keypad keeps minimum row heights; the
  bar's height follows the font scale up to 1.6×; the shutter badge is a pill that
  grows with its numeral; the timeline's day spine is `widthIn(min = 64.dp)` so a
  60sp day number is not sliced; the timeline's footer is a column; New trip's
  dates stack on a phone; Nightfall's two decisions stack and its footer stacks;
  `QuietAction` is 44dp by default and grows with its label; the register row is
  one node with one sentence. What is still fixed on purpose: the big amount
  numerals and the 9sp metadata labels, both of which are read as figures rather
  than as running text.
- **The stills to check it with:** fifteen frames at `fontScale = 2`, two of them
  in landscape at 900×440 (the shortest viewport the app has to survive), plus the
  two closed-trip states. They are defined in `Shots.kt` and **none has been
  rendered** — this environment has no Compose runtime, so the audit above is
  source-level until someone runs the harness.
- **Keyboard and system bars:** handled once, at the root. It pads the system
  bars, consumes them, then pads the remaining IME height, so the bottom action is
  above both the keyboard and the inset with no dead band between them. `Detail`'s
  amount has **no text field at all** — the ruled keypad is the input — so the IME
  can never open on top of it; the IME serves the merchant, note and category
  fields. The manifest is plain `adjustResize`; it used to say
  `stateAlwaysVisible`, which forced the keyboard up at launch on every page with
  a field on it. Not verified on a device from here: there is no Android build in
  this environment.

---

## 9. What this revision fixed, and what is still open

### 9.1 Fixed after the first review (2026-11-26)

- **One meaning of Home.** The Home tab opens today's page. The register is a
  stacked page one row down Home's index, with a `HOME` action back, and no
  longer claims the Home tab. A single return policy: ordinary actions go back to
  the record or the trip, `Home` means today, and Done in Preferences goes Home.
- **First run is reachable.** With no trip at all, launch opens Onboarding;
  Begin empty → New trip, Demo roll → a seeded trip. If every trip on disk is
  closed, launch opens the register instead, so closed trips remain readable.
- **Plate numbers no longer collide.** `01`–`09` are the pages of the trip
  (Home, Timeline, Stats, Preferences, Nightfall, Record, Detail, Search, Trip).
  New trip, the register, the shutter, the frame and the card carry no number.
- **Closing a trip ends it as a capture target.** The active trip is cleared, the
  register stops marking it, Capture and Home show a closed-trip state with the
  card as the way on, and nothing can be filed against a finished journey.
- **A trip that is bound but closed** no longer claims the register's blue tick,
  and an open trip that is merely not active is labelled `OPEN`, not `CLOSED`.
- **Search results open the photograph**, with the day one Back away.
- **“Bring one in from the library” opens the picker directly.** It is shown only
  where a target actually has a picker; the shutter stays centred either way.
- **Currency selection is symmetric enough to be safe.** Detail offers the
  current code plus the trip's base and home currencies plus THB/USD/EUR, and
  Preferences always includes the trip's current code.
- **Preferences' roman numerals run I–IV**; its Paper row says `Light paper only`
  instead of promising a system theme; its Dates row shows the real date.
- **The long-press claim is gone.** Copy now says closed-day cards can be edited
  from the record, which is true.
- **Record's Nightfall is inert on a closed day**, and Stats' `CLOSE TODAY` is
  disabled on a filed day as well as on an empty stack — a disabled `Action`
  draws on `sunk` in `inkFaint`, so a dead bar is visibly not the blue action. A
  closed trip's page offers `SEE THE CARD` rather than closing it twice.
- **Home with no open trip tells the truth.** If every trip is closed it says so
  and offers the register, instead of claiming there are no trips.
- **Copy drift fixed**: the dangling demo-roll sentence, and the New trip footer
  now makes the same promise as every other page (“nothing leaves this device”).
- **Contrast pass** on informative captions that were `inkGhost` (Capture footer,
  Frame reassurance, register shelf labels, rate-row states).
- **The keypad is one shared component** (`RuledKeypad`) with minimum row heights
  instead of fixed ones, used by both Nightfall and Detail. The progress rule is
  one shared `MeasuredRule`. Chip tap targets are 44dp while the chips stay
  visually small. The shutter is a single accessibility node, so its waiting
  count is announced once.

### 9.2 Fixed in the second pass (same day)

- **Night paper is shipped.** `Settings.Theme` (Light / System / Night) is stored
  with the settings, loaded before the first frame, and resolved at the root:
  `System` asks the platform. Preferences carries three chips where a read-out
  used to say "Light paper only".
- **One vocabulary for money.** `CurrencyCatalog` holds all sixty-seven codes the
  arithmetic understands, with a name and a sign each, searchable by code or name.
  New trip and Preferences choose from it; Detail's chips are its four-code
  shortlist. The three-letter box that accepted `ABC` is gone. (`PYG`, `XOF` and
  `XAF` were scaled but not "known"; they are now.)
- **The one-blue rule is audited.** Every surface gets an `AccentBudget`; an
  accented, enabled `Action` claims it, and a second claim on the same surface is
  recorded. The stills harness clears the audit before rendering and fails if any
  page claimed twice. The rule is now a gate rather than a paragraph.
- **`TripGround` is used.** The trip card is printed on the warmer stock, with the
  card itself as the raised sheet; the token had existed unused since the palette
  was written.
- **`InkMid` and `InkSoft` are different colours** (`#334155` / `#475569`): the
  quiet-action tone is a control and has to survive a glance.
- **Keyboard and system bars** are handled once at the root (`imePadding()`), and
  the tab bar's height follows the font scale instead of being fixed. Three
  stills render at 200% text.
- **Android has a photo picker.** `PickVisualMedia` — the system picker, no
  storage permission, bytes bounded to 1600px and re-encoded like the camera's.
  It is bound in `MainActivity` beside the camera, so "bring one in from the
  library" is a real control on the target most people use.
- **New stills.** `10-night`, `10-night-card`, `02-home-200`, `05-nightfall-200`,
  `02d-prefs-200`.

### 9.3 Still open

1. **Web and iOS have no picker.** Desktop and Android do. The browser build's
   picker needs typed DOM file-input interop, and iOS needs a `PHPickerViewController`
   delegate; neither can be compiled in this environment, and shipping platform
   interop that has never been through a compiler is a worse outcome than the
   honest gap. Android's implementation is the pattern to copy.
2. **Nothing in this pass was compiled.** The currency catalogue and its search
   were compiled and exercised with the real Kotlin compiler; every Compose file
   and every platform entry point was written against the source, not a build.
   `tools/run-preview.sh`, `tools/flow-check.sh` and `tools/entry-check.sh` are the
   gates to run first, and the Android picker's stubs in `tools/entrycheck/` were
   updated for it.
3. **Large-text behaviour is improved but unverified.** After the third review the
   date-pair stacks on a phone, the Nightfall decisions stack instead of sharing a
   row, the shutter badge is a pill that grows with its numeral, the day spine is a
   minimum width rather than a fixed one, `QuietAction` is 44dp by default and the
   timeline footer is a column. Fifteen stills at `fontScale = 2` now exist in the
   shot list, two of them in landscape. **None of them has been rendered** — this
   environment has no Compose runtime — so the audit is source-level.
4. **Keyboard/safe-area has not been seen on a device.** The manifest is now plain
   `adjustResize` (it used to force the keyboard up at launch, `stateAlwaysVisible`),
   the root pads the system bars, consumes them, and then adds the remaining IME
   height so there is no dead band above the keyboard. Whether that is enough on a
   6-inch phone is still a hardware question.
5. **Screenshots are archived, not current.** The pre-third-review renders are in
   `docs/shots/archive-2026-11-26-pre-third-review/` behind a README that lists
   what they predate. `docs/shots/` holds no current render, and no screenshot is
   cited as evidence anywhere in this document. Appendix A is the to-do list.

---

## 10. Brief for the reviewer

Please produce recommendations in this shape, so they can be triaged:

1. **Hierarchy** — is there exactly one obvious next action on each surface, and
   is the most important object (usually the photograph, sometimes the figure) the
   largest thing on it?
2. **Flow** — where would a first-time traveller get lost, double back, or fail to
   find the thing they need? Pay special attention to: first run, the two home
   surfaces, the hand-off from Frame to Nightfall, the end of a trip, and what
   happens when a day or a trip is closed.
3. **Consistency** — which components, plate numbers, states, or strings disagree
   with each other, and what is the single correct version?
4. **Phone ergonomics** — thumb reach on Nightfall and Detail; whether anything
   that finishes a card can be scrolled away; the size of the chips, keypad and
   charts on a real 6-inch screen.
5. **Copy** — where the voice slips (marketing, apology, praise, jargon), and
   whether each empty state tells the truth.
6. **What to remove** — this design is a subtraction exercise; call out anything
   that is decoration pretending to be a control.

Constraints for any proposal: keep one blue action per surface; no red; no
accounts, servers or sync; no mandatory fields; no streaks, targets, scores or
praise; no Material-default shapes (floating buttons, rounded filled buttons,
white cards on grey); the photograph stays the largest thing wherever it appears.

**Useful references in the repo:** `docs/shots/archive-2026-11-26-pre-third-review/`
(the only renders that exist, explicitly pre-review),
`docs/DESIGN.md` (visual rationale), `docs/PAGES-BRIEF.md` (intent-only page
brief), and the source in `ui/app/src/commonMain/kotlin/com/ariadne/app/ui/pages/`
and `core/design/src/commonMain/kotlin/com/ariadne/design/`.

---

## Appendix A — screenshot index

**No screenshot in this repository is currently offered as evidence.** Every PNG
that existed before the third review has been moved to
[`docs/shots/archive-2026-11-26-pre-third-review/`](shots/archive-2026-11-26-pre-third-review/)
with a README listing what it predates (the register bar, the closed-trip matrix,
the square plate, the 44dp targets, the single rate editor, the stacked New-trip
dates). `docs/shots/` holds no current render.

The harness in `preview/src/main/kotlin/com/ariadne/preview/Shots.kt` is the
source of truth for what *should* exist. It now lists **44 frames**, and
`./tools/run-preview.sh --shots docs/shots` renders them. Until that has been run
against this build, treat this appendix as a to-do list rather than an index.

| Group | Shots | Notes |
| --- | --- | --- |
| Front door | `01-onboarding`, `01-new-trip`, `01-new-trip-desktop`, `01-onboarding-200`, `01-new-trip-200` | Onboarding is reachable only on a first run; `02d`-style numbered pages are the trip's, the door is unnumbered. |
| Home | `02-home`, `02-home-scrolled`, `02-home-desktop`, `02-home-200` | The figure, the pill, the rail. |
| Register | `02a-trips`, `02a-trips-desktop`, `02a-trips-200`, `02a-trips-200-landscape` | Stacked from Home: bar visible, no destination selected. |
| Timeline | `02b-timeline`, `02b-timeline-scrolled`, `02b-timeline-200` | 1:1 hour rail, zone-correct hours, stacked collisions. |
| Stats | `02c-stats`, `02c-stats-budget`, `02c-stats-filled` | Empty, with a fence, populated. The “finished photographs only” qualifier sits on the total. |
| Preferences | `02d-prefs`, `02d-prefs-200` | One open rate editor at a time. |
| Closed trip | `02e-home-closed`, `04-capture-closed` | The two states the third review said had never been rendered. |
| Trip | `03-trip`, `03-trip-200` | |
| Capture | `04-capture`, `04-capture-200` | The ruled plate, the shutter in the notch. |
| Frame | `04b-frame` | |
| Nightfall | `05-nightfall`, `05-nightfall-desktop`, `05-nightfall-200` | Phone stack (decisions stacked, not side by side) + wide spread. |
| Record | `06-record`, `06-record-scrolled`, `06-record-200` | |
| Detail | `06b-detail`, `06b-detail-desktop`, `06b-detail-200`, `06b-detail-200-landscape` | Portrait, wide, 200%, and 200% on the shortest viewport. |
| Card | `07-trip-card`, `07-trip-card-200` | One blue Share. |
| Search | `08-search`, `08-search-200` | |
| Night paper | `10-night`, `10-night-card` | The dark palette on Home and on the kept card. |

Phone frames are 440×~900, desktop 1200×800, landscape 900×440. The render pass
**fails** if any surface claimed the blue twice (see §9.2).

---

## Appendix B — the one-paragraph version

> Ariadne is a photograph-first trip ledger. You photograph what you spend on
> while travelling and type nothing in the moment; in the evening you finish the
> day's photographs once, at about two taps each, and at the end of the trip you
> keep a card: every day, what it came to, and the photographs behind it. There is
> no account, no server and no sync — everything stays on the device. The UI is
> built to feel like a small printed book: a cool paper ground (or a night stock,
> chosen in Preferences), one blue action per page, a high-contrast serif for
> headlines, tracked capitals for every label,
> tabular figures for every amount, hairlines instead of cards, and plate numbers
> so the pages read as a sequence. Nothing is required; an unfinished photograph
> is a valid record, not an error; and the app never scolds, celebrates, or uses
> red. What it needs is a rigorous review of hierarchy, flow gaps, component
> consistency, phone ergonomics and copy — not a restyle into a dashboard.

---

## Appendix C — response to the 2026-11-26 review

The review was written against a different repository (`Naeha-S/breadcrumbs`), so
its “what to reuse / what not to reuse” advice describes a codebase this one does
not share. Several of its P1 items were already true here before the review:
Ariadne already has real serif/grotesque font families with tabular figures, a
shared component set (`Masthead`, `Rule`, `Action`, `OutlineAction`,
`QuietAction`, `PageAction`, `Field`, `FigureRow`, `Plate`, `DayBand`,
`Breakdown`, `ScrollRail`, the notched `TabBar`), one blue action per surface, no
cards-with-shadows, no toasts, no Material default shapes and no network code.

### The fifteen findings, one line each

| # | Finding | Now |
| --- | --- | --- |
| 1 | Onboarding unreachable on the launch path | Launch with an empty register opens `Onboarding`; Begin empty → New trip, Demo roll → a seeded trip. §4.1 |
| 2 | Two surfaces called Home | The Home tab opens `Home` (plate 01); the register is a stacked page with a `HOME` action. §3.1, §5.4 |
| 3 | Plate numbers collide / sequence has gaps | `01`–`09` are the trip's pages; the front door, register, shutter, frame and card carry none. §3.2 |
| 4 | Screenshots show the old bar | **Not fixed here** — no renderer in this environment. The list to render is in Appendix A; the stills pass now also fails on a two-blue page. |
| 5 | Preferences repeats numeral `I` | I · II · III · IV. §5.7 |
| 6 | Search opens the day, not the photograph | A result opens Detail; the day is one Back away. §5.13 |
| 7 | Home's library link routes to the shutter | It opens the picker directly, and only where a picker is bound. §5.3 |
| 8 | Night mode designed, never enabled | Light / System / Night, stored, resolved at the root. §9.2 |
| 9 | Closing a trip leaves it active | Close clears `activeTripId`, the shutter refuses a closed trip, the register stops marking it, and Home/Capture say so. §4.6 |
| 10 | “Long-press to reopen” in copy, no handler | The claim is gone; copy says a closed day's cards are edited from the record. §5.8, §5.9 |
| 11 | Currency choices asymmetric | One catalogue of 67 codes with search; New trip and Preferences use it; Detail's chips are its shortlist. §5.2, §5.7, §5.11 |
| 12 | `InkSoft == InkMid`; DESIGN.md stale | `InkMid #334155`, `InkSoft #475569`, and DESIGN.md's table now matches the code. §2.2 |
| 13 | `TripGround` defined, unused | The kept card is printed on it. §5.15 |
| 14 | Copy drift on New trip | The dangling sentence and the “only the photograph leaves the camera” line are both fixed. §5.2 |
| 15 | Stats' dead blue bar; Record's Nightfall enabled on a closed day | A disabled `Action` draws on sunk paper and claims no blue; Stats disables on a filed day too, and Record's Nightfall is inert on a closed day. §5.6, §5.9 |

| Review item | Status | Where |
| --- | --- | --- |
| P0-1 one meaning of Home | **Done** | Home tab → `Home`; register is a stacked page with a `HOME` action; §3.1, §3.3 |
| P0-2 first-run path | **Done** | Launch → Onboarding; register if every trip is closed; §4.1 |
| P0-3 closed-trip authority | **Done** | `closeTripIntoCard` clears the active trip; capture blocked; Home/Capture closed states; register label fixed; §9.1 |
| P0-4 Nightfall/Detail bottom-edge contract | **Mostly done** | Keypad shared with minimum row heights; chips get 44dp targets; IME and system bars are handled once at the root — not verified on a device |
| P0-5 explicit capture hand-off | **Kept** | Capture → Frame → Keep / Fill now unchanged; photos are written before extraction |
| P1-6 shared component vocabulary | **Done** | `RuledKeypad`, `MeasuredRule` and `CurrencyField` added; `AccentBudget` added for the blue rule; the rest already existed |
| P1-7 one blue action in the API | **Done (audited)** | Not a type — that needs a page DSL — but every surface has an `AccentBudget`, `Action(accent = true)` claims it, and the stills harness fails on a second claim. Record's Nightfall and Stats' Close today are disabled on a filed day |
| P1-8 no Material/card styling | **Already true** | no shadows, 2dp controls, 1dp chips, hairline panels |
| P1-9 real type tokens | **Already true** | vendored Instrument Serif + Inter Tight at four weights, `tnum` numerals |
| P1-10 palette/dark mode honesty | **Done** | Light / System / Night, stored with the settings, resolved at the root, and every token swaps |
| P1-11 plate numbering | **Done** | 01–09 are the trip's pages; front door, register and moments are unnumbered |
| P1-12 search destination | **Done** | Result → Detail; day is one Back away |
| P1-13 library semantics | **Done** | Opens the picker directly; hidden where no picker exists |
| P1-14 currency symmetry | **Done** | one catalogue of 67 codes with names and search on New trip and Preferences; Detail chips are its shortlist; arbitrary codes are only read from receipts |
| P1-15 long-press copy | **Done (removed claim)** | copy now says closed-day cards are editable from the record |
| P1-16 section numerals + copy | **Done** | Preferences I–IV; dangling demo sentence and footer promise fixed |
| P1-17 large text / accessibility | **Partly** | keypad grows, tab bar height follows the font scale, 200% stills are defined but unrendered; date strip and big numerals still fixed |
| P1-18 contrast for metadata | **Partly** | informative `inkGhost` captions moved to `inkFaint`; `InkMid` darkened to `#334155` so quiet controls are legible; `inkFaint` at 9sp still fails strict AA by design |
| P1-19 touch targets | **Partly** | category and currency chips have 44dp targets; quiet actions and date-strip cells unchanged |

### Third review (same day) — P0 and P1, in the order they were given

| Item | What was chosen, and where it lives |
| --- | --- |
| **P0-1** one startup rule | `startRoute(hasAnyTrip, hasOpenTrip)` in `Route.kt` is the only decision: no trip ever → `Onboarding`; an open trip → `Home`; every trip closed → the register. A cold launch calls it, `StartRouteTest` states it three times, and §7's stale "No trip → New trip" row was replaced by the full table. Opening a trip from the register afterwards is a separate, explicit act. |
| **P0-2** register bar contract | **Bar visible, nothing selected.** `Route.tab()` returns `Tab?` and returns `null` for `Trips` and `Preferences`; the page keeps its top-right `HOME` action; `TabBar(barDescription = "Trips register, stacked from Home")` announces where the bar is. Screen state, log the same rule, and deep links all land on a shell that behaves this way. |
| **P0-3** closed-trip matrix | One guard per door rather than a per-page decision: `capturePhoto` and `openCapture` refuse a closed trip; `saveDetail` and `closeDay` return early; the bar's shutter loses its colour, its tap and its role; `DetailPage` renders read-only with no keypad, no Save and no extracted Yes/No; Home, Capture, Record, Nightfall and Preferences each state the closed trip in one sentence and offer the card. The register row is a single semantic node that says "closed". |
| **P1-4** 200% text | Type reflows instead of shrinking: New-trip dates stack on a phone, Nightfall's decisions stack, the shutter badge is a pill, the day spine is `widthIn(min = 64.dp)`, `QuietAction` is 44dp by default, the timeline footer is a column. 15 stills at `fontScale = 2` are defined (two in landscape); **not yet rendered**. |
| **P1-5** IME + safe area | The manifest no longer forces the keyboard up at launch (`stateAlwaysVisible` removed, `adjustResize` kept). The root pads the system bars, consumes them, then adds the IME — so the bottom action is above both the keyboard and the inset, without a nav-bar-tall dead band. **Decision: on Detail the ruled keypad is the only amount input** — the amount has no `BasicTextField`, so the IME never competes with it; the IME serves the merchant, note and category fields only. |
| **P1-6** photo picker | One `PhotoPicker` interface behind `LocalPhotoPicker`, provided by Android (`PickVisualMedia`) and the desktop (`JFileChooser`), **absent** on iOS and Wasm — the control is not drawn where it cannot work, and the caller's fallback opens the shutter instead. Imported bytes go through the same `capturePhoto` → `Route.Frame` path as a camera capture: there is no second flow and no second save. |
| **P1-7** currency | **Catalogue-first, and it says so.** One `CurrencyField` in all three places, 67 codes, with a footer naming the scope: "the 67 codes the arithmetic reads … a receipt in any other code is still read and shown as it is; it simply cannot be a trip's own currency." Trip, base and home codes are stated wherever a figure is converted. |
| **P1-8** `InkSoft`/`InkMid` | **Documented, not merged.** `InkMid #334155` is the weight a small *control* uses; `InkSoft #475569` is running text that is not the point; `InkFaint` is metadata. The ladder is now in `docs/DESIGN.md` as well as in `Bone.kt`. |
| **P1-9** `TripGround` | **Kept, card-only.** It is used by exactly one surface — the kept card — and `docs/DESIGN.md` says so; a second use would defeat the point of a warmer stock. |
| **P1-10** route names | `Route.Preferences` is canonical, `PreferencesPage` is the page, the visible corner action still prints `PREFS` (what a printed index would say). `Route.Settings` survives only as a retired alias mapped at the shell, and every screenshot still showing a `PREFS` *tab* is archived. |
| Per-surface notes | Onboarding's "Begin empty" is a 44dp `QuietAction`; Nightfall's decisions are stacked and distinguishable at 200%; the register row is a semantic target; closed rows use `inkMid`, not ghost; the Stats qualifier sits on the total; Preferences has one open editor; the plate is 1:1 everywhere; the Trip card has one blue action (Share); Detail's Back returns to Search when that is where it was opened from. |
