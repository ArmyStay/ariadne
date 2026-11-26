# Phase 2 — the new flow

Phase 2 is larger than Phases 0 and 1 together, so it lands in three commits. This
document tracks all of it; the section headings say which commit a piece is in.

The same environment caveat as before, and it matters more here: **Compose cannot be
compiled or rendered in this sandbox.** What could be checked is checked by machine —
`:core:model` (36 tests) and `:core:fx` (38 tests) run green, and the UI sources are
run through the Kotlin compiler as a syntax and resolution pass, which catches
syntax errors, conflicting declarations and non-exhaustive `when`s but nothing about
layout. `tools/verify-ui.sh` on a machine with Gradle is the real gate for this phase,
and `tools/run-preview.sh --shots` is the only honest source of screenshots.

## Commit 1 — the frame: a stack, four places, and the right words

### A real back stack (`AppModel.history`, `AppModel.go`, `AppModel.back`)

Decision D2 from Phase 0 was "keep the sealed `Route`, add a real back stack" rather
than adopt Navigation Compose, and this is that stack.

- `AppUiState.history` holds up to 24 previous routes. `go()` pushes; `back()` pops
  and returns false when there is nothing left, which is the signal the platform
  needs to close the app instead of swallowing the gesture.
- Moving *between* the four places starts a fresh stack (one step back to where you
  came from); moving *into* a page pushes the place it came from. One rule, in
  `go()`, instead of every page deciding for itself.
- `detailOrigin` is no longer how Back finds its way out of the detail page:
  `closeDetail` calls `back()` first and falls back to the old guess only if the
  stack is empty.
- Arriving on Today re-reads the calendar's today. Before, "Today" could still be
  showing the 14th because that was the last day looked at.

### Predictive back

`AriadneApp` takes an optional composable `onBack` parameter. `:app:android` supplies
`BackHandler(enabled = canGoBack)` and the manifest sets
`android:enableOnBackInvokedCallback="true"`, so the system's predictive-back
animation has the app's own callback to animate from rather than the launcher. This is
a composition parameter and not `expect/actual` because only Android has the gesture,
`BackHandler` is a composable from `androidx.activity`, and `:ui:app` has no Android
source set to put an actual in. Same seam, one fewer target to maintain.

### The bar: `NavigationBar` and a docked shutter

The curved-notch bar is gone (`TabBar.kt` deleted). It was the most distinctive thing
on the screen and the least useful: the notch cut the bar's own labels in half at
440dp, the disc overlapped the middle two destinations, and at 200% text the curve
clipped the words under the icons.

`Nav.kt` is a Material 3 `NavigationBar` with four destinations — **Today, Days,
Trip, Stats** — and the shutter as a `FloatingActionButton` docked in an empty middle
slot. The slot is a real item, so the shutter sits in space the bar reserved for it
and no label is ever underneath. The FAB keeps its own role and click action for
TalkBack; only its description changes ("Take a photograph. 3 waiting for an amount").

### The words

- `Route.Timeline` → `Route.Days`; `Route.Nightfall` → `Route.CloseDay`;
  `openNightfall` → `openCloseDay`; `NightfallPage.kt` → `CloseDayPage.kt`.
- Every user-facing "Nightfall" is now "Close day" or "add the amount later".
- The plate numbers ("01 —", "05 —") are gone from Today, Days and Close day. The
  remaining pages keep theirs until their own commit.

## Commit 1 — Today (replaces Home)

