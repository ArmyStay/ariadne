**Browser demo:** [GitHub Pages deploy workflow](.github/workflows/ci.yml) builds the Wasm app on `main` (`app/wasm/build/dist/wasmJs/productionExecutable`). Add the live Pages URL here once Pages is enabled for this repository.

<p align="center">
  <img src="docs/shots/05-nightfall.png" width="320" alt="Nightfall: photo first, amount typed, confirm when ready" />
</p>

<p align="center"><strong>Ariadne</strong> takes the photo now and lets you tally at nightfall.</p>
<p align="center"><em>Not an expense tracker, not a travel app: a trip-scoped spend memory you can keep on the device.</em></p>

---

## What it is

You spent forty rupees on chai and twelve hundred on shoes, and by tonight you won't remember either. Ariadne is the net for the record you would otherwise lose between a market stall and a hotel room.

- **Capture is a photo.** No amount, no category, no note. One thumb, under two seconds, workable with the radio off. The camera opens inside the app - a CameraX viewfinder on Android - so the shutter is one tap from Nightfall rather than one app switch away.
- **The numbers wait for nightfall.** A card stack of the day, photo dominant, two taps each, keyboard first on desktop. On-device text recognition can prefill amounts (Android), and ready cards can be confirmed in one action.
- **Being half-filled is allowed.** Extraction is best effort. When OCR cannot read a figure, the field stays blank and editable - the capture is still a record.
- **Two currencies from the first minute.** Original amount always visible under the converted figure, rounding applied once, at the conversion boundary.
- **It ends.** The trip closes into a card you keep, and the card can be copied as text to share or save elsewhere. No account, no server, no sync, nothing leaving the device by default.
- **It survives upgrades.** An install made by an older build opens its own database, migrates it, and keeps the trip. The opener decides from the columns present, not from a trusted version number alone.

## The promise this repo keeps in code, not in slides

| Claim | Where it lives |
|---|---|
| Money is never a float | `Money` value class over `Long` minor units — `core/model` |
| A capture cannot be lost | photo → row → shutter returns — `CaptureCommitter`, `Outbox` |
| Extraction failure is editable, not fatal | `ExtractionOutcome.Failed`/blank results leave the capture queued for Nightfall |
| Every state transition is legal or refused | `QueueState.next`, a total function |
| No red, ever | `AriadneColors` has no red; Material's `error` is mapped to indigo |
| Search by what you remember | `SearchIndex`: merchant, price, day, currency — one index |
| Retries survive a dead zone | retry/backoff handled by outbox + extraction service, resumed on start |
| A button never silently does nothing | `commitDraft` reports a failed write on the page; `failureNote` in Prefs |
| An old install keeps its data | `openOrMigrate`: decides from the columns present, not from `PRAGMA user_version` alone |
| Startup cannot be blocked by background work | every step in `AppModel.start()` is guarded on its own |

## The screens

Real frames, rendered from the shared Compose UI by `tools/run-preview.sh --shots` — not mockups. Palette and components from a sign-in reference, structure and type from a trips reference; see `docs/DESIGN.md`.

<p align="center">
  <img src="docs/shots/01b-shutter.png" width="200" alt="Shutter" />
  <img src="docs/shots/05-nightfall.png" width="200" alt="Nightfall" />
  <img src="docs/shots/02-home.png" width="200" alt="Home" />
  <img src="docs/shots/07-trip-card.png" width="200" alt="The trip card" />
</p>

Shutter · Nightfall · Home · the card. Timeline, the record, detail, search, onboarding, Stats, Prefs and more are in [`docs/shots/`](docs/shots/).

## The design is a component library, not per-page layout

`core/design` is the whole visual language and every page is composed from it.
The point is not that it looks consistent - it is that a page cannot invent its
own version of a thing that already exists.

| Module | What it holds |
|---|---|
| `Bone.kt` | one palette, one type scale, one optical-correction table |
| `Components.kt` | `Rule`, `Tick`, `Label`, `Title`, `Body`, `Numeral`, `Action`, `OutlineAction`, `QuietAction` |
| `Blocks.kt` | `NavRow`, `SectionBreak`, `Framed`, `FigureRow`, `TapSurface` - the blocks pages kept retyping |
| `Motion.kt` | one duration, one curve, and the four effects built from them |
| `Plate.kt` | the drawn photographs, so every screen has a subject before it has data |

**Motion** is one vocabulary, deliberately small: `Motion.Page` is 180 ms, a single
ease-out with no overshoot. Four effects are built from it, and each one is a
function rather than a tween typed into a page:

- `Reveal(index)` - a list settles in, 34 ms behind itself, capped at six rows.
  Fades only; no slide, because a list that slides up is the most generic
  animation there is.
- the route crossfade plus `Motion.Drift` - ten points sideways as a page
  arrives, because the app is a book and pages turn across, not up. The index on
  Home and the ways into Search both use `Reveal`; Search's four example queries
  are live searches, not captions.
- `animatedLong` - a total counts to its new value. Money here is read in columns
  and compared against a fence, so the direction of a change matters. The value
  underneath stays a `Long`; the float exists only for the frames in between.
- `motionSpec` - the one place that asks `LocalMotionPreference`, so reduced
  motion is honoured everywhere and nothing animates when the platform says not
  to.

**Performance** is the same discipline applied to recomposition. The state object
is immutable and replaced wholesale on every keystroke, so a composable that
takes the whole state recomposes everything around it. The amount plate, the
keypad and the decisions take primitives now, and the columns are keyed on the
facts they actually read - typing a digit recomposes the amount and nothing else.

## Layout

