# Phase 1 — the bugs, fixed at the source

Phase 0 (`docs/REDESIGN-AUDIT.md`, commit `5db279b`) found the six bugs and named the
line each one lived on. This phase fixes them. It is deliberately *not* a redesign:
the pages keep their current shape, so that when the flow changes in Phase 2 it is
obvious which change caused what.

## What was verified, and how

This sandbox cannot run Gradle (no Maven Central) and cannot compile Compose, so the
honest split is:

| Claim | How it was checked |
|---|---|
| `:core:model` — entry state machine, merchant/title rule | compiled with the Kotlin 2.4.21 CLI and **run**: 36 tests, 0 failures |
| `:core:fx` — conversion, totals, category breakdown, rate parsing | compiled and **run**: 38 tests, 0 failures |
| `:core:extract` — merchant gate | changed, **not run** (module needs coroutines jars not present here) |
| `:core:store` — migration 4, rate snapshot | changed, **not run** (needs the SQLDelight jars in `~/.cache/tools`) |
| `:ui:app`, `core:design`, `app:android` | edited, **not compiled** — Compose is not compilable in this environment |

Run `tools/test-domain.sh`, `tools/store-check.sh` and `tools/verify-ui.sh` before
merging. The first two are the ones with real assertions behind them; the third is
what will tell you whether the UI edits compile.

## Bug 1 — currency: "THB 2,555 + INR 50 = ₹2,605"

Four separate faults, all fixed:

1. **`Tally.total` added unconverted money into the base total.** It counted the
   misses (`unconvertedCount`) and then added the amount anyway. Now unconverted
   money is held in `TallyResult.unconverted`, per currency, and *never* enters the
   base figure. `TallyResult.isComplete` says whether the figure accounts for all of
   it.
2. **The same capture converted at three different dates.** The day page used the
   day in view, the timeline used the trip's first capture, the category totals used
   the trip's start date. `Tally.total` no longer takes a date at all: each capture
   converts at the rate pinned to its row, or at the table for its own day.
3. **No rate was ever fetched.** `Frankfurter` (URLs + a hand-written parser for the
   two fields that matter) and `RatePolicy` (fresh for two days, `shouldFetch`) live
   in `:core:fx`; `AndroidRateSource` is the only code in the app that touches the
   network, and `PlatformServices.rateSource()` is how it arrives. `INTERNET` is the
   second permission the app asks for, and the request carries a currency code and
   nothing else. A failure changes nothing: the cached rate keeps working.
4. **The rate a figure was confirmed at was not stored.** Migration `4.sqm` adds
   `rateScaled` and `rateDate` to `capture` (both nullable, nothing rewritten).
   `AppModel.pinRate` writes them from every door that confirms an amount — the
   stack, "confirm all ready", the detail page. A later refresh cannot move a
   confirmed figure.

On screen: the day and trip figures carry `≈` when any part of them was converted,
`ConversionNote` prints the rate's date underneath, and money that could not be
converted is named in its own currency ("฿2,555.00 not counted — no THB rate yet")
instead of being added to a rupee column.

## Bug 2 — contradictory state: "ALL SETTLED" over an open card

`core/model/EntryStatus.kt` defines one ladder —
`Captured → HasAmount → Confirmed → InRecord`, with `NoAmount` and `Failed` hanging
off it — and every label now derives from it:

- Home's pill: the day's pending count (`DaySummary.of`), not `stack.isEmpty()`; the
  "waiting" rail figure and the tab badge use the trip-wide count.
- Timeline day rows say "settled", "1 to settle", "3 to settle" or "in the record",
  counted the same way.
- The record's banner and each row's metadata line use the same vocabulary
  ("Needs an amount", "Not confirmed", "No amount").

Two screens can no longer disagree, because there is nothing left for them to
disagree with.

## Bug 3 — garbage titles: "ction View Go Run Terminal Help"