`TodayPage.kt`. The old Home was a contents page: five rows of links (Trips,
Timeline, The record, Search, The trip's page) around one figure. Every one of those
destinations is now either in the bar or a tap from the page it belongs to.

Today answers, in order:

1. **What have I spent today** — the figure, hung on the paper with no card, with `≈`
   and the conversion note when any of it was converted.
2. **How am I doing** — today against the trip's budget spread over its days, in the
   same voice either way ("₹1,200 left", "₹340 over").
3. **What is waiting** — "Needs an amount (3)" with the first three photographs as
   thumbnails and one action that finishes them all in the existing pager.
4. **The day itself** — today's entries as photo cards: a recognisable thumbnail, the
   name from `titleOf`, the entry state, the figure.

The shutter is still the page's one blue action, and the library is still a real
second way in rather than a label.

## Commit 1 — Days (replaces Timeline)

`DaysPage.kt`. A strip of every date in the trip across the top, the selected day
underneath with its total, its state, its two actions (open the day / close the day)
and its photographs, then the whole trip as a ruled list of days.

The 24-hour tick chart is deleted. It was the most decorative thing in the app and it
answered a question nobody asks ("which hour did I spend in?"), at a cost of one
bar per hour per day.

## Commit 1 — Close day (replaces Nightfall)

Same pager, new name and a real ending: after the last card the page is a **day
summary** — what the day came to, how many photographs, how many confirmed, how many
kept without an amount, how many still open — and then the one action, "Close the
day". The plate number is gone and the header is the day and its figure.

## Commit 2 — the capture sheet, and one tap saved

The shutter used to open a page that was not an editor: a held-up photograph with
two buttons, one of which ("Fill it in now") opened the editor. So the amount — the
only thing anybody came to type — was two taps and a page transition away from the
shutter, and the photograph got smaller at every step.

Now the shutter opens the sheet. `FramePage` is rewritten as that sheet:

- The photograph fills the page behind a `Paper.scrim` veil; the sheet rises over the
  bottom two thirds on paper, with a hairline top edge rather than a shadow.
- The amount, keypad, currency chips, category chips, where and remember-it-by are
  **on the sheet**. Save is one tap. "Add the amount later" is the sheet's own button,
  not a page you have to find, and it is the primary action when nothing has been
  typed (labelled "Add the amount later", because a Save that saves nothing is a lie).
- `capturePhoto` now loads the editor's state as it opens the sheet, so the field is
  live on the first tap. Extraction still arrives later and is offered as a
  suggestion — "Found ₹450 on it. yes / no" — and `acceptDetailSuggestion()` is the
  one call that takes it, never a pre-filled field.
- Wide windows get a spread instead: print on the left, the same fields on the right.

**Why the sheet is hand-built.** A Material `ModalBottomSheet` would bring its own
surface, elevation, shape and drag handle into a design that uses none of those
things, and its behaviour (partial expansion, nested scroll arbitration) is exactly
the kind of thing that cannot be checked in this environment. The sheet here is a
`Column` with a paper ground, a hairline edge and its own scroll — the same parts as
every other page — and the bar that looks like a handle is drawn, not draggable,
because a handle that promises dragging without dragging is worse than a rule.

`DetailPage`'s `LineField`, `CategoryChips` and `CurrencyChip` became `internal` so
the sheet uses the same controls instead of copies. `Paper` gained `scrim`.

### Flow checks added

`preview/FlowCheck.kt` now asserts the two things this commit changed, and the stack
contract from commit 1:

- the shutter lands on the sheet with the amount field live and empty;
- "add the amount later" returns to Today and deletes nothing;
- a day opens over the tab that asked for it, Back returns to that tab, Back again
  returns to the tab before it, and a third Back is refused without moving the app.

These compile and run only with Gradle against the real modules; that has not been
possible here (see the note at the top).

## Commit 3 — Trip and Stats, and several photographs at once

### Trip

`TripPage.kt` is rewritten on the tab shell. The old page was a contents list for
the trip with a "Back" action in its header — a leftover from when the trip was
reached from Home. It has its own place in the bar now, so the page is about the
journey instead: what it is (name, place, dates), what it has cost (spent against
budget, with the share drawn and the reminder labelled), how far through it you are
(days closed, as a measured rule), and what it was spent in (per-currency subtotals
and the one figure they add up to).

The trip switcher appears only when there is another trip on the device: a list of
one thing you already know is not a switcher. Budget and rates are one quiet action
away rather than being fields on this page — `/preferences` is still the place where
numbers are edited, and that is the next page to be rewritten.

### Stats

`StatsPage.kt` moves out of `TabPages.kt` and stops being a table. It now answers
three questions in order — what it came to (with the daily share of the budget named
explicitly, because "₹3,400 a day" is the number a traveller can use), what it was
(categories drawn to scale rather than only tabulated), and day by day (one bar per
day across the whole trip with the daily budget as a single line, so the *shape* of
the trip reads at a glance). Days with nothing on them are drawn as empty slots: a
gap in a trip is information.

Two things were removed rather than restyled: the numbered section heads ("II — what
it was"), and the "Close today" button at the bottom of a page of figures, which was
an unrelated action in the wrong place.

### Several photographs at once

`PhotoPicker` gains `pickMany(limit)`, with a default implementation that falls back
to `pick()`, so every existing target keeps working and the caller never has to ask
which kind of picker it has. `AppModel.importPhotos` files a batch without opening a
sheet — the tray the import just filled is the answer, not a sheet for the last
photograph in the pile — while a single imported photograph still opens its sheet.
Today's library action now asks for as many as the picker will give.

Android now overrides `pickMany` with `PickMultipleVisualMedia(12)` and a second
launcher, so a batch is one trip to the system picker. Both launchers share the
"one dialog at a time" guard, and cancelling arrives as an empty list rather than as
a special case. Desktop and the web keep the one-at-a-time fallback, which is what
their current pickers offer.

### Also

- `Modifier.rowTap` in `TabShell.kt`: one tap target per row with a real click action
  and role for TalkBack, no ripple. The rows in Days and Trip were `pointerInput`
  only, which is invisible to a screen reader.
- New stills in the manifest: `04-sheet`, `04-sheet-desktop`, `03-trip-desktop`,
  `02c-stats-desktop`.

## Not yet in this commit

Still to come in Phase 2 (next commits):

- **Android multi-select** in the picker (`PickMultipleVisualMedia`).
- **Preferences**, the last page still built as a long numbered form.
- **Snackbar undo** for delete and edit, and loading/error states for every list.
- **Preview definitions** (`@Preview`/`@PreviewParameter`) for each redesigned screen,
  which is what "screenshots" has to mean in an environment that cannot render Compose.

## What to review in commit 1

1. **The back rule.** Between places: one step back. Into a page: push. Is that the
   behaviour you want from Days → day → detail → back → back?
2. **The docked shutter.** It now sits inside the bar's middle slot rather than in a
   notch. It is 56dp, standard Material, and it does not overlap a label — but it is
   also much less distinctive than the notch was. The brief asked for exactly this
   trade; worth confirming it is the trade you wanted.
3. **Today's four blocks.** Spend, pace, tray, photographs. If the tray belongs
   higher (before the pace), that is a one-line move.
