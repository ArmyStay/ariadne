# Phase 8 — the timeline, rebuilt to the journey reference

## What this is

The Timeline was the last page still drawn as a list. It worked — a date block, a state
word, a figure, and a 24-hour band showing when the day happened — but it read as a feed
of rows rather than as a page about a journey, and the reference you sent is the better
shape for it: a globe hung off the top corner, a masthead set over it, the trip's own name
under a section mark, what it has cost so far, then the days as a ledger spine.

This phase rebuilds that page to the reference, in this app's language: hairlines, tracked
capitals, one blue.

## What the page is now, top to bottom

1. **The globe.** A watercolour of the earth's limb with a plane crossing it, hung off
   the top-right corner and bleeding past the page edge. It is the only picture in the
   product. It sits *behind* the content and *outside* the scroller, so it stays still
   while the days move under it — a watermark that scrolls is a banner, one that does not
   is paper. It carries no description, so a screen reader steps straight over it, and it
   prints at 18 % on night stock rather than disappearing.
2. **The masthead.** `02 — KOREA` on the left, `RECORD` on the right with a hairline
   under it, then **Timeline** set at 50 sp upright (76 sp on a wide page), then the lede,
   then the rule. The headline is set larger than the book's display scale because on this
   page the headline *is* the page; it is left upright because an italic headline over a
   watercolour reads as a magazine cover and this is a ledger.
3. **I · THE JOURNEY.** A section marked by a short upright bar rather than a roman
   numeral — this page already carries a plate number and a date, and a third numbering
   system would be one too many. Under it: the trip's name in the serif, the trip's
   identity and dates as one tracked line (`KOREA · 2026-11-27 — 2026-12-03`), then
   **SPENT SO FAR** over the trip total with `0 OF 7 DAYS CLOSED` on the same baseline,
   then a rule.
4. **II · THE DAYS.** One row per day: a hairline spine down the margin with a ring at
   each date, the date as a printed block (`27 / NOV / FRI`), an upright hairline, then the
   day's state stamped in a soft wash — `IN THE RECORD`, `SETTLED`, `2 TO SETTLE` — one
   line under it saying what was kept (`NOTHING KEPT`, `3 KEPT · ₹1,240.00`), a chevron,
   and a hairline under the content only. The whole row is the tap target and one TalkBack
   node: *“2026-11-27, in the record, nothing kept.”*

## What went, and why

**The 24-hour day band.** It plotted every photograph at the hour it was taken and was
genuinely novel. It is gone from this page, and `DayBand`, `DayPlate` and its legend are
deleted rather than left unused. Two reasons, in order:

- the reference draws a day by its **state**, and two readings of the same row competed
  for the eye at 84 dp;
- the state is the fact a traveller cannot recover from the photographs. A day with eight
  photographs has a shape whether or not the app draws it.

`docs/DESIGN.md` is updated to say so rather than to keep describing a component that no
longer exists.

## The one model-level bug this fixed

The old plate formatted each day's figure with `state.dayCurrency` — the currency of the
day that happened to be *open*. `dayFigures` is already in the trip's base currency
(`loadTripDays` converts every capture at its own pinned rate), so a THB day in an INR
trip could print a baht figure with a rupee sign. The rebuilt page formats with
`trip.baseCurrency`, and **SPENT SO FAR** is the plain sum of that map. This is now item 4
of `docs/UI-AND-FLOW.md` §10.

## The image, and how it is kept

The artwork is the globe from the wallpaper you sent, cropped to the hero region
(earth's limb, orbit arcs, plane), with the near-white wallpaper keyed out to transparency
so it sits on the paper with no edge and no rectangle. It ships as
`core/design/src/commonMain/composeResources/drawable/globe_hero.png` — 620×835, 180 kB,
192 colours, no dither — and is drawn by `GlobeSky` in `core/design/.../Sky.kt`. The crop
is deterministic: the same source rectangle, the same alpha ramp
(`alpha = clamp((0.925 − luminance) / 0.07)`), the same resize.

## What else changed under the page

- `Page` gained a `backdrop` slot, drawn first and filling the page, outside the scroller.
- `Masthead` gained `titleSize`, so one page can set its own headline without every
  masthead drifting.
- `Numeral` gained `serif`, for headline figures only. Ledger figures stay tabular
  grotesque so a column of amounts still aligns digit for digit.
- New primitives: `MarkHead` (the bar-marked section), `StatusTag` (the rounded wash
  stamp), and `Glyph.Chevron`.

## Verified

- Nothing was rendered. There is no JDK, no display and no Compose jar cache in the
  sandbox this was written in, so `tools/verify-ui.sh` and `tools/shoot.sh` could not run:
  the globe's placement, the 2 dp section bar, the row at 200 % text and the night-alpha
  are **written, not looked at**. CI's `shared` and `desktop` jobs are the compile gate for
  this branch.
- `:core:model` and `:core:fx` suites: unchanged and re-run green (see the PR).
- The `ios` job was already failing at `8185219`, before this phase, on
  `compileKotlinIos{SimulatorArm64,Arm64,X64}`.

## Deliberately not matched

The reference's bottom bar is a floating rounded pill with the camera button above its
right end. The bar's **labels** no longer differ: `main` renamed the four destinations to
**HOME / TIMELINE / STATS / MORE** in the export pass this branch merges. What stays
different is the bar's *shape* — flat, square-cornered, with the shutter in the gap it
reserves — which was an explicit decision about the shell and is not something a page
rebuild should quietly reverse.

## Merciful note: this branch merges main

`main` moved while this was being written: `d6f6fe0` added local data export, manual
entry, a More destination and its own (smaller) pass at this same timeline. This branch
merges that commit rather than rebasing over it, so the PR shows only the timeline change,
and the two versions of the page were reconciled by hand: the reference layout wins, and
the data `main` had added to that page — `trip.place` in the meta line, `tripTotalMinor`
as the figure, `budget` under it, and "Trip total" instead of "Spent so far" on a closed
trip — is carried into it.
