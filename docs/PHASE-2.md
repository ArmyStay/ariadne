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

## Not yet in this commit

Still to come in Phase 2 (next commits):

- **The capture sheet**: shutter → sheet over the captured photo with keypad, currency
  chips, category chips, note, Save / "Add amount later", replacing both `CapturePage`
  and `FramePage`. OCR suggestions as chips, never auto-committed.
- **Multi-select library import** into the "needs an amount" tray.
- **Trip**: budget editing, per-currency subtotals, trip switcher.
- **Stats**: category breakdown as a bar, daily spend against the daily budget, top
  entries.
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
