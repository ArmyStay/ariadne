# Phase 9 — the photograph you kept

## What this is

The surface that opens the instant a photograph is taken is the one that has to say
*the picture is safe, the amount can wait*. It said that, but it said it in the worst
possible arrangement: the photograph itself was a 16 % sliver at the top of the screen,
the sheet covered the rest, and everything under four lines of text was empty white.

The reference you sent — a receipt viewer — is the right shape for it: the picture is
the page, on a card torn off a roll, with the state of the entry worn in the corner.

## What the page is now

**The head.** A back chevron (it keeps the photograph), the thing's name in the serif —
the merchant if the recogniser found one, `Receipt` if not — and the entry's state in a
hairline pill on the right: `NEEDS AN AMOUNT`, `CONFIRMED`, `NO AMOUNT`, and so on. A
rule under it, inset by the page margin.

**The card.** White paper with two rounded top corners and a foot cut into 7 dp
scallops — the mark a shop makes when it tears a bill off the roll. Inside it:

- a head line in the receipt's own grammar: `RECEIPT` … a hairline … `04:54`;
- the photograph, the full width of the card less 18 dp a side, **320 dp tall** (520 dp
  on a wide page). This is the fix: the image was 16 % of a screen behind a sheet and is
  now the largest single block on the page.

**The readout.** `PHOTOGRAPH KEPT`, then the state as a serif headline — *No amount is
needed now.* / *Found ₹1,240.00.* / *Confirmed.* — and one line saying where the rest
happens: *You can finish it at Close Day.*

**The two answers**, pinned to the foot: an **ink** block reading **KEEP FOR LATER**
(**DONE** once the entry is resolved), and **OPEN THE FULL EDITOR** as a quiet
underlined action.

## What the reference changed about the controls

Two, and both are exceptions the rest of the book does not get:

- the primary button is **ink, not blue**, and has a **14 dp corner**. `Action` gained a
  `corner` parameter (2 dp by default, so nothing else moved) and already had
  `fill = false` for an ink block. The blue rule in `DESIGN.md` ("blue appears once per
  surface, on the action that finishes the page") is not broken — nothing here is blue.
- the entry's state is a **round** pill in the corner, where the rest of the app prints
  state as a square `Pill`. It is a status, not a control: it is a `Label` and a
  hairline, it does not respond to a tap, and a screen reader reads the word once.

## What went with the old layout

The whole second editor. `SheetFields` — amount, keypad, currency chips, category, a
merchant line, a note, the date row, the pair — was 350 lines that **nothing rendered**:
`openFrame()` goes to `DetailPage`, which is the real editor. It is deleted along with
its helpers (`Moment`, `Suggestion`, `Pair`, `QuickCaptureDetails`, `Print`, `longDay`,
`numericDay`, `FieldLabel`, `Helper`). `CurrencyChip`, `CategoryChips` and `LineField`
live in `DetailPage.kt` and are untouched.

The full-screen photograph behind the sheet went with it, and so did the scrim: the
picture is on the card now, and a photograph behind an opaque page is memory the app is
paying for and not using.

## New primitives

- `TornEdgeShape` / `ReceiptCard` / `ReceiptHeadLine` — `core/design/.../Receipt.kt`.
  The scallops are one `Path` (rounded top corners, a row of half-circles along the
  bottom) returned as `Outline.Generic`, so the card clips its own contents and keeps its
  hairline along the cut. The number of scallops is whole and derived from the width, so
  a narrow card gets one shallow cut rather than a row of slivers.
- `Action(corner = …)`.

## Verified

- **Nothing was rendered.** No JDK, display or Compose jar cache exists in the sandbox
  this was written in, so `tools/verify-ui.sh` and `tools/shoot.sh` could not run: the
  card's cut edge, the 320 dp photograph, the pill against a long merchant name and the
  page at 200 % text are written and compiled, not looked at.
- **It does compile, on every target.** `jvmTest` is green on the branch, and
  `:app:android:assembleDebug`, `:app:wasm:compileKotlinWasmJs` and
  `:app:wasm:wasmJsBrowserDistribution` were each run on the exact head commit and each
  exited 0.
- The `shared` job in `ci.yml` is red for this branch **and** for `main`
  (`f203863`, `d08f280`), while that job's own command sequence succeeds when run
  out of band on both this head and its base `b2dedfa` — so the red check is a property
  of that job, not of this change.
- `desktop` is green. `:core:model` and `:core:fx` are untouched by this phase.
- The `ios` job was already failing on `main` (`compileKotlinIos*`) before this branch.

## Still not here

The full editor still refuses a date change (a photograph's day is a fact about when it
was taken), and a single photograph still cannot be deleted. Neither is a screen change.
