# Ariadne — design language

> For the current build's page-by-page inventory and flows, see
> [`UI-AND-FLOW.md`](UI-AND-FLOW.md). This file is the rationale; that one is the
> specification as built.

**Ink.** A cool paper ground, one blue, a high-contrast serif for headlines, and
small tracked-out uppercase for everything structural. The colour is the
reference's; the bones are the second reference's: big italic serif, ink-filled
controls, and a bottom bar with the shutter seated in a notch cut out of it.

Nothing on screen is decorative. If an element is not carrying a name, a figure,
or a decision, it is removed.

---

## 1. Colour

One chromatic value. Blue means *"this is the thing to do"* and appears **once per
surface** — one button, one selected state, one figure you acted on. Two blues on
a page means one of them is wrong.

| Token | Light | Night | Use |
| --- | --- | --- | --- |
| `Paper` | `#F8FAFC` | `#111318` | Page. Cool off-white, never cream. |
| `PaperRaised` | `#FFFFFF` | `#181B21` | Panels, fields, the tab bar. |
| `PaperSunk` | `#F1F4F7` | `#0C0E12` | Pills, plate ground. |
| `Edge` | `#E2E8F0` | `#272B33` | Hairlines inside content. |
| `EdgeStrong` | `#CBD5E1` | `#3A404A` | Frame edges, the bar's top line. |
| `Ink` | `#1F1F20` | `#F2F5F9` | Headlines, figures, the shutter. |
| `InkMid` | `#334155` | `#B6C0CC` | Quiet action labels, secondary figures. A control, so a step stronger. |
| `InkSoft` | `#475569` | `#8794A3` | Running text that is not the point. |
| `InkFaint` | `#64748B` | `#616C7A` | Footnotes, inactive tabs, metadata. |
| `InkGhost` | `#CBD5E1` | `#3A404A` | Ticks, plate line work. |
| `Blue` | `#2563EB` | `#7DA2F7` | The one action. |
| `BlueWash` | `#EFF4FE` | `#1A2436` | A count pill, a selected row. |
| `TripGround` | `#F5F2E7` | `#14181E` | The kept card's warmer stock. |

**No red.** It is not in the palette, so it cannot appear in the product: errors
are ink, and a failure says what happened in words.

**Both stocks are shipped.** The palette above has always had a night half; since
2026-11-26 it is chosen in Preferences (`Light` / `System` / `Night`), stored with
the settings, and resolved once at the root. `System` asks the platform. Nothing
else in the product knows which stock it is printed on — pages read `Paper` and
the tokens resolve.

**One blue, audited.** A page cannot ship two blue actions quietly: the shell hands
every surface an `AccentBudget`, `Action(accent = true)` claims it, and the stills
harness fails if any surface claimed twice. A disabled action draws on `PaperSunk`
and claims nothing. Two `Action(accent = true)` calls in the alternative branches of
one `if` are not a violation — only one of them composes (this is why `TripPage`'s
"Close the trip into a card" / "See the card" pair is not on the violation list).

### The ink ladder, and why there are five rungs

They are not improvised greys; each rung is a role, and a component that picks the
wrong one is the most common way a page looks almost right.

| Rung | Role | Never |
| --- | --- | --- |
| `Ink` | Headlines, figures, the trip name | — |
| `InkMid` | **A small control's label** and secondary figures. One step darker than `InkSoft` on purpose: a control has to survive a glance, and the two were once the same value, which made the palette lie. | body copy |
| `InkSoft` | Running text that is not the point: notes under a title, a state sentence under a rate row | a control's label |
| `InkFaint` | Metadata a person reads only when looking for it: 9sp tracked caps, inactive tabs, footnotes | any sentence that carries information |
| `InkGhost` | Line work only: ticks, plate rules, a hairline that happens to be ink | any text at all |

`InkSoft` and `InkMid` were **kept as two rungs** rather than merged (third review,
P1-8). They are 2 steps apart in value and 1 step apart in role, and the codebase
uses both deliberately — merging them would either make quiet controls faint or make
running prose shout. The decision is recorded here so the next reviewer does not
have to re-derive it.

**`TripGround` has exactly one user**: the kept card (§5.15 of `UI-AND-FLOW.md`).
A warmer stock is only warmer if it is rare; a second page on it would make it a
theme, and the app already has two of those.

## 2. Type

### Weights, and the bug that hid them

`Sans` was bound to a single 400-weight font file, so every `fontWeight =
Medium` or `SemiBold` in the app silently resolved back to 400: the entire
product rendered in one weight. That is the single most recognisable tell of
generated UI, and it was invisible in review because the type looked "clean".
Inter Tight is now bound at four static instances — Light 300, Regular 400,
Medium 500, SemiBold 600 — instanced from the variable font with fontTools and
committed under `core/design/src/commonMain/resources/fonts/`.

### Optical tracking