```
:core:model    Money, CurrencyCode, LocalDay, Capture, sealed results
:core:time     Clock (injected; faked in tests)
:core:queue    outbox state machine, backoff, resume-on-start
:core:extract  receipt parser (pure Kotlin) + TextRecognizer interface + 30 fixtures
:core:fx       scaled-integer rate table, HALF_UP conversion, Tally
:core:search   in-memory inverted index
:core:store    SQLDelight schema + migrations, AriadneStore, openOrMigrate, photo store
:core:design   Ink: palette, Instrument Serif + Inter Tight, components, blocks, motion, plate
:ui:app        routes, AppModel, the four tabs and the six stacked pages
:app:android   :app:ios   :app:desktop   :app:wasm
```

`:core:*` never imports `:ui:*`. `:ui:*` never touches a driver. `expect/actual` is
confined to the driver, photo store and recognizer boundaries — everything else,
including every screen and the whole state machine, is `commonMain`.

## Tests

Everything below runs in this checkout with no Gradle and no device. All green:

| Gate | Command | Result |
|---|---|---|
| Domain logic | `./tools/test-domain.sh` | **70 tests** |
| The capture flow, walked end to end | `./tools/flow-check.sh` | **54 checks** |
| The store against a real SQLite file | `./tools/store-check.sh` | **59 checks, incl. three real database shapes** |
| Every page renders | `./tools/run-preview.sh --shots` | **21 frames** |
| All four entry points compile | `./tools/entry-check.sh` | android, ios, wasm, desktop |

The domain suites:

```
MoneyTest                10   minor units, scale, Indian grouping, no float drift
LocalDayTest              7   epoch round-trip over 40 years, leap days, day ranges
QueueStateMachineTest     9   the full machine, including every illegal transition
OutboxTest               12   ordering, resume-on-start, backoff, no-amount captures
ReceiptParserFixtureTest  8   30 real receipt shapes, incl. phone-number and GSTIN traps
FxTest                    12   HALF_UP once, inverse pairs, missing rates, multi-currency totals
SearchIndexTest           12   merchant / price / day / currency, results are captures
```

`store-check.sh` and `flow-check.sh` are the two that earn their keep, and the
reason is worth stating plainly because both were written after being wrong.

The store gate opens **three real database shapes**: the schema exactly as it
shipped at the first commit (kept verbatim in `tools/storecheck/baseline-v1.sql`
and regenerated from git history), a file created by an interim build that never
stamped its version, and a genuinely empty file. Only the last is allowed to
`create`. The earlier version of this test invented a "version 1" that no build
ever wrote to disk, so it passed while every real database in existence failed
with `no such column: capture.category`. A fixture is only worth anything if it
is the thing the user has.

The flow gate walks the same failure deliberately, with a store that refuses
every write, and asserts the form says so instead of sitting there looking dead.

## Database versions

The schema is at **3**, and the two migrations are `1.sqm` (capture gained
`category` and `note`) and `2.sqm` (trip gained `place` and `budgetMinor`).

The version number is written by `openOrMigrate`, not by the generated `create` -
that omission is what broke every existing install, so the opener does not trust
the number on its own. It decides from the file: no `trip` table means new; a
`trip` table means this app has been here before, whatever the pragma claims; and
a failed `migrate` is repaired by adding only the missing columns, because a
half-upgraded file makes `ALTER TABLE ... ADD COLUMN` fail with "duplicate column
name". On desktop, a file that cannot be reconciled is moved aside and kept -
never deleted - and the app opens.

## Run it

The four-target Gradle build has not been run in a sandbox with enough memory, so
the paths that are verified are Gradle-free:

```bash
./tools/run-preview.sh             # compile + open the real shared UI in a window
./tools/run-preview.sh --phone     # 420x860, the phone layout
./tools/run-preview.sh --shots docs/shots   # render every page to PNG, headless
./tools/test-domain.sh             # 70 domain tests
./tools/flow-check.sh              # 54 end-to-end state checks
./tools/store-check.sh             # the store on real SQLite, plus the migration
./tools/entry-check.sh             # all four entry points, signature-checked
```

See **[RUN.md](RUN.md)** for the full walkthrough, or `TESTING.md` for the short version. And on a machine with the memory for it, the real targets:

```bash
./gradlew :app:desktop:run          # the comfortable triage surface
./gradlew :app:wasm:wasmJsBrowserDevelopmentRun   # the browser build
./gradlew :app:android:assembleDebug
./gradlew jvmTest                    # the shared suites
```

## Status, honestly

Implemented in this checkout: the shared model/queue/parser/FX/search/store, schema migration path, design system and motion vocabulary, tabbed navigation and stacked pages, CameraX capture wiring on Android, on-device ML Kit text recognition on Android (`core/extract` contract end-to-end with graceful fallback states), extraction-result refresh into the UI, and a Nightfall batch-confirm path for captures that already have amounts.

Still not fully verified from this environment and left for human/device confirmation:

- **Real device run and footage.** Camera and ML Kit behavior must be confirmed on an Android phone/emulator outside this sandbox.
- **Full four-target Gradle build and CI green on your runners.** `BUILD-STATUS.md`/`TESTING.md` should be updated with the exact commands and outcomes you run.
- **Live GitHub Pages URL.** The workflow builds and can deploy the Wasm distribution on `main`; enable Pages and add the final URL at the top of this README.
- **Non-Android OCR bindings.** Desktop/iOS/Wasm still use fallback recognizers by design/limitation; extraction failures remain editable blank fields.

Performance fixes described in history should be re-measured on device; do not treat sandbox-only checks as hardware proof.

Apache-2.0. Kotlin Multiplatform Contest 2027 entry.
