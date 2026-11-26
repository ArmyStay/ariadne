# Phase 3 — the visual system

The audit's third phase was "theme, type scale, components, spacing, motion, copy,
previews". Most of that already existed before this phase started: `Bone`/`Paper`
have been the palette since the first commit, the type is two vendored families with
tabular figures, `Motion` covers page transitions and the reduced-motion preference,
and `Components.kt`/`Blocks.kt`/`Ruled.kt` hold the primitives every page draws from.
So Phase 3 here is the part that was actually missing, not a re-do of the part that
was already there.

## Commit 1 — one palette, one measure, one target size

### `AriadneTheme` (`:core:design/AriadneTheme.kt`)

The shell now contains real Material components — `NavigationBar`, its items, the
docked `FloatingActionButton`, the diagnostics surface — and each of them asks the
theme for every colour it is not told. Without a theme they answered in the stock
Material purple: a ripple, a focus ring, a disabled label, all in a hue that exists
nowhere in this app.

`Paper.materialScheme()` translates the palette into a Material 3 `ColorScheme`, and
`AriadneTheme(paper)` installs it. Pages keep taking `paper` as a parameter; this is
only for components that ask instead of being told.

Two mappings are deliberate and documented in the file:

- **error is blue.** This app has exactly one chromatic value on purpose. A red that
  appeared only through a Material default would be the loudest thing on a page about
  somebody's holiday; things that need to look wrong here look *heavier*.
- **surfaceTint is blue**, so any elevated Material surface tints towards blue rather
  than lavender.

### `BudgetBar` (`:core:design/Budget.kt`)

Three pages had grown three versions of the same idea: Today said it in a pill, Trip
drew two nested `Box`es, Stats wrote it in a line of figures. It is now one `Canvas`:
a sunk track, the spent share in ink, and a **pace tick** at the point the trip's days
have reached — because a bar says how much is gone and the tick says whether that is
too much *by now*, which is the only question a person has in the middle of a trip.

Trip and Stats both draw it. Trip's tick is days closed ÷ days in the trip; Stats'
is the same, so the two pages cannot disagree.

### 48dp

Every tappable row and chip was 44dp. Apple's guidance says 44 and Material's says
48; this app follows the larger one, so `heightIn(min = 44.dp)` became 48 in the
currency chips, the category chips, the paper chips, the register rows and the day
rows, and `Modifier.rowTap` gained nothing — the height belongs to the caller, since
only the caller knows what the row holds.

## Commit 2 — the shape of an empty page, and a bar that settles

`EmptyState` (`:core/design/States.kt`) is now the one shape for "there is nothing
here yet": a small italic title naming the situation, one sentence saying what to do
about it, and optionally the one control that acts. Five pages had five versions — a
bare sentence on Today, an 18sp serif line in the register, a stretch of empty paper
on Stats — and an empty screen is the first thing someone sees on a new trip, so it
should look deliberate rather than unfinished. Used on Today, Days, Stats, the
register and search.

`BudgetBar` eases to its value instead of snapping, because a figure read against a
fence is worth watching move, and settles on the first frame when the device asks for
reduced motion.

### What is not here

- **`@Preview` functions.** `compose.components.uiToolingPreview` is not in the
  version catalog and cannot be resolved in this sandbox, and a preview that has never
  been rendered is a claim rather than a check. The repository already has a real
  screenshot mechanism — `preview/` renders the actual composition through Skiko via
  `tools/run-preview.sh --shots` — and that is the honest one to extend. Phase 3's
  preview deliverable is therefore blocked on a machine with Gradle, not on design
  decisions.
- **Dynamic colour.** Decision D5 was "dynamic colour only as an opt-in on Android 12+
  via `expect/actual`". It is not built: with three explicit paper choices in
  Preferences, a fourth source of colour would need its own setting and its own
  contrast checks, and the audit's recommendation was already the conservative one.
- **Shared-element motion (thumbnail → detail).** Compose Multiplatform's shared
  transition API is still experimental and its behaviour at 200% text is exactly what
  cannot be checked here. The page crossfade and drift stay.