`core/model/MerchantName.kt` holds the rule: a line with two or more words from the
vocabulary of *screens* (menus, window controls, status bars) is not a merchant, a
line of nothing but receipt words is not a name, and more digits than letters is a
receipt number. `ReceiptParser.detectMerchant` applies it per candidate and keeps
scanning instead of giving up on the first line, and every title in the UI now comes
from `AppModel.titleOf(capture)` →
`MerchantName.receiptTitle(...)`, which falls back to **"Receipt · 21:09"**. The word
"Untitled" is gone from every page. This also fixes the same string being used as the
photo's content description — a screen reader no longer reads out a menu bar.

## Bug 4 — Stats: "Not said" twice

The breakdown was `GROUP BY category, currency`, so one category produced one row per
currency, and each was converted at the trip's start date while the day pages used
the capture's own day. `:core:fx/CategoryTotals` now aggregates in Kotlin over the
same finished captures the total is computed from, with the same rules: one row per
category, unconvertible money excluded rather than added, largest first. `AriadneStore.categoryTotals`
delegates to it, so there is exactly one aggregation rule in the codebase. The label
is "Uncategorised".

## Bug 5 — the amount screen showed no photo

On a phone the triage card put the photograph inside a scrolling column above a
pinned keypad, so typing an amount scrolled the receipt away. The plate now takes the
space the pinned parts below it do not need: it shrinks at large font sizes instead
of leaving the screen.

## Bug 6 — money truncated and collided

- `Numeral` no longer ellipsises (`overflow = Visible`, `softWrap = false`): an
  amount is either drawn whole or drawn wrong where someone can see it.
- Money is no longer put through `Label`, which truncates.
- The budget rows on Trip and Home stats are two columns with a real 12dp gap
  instead of five siblings and a weighted spacer that collapsed to nothing — which
  is how "₹10,000.00" and "LEFT" came out touching.
- The viewfinder hint "TAP ANYWHERE TO KEEP" sat on top of the live preview, and was
  wrong as well as in the way: only the frame is tappable. It now reads "Tap the
  frame to keep it", in sentence case, under the frame.

## Files

New: `core/model/{EntryStatus,MerchantName}.kt`, `core/fx/{Rates,CategoryTotals}.kt`,
`core/store/.../db/4.sqm`, `app/android/.../AndroidRateSource.kt`,
`core/model/commonTest/EntryStatusTest.kt`, `MerchantNameTest.kt`,
`core/fx/commonTest/FxHonestyTest.kt`.

Changed: `Fx.kt` (Tally rewritten, `convertAt`), `Records.kt` (`Capture.rateScaled/rateDate`),
`Clock.kt` (`minuteOf`), `Outbox.kt` (`recordRate` with a default body),
`AriadneStore.kt`, `Ariadne.sq` (`recordCaptureRate`), `ReceiptParser.kt`,
`Components.kt` (`Numeral`), `AppGraph.kt` (`rateSource`), `AppModel.kt`
(`refreshRates`, `pinRate`, `titleOf`, `clockTimeOf`, category aggregation, state
counts), `Route.kt` (`tripPending`, `daySummary`, unconverted fields), and the pages
that print any of it.

## Data safety

Migration 4 is additive and nullable. No row is rewritten, nothing is dropped, and a
capture with no snapshot keeps converting at the rate table for its own day — which is
what it did before, only at the right date now.

## What to review

1. **`Tally`'s new contract.** Do you agree that a total must exclude money it cannot
   convert, and say so, rather than show a number that is not true? (If you would
   rather see a floor marked "at least…", that is a copy change, not a code change.)
2. **`RatePolicy.MAX_AGE_DAYS = 2`.** Fresh enough not to ask on every launch, stale
   enough to notice a move.
3. **`AndroidRateSource` and the `INTERNET` permission** — the one thing this phase
   adds to the app's surface outside the record.
4. **`StoreSmoke`'s EUR assertion changed.** It used to require that an unconvertible
   amount surface as its raw figure (9,900); it now requires that it surface in
   `Tally.unconverted` and stay out of the total. That test was encoding the bug.
5. **Ui edits are uncompiled.** Treat `verify-ui.sh` as the real gate.

## Not in this phase

No screen has been redesigned, no navigation has moved, and no previews are attached —
the sandbox cannot render them, and Phase 0's note about that still stands. The four
destination bar, the capture sheet, Today/Days/Trip/Stats and the design system are
Phase 2 and 3.
