# Phase 7 — the snap sheet fills in the detail

## What this is

The screen that opens the instant a photograph is taken is the one that decides
whether the app is a record or a filing cabinet. It was already a sheet over the page
rather than a page of its own (Phase 5), and it already asked for the amount, the
currency, the category and a name — but it asked in the order a form asks, and it told
you what your money was worth only *after* you had saved it.

Your reference — the receipt detail screen — has the right structure, and this phase
rebuilds our sheet to it, in this app's own language of paper, hairlines and one blue.

## The order the sheet now reads in

1. **The print and the moment.** A thumbnail of what was photographed (the sheet
   covers most of the photo behind it now, so without this you would be typing a
   figure for a picture you cannot see), the day spelled out — `TUESDAY 11 NOVEMBER ·
   05:30` — and the figure as it stands.
2. **The machine's reading, offered.** `Found S$18.41 on it.  yes / no`. Still a
   suggestion, never typed in.
3. **The pair.** Two labelled columns over one sunk block: `ORIGINAL PURCHASE` and
   `BASE TRIP EQUIVALENT`, the figure on the left, `≈` between them, and the rate
   underneath — `rate: 1 SGD ≈ 74.6298 INR`. This is the block that was missing. A
   figure typed in Bangkok used to become rupees somewhere the person never saw;
   now both numbers and the rate that joins them are visible before either is saved.
   When there is no rate on file the block says so — `no SGD rate yet — it will be
   counted in SGD` — instead of inventing a conversion. That is the ₹2,605 bug's
   lesson, applied to the one screen where money first enters.
4. **Read the photograph again.** One action that re-runs extraction on the same
   file. A wrong suggestion should cost one tap, not a retyped receipt.
5. **Amount.** The currency's symbol in its own box, the field, the keypad, and one
   sentence saying what will happen on save.
6. **Currency, category, where, remember it by.** Each labelled, each optional.
7. **Date.** Printed, not editable: a photograph's day is a fact about when it was
   taken. The reference has a dropdown here; this app does not, because moving the day
   would be offering to falsify a record.
8. **Save, or the amount later** — pinned to the foot, never below the fold.
9. **Back, top-left.** Drawn as a chevron, `contentDescription = "Back"`, and it keeps
   the photograph — beside it, in the smallest type on the sheet: *the photograph is
   kept.*

## What had to change behind the screen

Two additions to the model, both small and both pure where they could be:

- `AppModel.sheetPreview(): SheetPreview?` — the pair, derived, never stored. It reads
  the typed text, the capture's currency, the trip's base currency and the rate table
  for *today* (the rate `saveDetail` will pin), and returns the base figure, the rate
  line, or nulls where there is genuinely nothing to say. It falls back to the machine's
  suggested amount when nothing is typed, so the pair is visible before you commit to a
  figure.
- `AppModel.reExtractCurrent()` — re-runs `ExtractionService.extractOne` against the
  same file, then re-reads the row and hands the result back to the sheet. It never
  throws: a failed read lands in the same suggestion row the shutter's failures land in.

`FlowCheck` now asserts the derivation without a screen: nothing typed means nothing to
convert; a typed figure names its own currency; a same-currency pair converts to itself;
clearing the field clears the pair.

One shared component changed: a chosen **category chip is now filled ink** rather than
blue-washed, which is what a chosen currency chip already did and what the reference
does. Two rows of chips that select differently was the kind of inconsistency that makes
an app feel assembled.

## Verified

- `:core:model` 47 passed, `:core:fx` 38 passed, green.
- Whole tree passes the Kotlin compiler for syntax, conflicting declarations and
  non-exhaustive `when`s.
- **Nothing was rendered.** The sheet's new proportions (84% of the screen, a scrolling
  body with a pinned foot), the pair block at 200% text, and the keypad's 46dp rows have
  not been looked at on a screen. `tools/verify-ui.sh` on a machine with Gradle is the
  gate. New stills: `04-sheet`, `04-sheet-typed`, `04-sheet-desktop`.

## Still not here

Deleting a single photograph. The reference has a bin in the corner; this app can
delete a whole trip and not one capture, and a button that cannot do what it says is
worse than no button. That is a model change, not a screen change, and it is not in
this phase.