Type set at 44px and type set at 10px cannot share one letter-spacing value.
`Optical` in `Bone.kt` holds the corrections and the components apply them by
size:

| Size | Tracking | Why |
| --- | --- | --- |
| display 44 (desktop 56) | **-0.9sp** | Big serif set at default spacing looks gappy and amateur. |
| title 30 | -0.5sp | |
| heading 19 | -0.15sp | |
| body 15 | +0.05sp | Small type wants a hair of air. |
| numerals ≥34 | **-0.8sp** | A long amount should read as one object, not as digits. |
| labels 9–10 | +1.6 to +2.2sp | Tracked-out capitals, as a printed index uses. |

### Shapes: square, not rounded

Radii were 10dp on controls and 16dp on cards, which is the default shape
vocabulary of every Material-derived layout. The book now uses **2dp on
controls and 1dp on tags**: enough to survive a renderer without
antialiasing, too little to read as a card. Capsule pills became
rule-bordered rectangles; the keypad became a grid of hairlines with no fills.

### The grid

Every page used to be one centred 620dp column at 24dp margins with everything
stacked in it — a single axis, uniform rhythm, no left edge to hang anything
from. That shape reads as a form whatever type you put in it.

`Page` in `:core:design` gives a two-axis grid instead: a **132dp hanging rail**
carrying dates, figures and marks; a **28dp gutter**; then the measure-bounded
text column; the whole spread hung from the **left** of the page, capped at
980dp, as a printed book does. On a phone the rail folds to zero and the same
content is one column again.

Every page opens with a `Masthead`: a **plate number** (01…09), a hairline, an
eyebrow, then the headline at 44px (56px wide), then optionally a serif lede,
then a rule. The number is what makes the app a sequence of plates rather than a
set of screens. Sections inside a page are numbered in roman (`SectionHead`).

## 3. Legacy layout notes: the grid, not the column

| Role | Family | Size | Notes |
| --- | --- | --- | --- |
| Display | Instrument Serif | 40 | Page headlines. Italic where a page wants to whisper. |
| Title | Instrument Serif | 30 / 24 / 19 / 17 | Panels, rows, names. |
| Body | Inter Tight | 15 | Line height 1.55. |
| Numeral | Inter Tight, `tnum` | 44 / 40 / 17 / 15 | **Tabular**, so columns never shift. |
| Label | Inter Tight Medium | 10, tracking 2.2 | Uppercase. Every field, tab and eyebrow. |

Both faces are SIL OFL and vendored in `core/design/src/commonMain/resources/fonts`
with their licences beside them.

The rule that matters: **a number is never a float and never shifts.** Every
amount is `Long` minor units rendered through `Numeral`, and `tnum` keeps 1,200.00
and 25.00 in the same column.

## 4. Legacy layout notes

- Page margin **24dp** (56dp on wide), vertical **24dp**.
- Text measure caps at **620dp**, centred. Desktop never stretches to the window.
- Field **52dp**, button **56dp**, radius **10dp** (panels 16dp).
- Rules are 1px hairlines. Ticks are 2px × 28dp.
- Depth comes from a hairline and a white panel, not from a shadow.

## 5. The shell

Four destinations — **Home · Timeline · Trip · Stats** — plus the shutter.

The tab bar is part of the shell, not of any page, so it cannot drift between
screens. The shutter sits in a **notch cut out of the bar**: a filled path with a
pair of cubic curves dipping around it, traced with the same hairline as the bar's
top edge. The dip is drawn rather than decorated, which is why it reads as the bar
bending around the button.

The bar is **hidden** on the four surfaces that are moments rather than places:
first run, the shutter, the ritual, and the card. A bar under the ritual would
make it feel like an errand.

## 6. Pages

| # | Route | Tab | What it is |
| --- | --- | --- | --- |
| 1 | `Onboarding` | — | One page, four sentences, two actions. States the inversion. |
| 2 | `Trips` | Home | The register. Every trip this device holds, newest first; the one active is marked by blue rather than by a badge. |
| 3 | `Home` | Home | Today's figure, the one blue action, and the way into everything else. |
| 4 | `Timeline` | Timeline | The trip as a run of days, each with its 24-hour band. |
| 5 | `Stats` | Stats | What it came to: by what it was, then by currency. Finished photographs only. |
| 6 | `Trip` | Trip | Dates, progress, the card. The tab the register used to leave unreachable. |
| 7 | `Capture` | — | The shutter. Full bleed, no chrome, no fields. |
| 8 | `CloseDay` | — | Close Day. The ritual. Plate, merchant, amount, two taps. |
| 9 | `Record` | Timeline | A day, as it happened. No table, ever. |
| 10 | `TripCard` | — | The thing you keep. |
| 11 | `Detail` | Timeline | One photograph in the hand: amount, currency, category, a line to keep it by. |
| 12 | `Search` | — | Remember a thing by shop, price, currency or day. |
| 13 | `Settings` | — | Currency, paper, the evening reminder, and plainly what leaves the device. Reachable from Home; it is a corner, not a destination. |

