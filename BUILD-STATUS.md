# Build status

Last updated: 2026-11-23.

## Verified — actually run, in this session

| Check | Command | Result |
| --- | --- | --- |
| Shared UI compiles | `tools/verify-ui.sh …` | **0 errors** |
| Domain logic | `tools/test-domain.sh` | **OK (70 tests)** |
| Real UI renders | `tools/run-preview.sh --shots docs/shots` | **21 frames, PNG** |
| Real UI window | `tools/run-preview.sh` | launches under Xvfb |
| SQLDelight codegen | `tools/store-check.sh` | generates from `Ariadne.sq` |
| `AriadneStore` vs real SQLite | `tools/store-check.sh` | **30 checks pass** |
| The capture flow, walked | `tools/flow-check.sh` | **32 checks pass** |
| Four entry points compile | `tools/entry-check.sh` | **all four** |
| Desktop app launches + persists | manual, under Xvfb | window opens, DB written |

The compile covers `:core:model`, `:core:time`, `:core:queue`, `:core:extract`,
`:core:fx`, `:core:search`, `:core:design` (Ink palette, components, plate,
motion), `:core:store` (`InMemoryStore`, `PhotoPaths`), the four tab destinations,
the five stacked pages, the tab bar with its notch, and `AppModel`, `Route`,
`AppGraph`, `AriadneApp` — against real Compose jars with the real Compose
compiler plugin, no Gradle.

The screenshots in `docs/shots/` are produced by `ImageComposeScene` rendering
that same `AriadneApp`, so the layout, type scale and spacing on screen are the
ones the four app targets compile.

## Written, not executed

| Area | Why not | Risk |
| --- | --- | --- |
| Gradle four-target build | sandbox is 2 GB, no swap; AGP 9 configuration is OOM-killed | medium — one `./gradlew build` settles it |
| Android/iOS on-device run | no SDK, no Xcode here | low — the Ariadne code in both now compiles against the real schema |
| iOS linking | no Kotlin/Native here | unknown — nothing below the entry point is iOS-specific |

## Flow pass — the front door, the frame, the fence

Three changes, each of which the product was missing rather than merely
under-designed.

**The front door is creating a trip.** On a first run there is no trip, so there
is nothing to show: the record has to be scoped before a frame can hang on it.
`Route.NewTrip` and `NewTripPage` now take name, place, dates, currency and an
optional budget, in a stack of labelled hairlines rather than a form. A draft is
held in `AppUiState.draft`, validated on commit (`Ariadne` can say *which* field
is wrong), and only then written. `startEmptyTrip` and `startSeedTrip` are now
thin wrappers over the same draft path, so there is exactly one place a trip is
born.

**The shutter stops and shows you the frame.** It used to fire and throw you
back into the app, which meant the photograph vanished into a queue you could not
see. `Route.Frame` holds it up with two ways out and no way to lose it: *keep it
for nightfall* (nothing to type) or *fill it in now* (amount, category, what it
was for). The photograph is the largest element on the page, at the same measure
it had in the viewfinder.

**Photos are written before the row.** `PhotoFiles` is the new seam: one write,
one read, one delete, one footprint, and a platform supplies it. The order is
enforced in `AppModel.capturePhoto` — bytes hit the disk first, then the row, so
a capture can never point at a file that was never written. `CameraBinding` in a
`CompositionLocal` is how a camera arrives; with no binding (every target today)
the ruled plate is drawn, the shutter still fires, and the flow is identical.
The page never branches on the target, only on whether it was handed a frame.

**The budget is a fence, not a target.** `Trip.place` and `Trip.budgetMinor` are
new columns. `remainingAgainstBudget` is arithmetic, not a stored column, so it
cannot disagree with the captures. Stats states over-budget in the same voice as
under: this is a record, not a scold.

### On Appwrite

A server API key was supplied for Appwrite storage. It was **not** wired in.
That key grants full read/write/delete on the whole database; shipping it in a
client — and the Wasm build is public — would hand it to anyone who opens dev
tools, and it would contradict the app's central claim that nothing leaves the
device. Trips live in SQLite, photos in the app-private folder. Decision taken
by the user, recorded here because it is a design decision, not an omission.

## Design pass — Ink

The palette is the reference's (cool paper, one blue, slate) and the bones are the
second reference's (big italic serif, white panels, ink-filled controls, a bottom
bar with the shutter in a notch). Type is Instrument Serif + Inter Tight, both
vendored under OFL in `core/design/src/commonMain/resources/fonts`.

## What the first compile pass found

The store and the entry points had never been built. Building them found five
real defects, all now fixed:

1. **`AriadneStore` had no `override` modifiers** — 18 members, so it could never
   have compiled as written.
2. **`row.base_`** — a reference to a column that does not exist. The schema says
   `base`; this was a stale workaround for a name clash.
3. **`com.ariadne.store.Schema`** in all four entry points — no such object. The
   schema lives on the generated interface: `AriadneDb.Schema`.
4. **Desktop crashed on every relaunch.** `Schema.create` ran unconditionally, so
   the second start threw *"table trip already exists"* and died before the window
   appeared. It now checks `sqlite_master` first. This was the real bug of the
   batch: the app looked fine on a fresh install and broke on every launch after.
5. Missing transitive jars in the Gradle-free harness (`jdbc-driver`,
   `sqlite-jdbc`, savedstate/lifecycle) — a harness problem, not an app one.

## Known next steps

1. `./gradlew build` on a machine with memory. `tools/store-check.sh` and
   `tools/entry-check.sh` have already removed the highest-risk unknowns.
2. Point `AppGraph` at `AriadneStore(driver)` per target and delete `InMemoryStore`
   from the shipping paths (keep it for previews and tests).
3. Bind OCR per target behind `TextRecognizer`: ML Kit (Android), Vision (iOS),
   Tesseract (desktop), none (Wasm — by design).
4. Wasm: replace `InMemoryStore` with the IndexedDB driver before the Pages deploy.
5. A real camera/photo picker behind `PlatformPhotos`.