`Settings` survives in the sealed route type so an older persisted route still
resolves, but it renders the register behind it. `Trips` is the register the
register is listed as above.

### Two pieces that read the data rather than collect it

**The day band** plotted each photograph at the hour it was taken — recorded ones
filled, unfinished ones outlined. It said *when* the day happened and stopped
there: no streaks, no targets, no missing-hour guilt. **Retired in phase 8**: the
timeline was rebuilt to the journey reference, and a day there is marked by its
state (`SETTLED`, `IN THE RECORD`, `2 TO SETTLE`) rather than by its hours. A day
with eight photographs has a shape whether or not the app draws it; the state is
the fact the traveller cannot get from the photographs themselves.

**The breakdown** (Stats) is a ruled column, not a donut, because a ledger should
be readable to the rupee rather than only glanceable. It is always in the trip's
base currency, converted through the same `FxTable` the ritual uses, so the
breakdown and the total can never disagree.

The one sentence underneath it (`PatternLine`) states a fact about this trip's own
numbers — "most of this trip so far went on shopping, 75% of it". It never
compares the traveller to other people, to a previous month, or to a budget they
never set.

### What the reference screens added, and what they did not

The uploaded references were treated as references, not a port. Each feature in
them was kept only if it is either a competition "wow" or a plain need in the
basic flow.

| Feature in the references | Decision | Why |
| --- | --- | --- |
| Capture detail / edit page | **kept** — basic need | A night of triage cannot be the only editing surface. Now `Route.Detail`. |
| Category chips | **kept** — basic need | Makes the breakdown possible; never required, never guessed by OCR. |
| Currency picker on the detail page | **kept** — basic need | Correcting a misread currency is the most common fix. |
| 24-hour day band on the timeline | **removed in phase 8** | Novel, but the rebuilt timeline carries the day's *state* instead; two readings of the same row competed for the eye. |
| Category breakdown + a pattern line | **kept** — wow, restyled | The reference's "5660% more than usual" is a machine grading a stranger; ours states a fact about the trip. |
| SMS parsing ("speak or send SMS") | **left out** | Needs platform permissions for a flow the ritual already covers. |
| Voice-to-expense | **left out** | Same: a second input path to maintain, no new idea. |
| Group-trip bill splitting | **left out** | A different product; the brief is one traveller's ledger. |
| Receipt auto-OAuth / cloud accounts | **left out** | Ariadne has no accounts and no server, by rule. |
| "Re-extract from photo" as a primary button | **left out** | Extraction is best-effort and already automatic; a button would imply a promise the app cannot keep. |
| Trip day counters ("Day 66 of 14") | **left out** | Contradictory arithmetic in the reference; dates are already on every surface. |

## 7. The ritual (Close Day)

The most important layout in the app, and the one with the tightest constraint:
**two taps finish a card.** So on a phone the plate, merchant and amount are
pinned, the keypad sits below them, and both decisions — `Confirm` (blue) and
`No amount` (outlined) — sit between the two. Nothing that finishes a card is
ever below the fold.

The plate is the largest element on the surface, always. Where a real photograph
is not bound it shows a single engraved line study of the subject — one weight of
line, one subject, generous margin. It never pretends to be a photo.

## 8. Motion

One duration, one curve, one property: **180ms**, ease-out, cross-fade between
routes only. No bounce, no slide, no scale. Reduced motion sets the duration to 0
and is honoured from Android's animator scale and the browser's
`prefers-reduced-motion`.

`LocalMotionPreference` carries the platform's answer down; pages never sniff for
it themselves.

## 9. Rules worth enforcing

1. No red, anywhere, for any reason.
2. Money is `Long` minor units + a currency code. `HALF_UP` only at the FX boundary.
3. Blue appears once per surface.
4. Every screen works offline. Every figure is legible with no network.
5. A card you cannot finish without scrolling is a bug.
6. No `!!`, no `GlobalScope`, no mutable global state.
7. **One plate geometry.** 1:1, hairline, square corners, wherever a single
   photograph is shown: viewfinder, frame, record thumbnail, triage card, Detail,
   the card. Four different ratios for one object is four different objects.
8. **44dp is the floor for anything a thumb has to find**, including the quiet
   actions and the shutter's count badge. Quiet is a weight, not a target size.
9. **A page that is stacked on a tab marks no tab.** The bar stays (it is how you
   leave) and `selected` is null; the bar announces where it is stacked from.
10. **Grow, never shrink.** At 200% text things stack, wrap and reflow; nothing
    drops below the specified rung in §2, and no action may be pushed off-screen.
11. **A day that has not happened is not a record.** Nothing is filed into a future
    day: not a photograph, not a spoken amount, not a typed one, not a Close Day. The
    list of days stops at today because tomorrow has nothing in it yet, and a refusal
    is a sentence in the margin, never a dialog.
