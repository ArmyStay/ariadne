# Build status

Last updated: 2026-11-26 (second review pass: night paper, currency catalogue, one-blue audit, Android photo picker, IME insets, token fixes, 200% stills; earlier edits: desktop OCR bound via the Tesseract CLI in `app/desktop`, tab bar now Home/Timeline/Trip/Stats replacing Prefs, contrast pass in `Bone.kt`, six curated hero shots in the README, docs/DESIGN.md + docs/ESSAY.md fixes; Gradle/device verification pending).

> Note: Checks listed below were recorded in prior sessions and were not re-run in this session after the latest source edits.

## Review pass (2026-11-26): flow, authority and components

`docs/UI-AND-FLOW.md` was reviewed against a stranger's reading of the repo. This
pass turns the review's findings into code, not a report. **None of it has been
compiled in this environment** — there is no JDK, no Gradle and no `kotlinc` in
the sandbox, so `tools/verify-ui.sh`, `tools/flow-check.sh` and the real targets
are all unrun, and every claim below is a reading of the source rather than a
green check.

What changed:

- **Home means one thing.** The Home tab routes to `Route.Home` (today, plate 01).
  The register is a stacked page with a `HOME` page action, plate `none`.
- **First run opens the title page.** `AppModel.start()` routes to `Onboarding`
  when the register is empty and to `Trips` when every trip on disk is closed.
  This supersedes "the front door is creating a trip" in the Flow pass below:
  Onboarding's *Begin empty* goes to the same `NewTrip` draft path, one tap away.
- **A closed trip is not a capture target.** `closeTripIntoCard` clears
  `activeTripId`, `capturePhoto` refuses a closed trip, `bindTrip` does not mark
  one active, and Home, Capture, the record and the trip page each say so instead
  of offering a shutter that would do nothing.
- **One keypad, one measured rule.** `RuledKeypad` and `MeasuredRule` moved to
  `:core:design`; both pages use them, rows have minimum rather than fixed
  heights, and chip tap targets are 44dp inside their printed look.
- **Smaller corrections.** Search results open the photograph; the library link
  opens the picker directly and appears only where a picker is bound; Detail's
  currency chips include the trip's own codes; Preferences' numerals run I–IV;
  the long-press promise is gone from copy;
  `CLOSE TODAY` is disabled on a filed day as well as an empty stack; plate
  numbers no longer collide (`01`–`09` are the trip's pages).

`docs/UI-AND-FLOW.md` carries the full inventory, and Appendix C maps each review
item to its status and to the pages that changed.

## Second review pass (2026-11-26): the backlog the first pass left

The first pass shipped the flow and authority fixes and listed what it had not
done. This pass does that list, except the two items that cannot be written
honestly without a compiler for their target.

**Night paper.** `Settings.Theme` already existed in the model and the palette
already had a night half; nothing selected either. Now the choice is stored with
the settings, loaded before the first frame, offered in Preferences as three
chips (Light / System / Night), and resolved once in `AriadneApp`, where `System`
asks the platform. Every token swaps, including the kept card's warmer stock.

**A currency catalogue.** `CurrencyCatalog` holds all sixty-seven codes
`CurrencyCode` already scales, each with a name and a sign, searchable by code or
name. New trip and Preferences choose from it; Detail's four chips are its
shortlist. This replaces a three-letter box on New trip that accepted `ABC` and a
six-code row on Preferences that could not reach most trips. `PYG`, `XOF` and
`XAF` were scaled but not "known"; they are now.

**One blue action, audited.** A design rule that is only prose erodes. Every
surface now gets an `AccentBudget`; an accented, enabled `Action` claims it; a
second claim on the same surface is recorded, and the stills harness fails if the
audit is not empty after rendering every page. This is not a type — that would
need a page DSL — but it is a gate.

**Tokens that lied.** `InkMid` and `InkSoft` were the same colour (`#475569`);
`InkMid` is now `#334155`, because it is the tone a quiet *control* uses. `TripGround`
existed unused; the trip card is now printed on it, with the card raised on top.

**Keyboard and text size.** `imePadding()` and the system bars are applied once at
the root, so no page has to know the keyboard is open. The tab bar's height
follows the font scale instead of being fixed. Three stills are defined at
`fontScale = 2` (`02-home-200`, `05-nightfall-200`, `02d-prefs-200`).

**Android has a photo picker.** `PickVisualMedia`: the system picker, no storage
permission, bytes bounded to 1600px and re-encoded exactly as the camera bounds
them. It is bound in `MainActivity` beside the camera. `tools/entrycheck/stubs/android`
grew the launcher generics, `PickVisualMedia`, `Uri`, `ContentResolver.openInputStream`
and `BitmapFactory.decodeStream` so the entry-point gate can still compile the module.

### What is verified, and what is not

Verified in this environment: the currency catalogue and its search (compiled with
the real Kotlin compiler against `CurrencyCode`, and exercised with 21 assertions
covering catalogue completeness, name coverage, search order, case handling,
shortlist stability and the zero/three-decimal currencies). The Android picker was
compiled against the repo's own Android stubs plus minimal Compose/coroutines
stubs, so its Kotlin type-checks — but not against the real androidx API.

Verified in the third pass: the startup rule. `startRoute(hasAnyTrip, hasOpenTrip)`
was compiled against `:core:model`'s sources with the real Kotlin compiler and
executed (all three cases: onboarding / home / register) — it is the one place the
P0-1 finding lived. The currency harness from the previous pass still passes.

Not verified: every Compose file in this pass, the shared UI build
(`tools/verify-ui.sh`), the flow walk (`tools/flow-check.sh`), the entry-point gate
(`tools/entry-check.sh`), all four targets, and every still. The only renders in the
repository now live under `docs/shots/archive-2026-11-26-pre-third-review/` and are
labelled as pre-review; `docs/shots/` holds no current render. The harness lists 44
frames — fifteen at 200% text, two in landscape, two closed-trip states — and none
has been rendered here, because there is no Compose runtime in this environment.

Not written: the web and iOS pickers. The browser needs typed file-input interop
and iOS needs a `PHPickerViewController` delegate; neither can be compiled here,
and unchecked platform interop is more likely to break those two builds than to
help them. Android's implementation is the pattern to copy.

## Build log round (2026-11-24): the iOS X64 failure + warning cleanup

A workspace build log (`gradlew :app:android:installDebug`, AGP 9.5.2) failed during
configuration on every module with the systematic error:

    KMP Dependencies Resolution Failure
    Source set 'commonMain' couldn't resolve dependencies for all target platforms
    Couldn't resolve dependency 'org.jetbrains.compose.runtime:runtime:1.12.2'
    ... Unresolved platforms: [iosX64]

so the build was not green and the first failing build on a stranger's machine undid the
reducibility work above. Changes made on the current branch to fix the failure and the
warning cascade in that log:

- Removed `iosX64()` from every module (`app/ios`, `core/{design,extract,fx,time,queue,
  search,store,model}`, `ui/app`): the compose dependencies do not resolve for it with
  the pinned compose 1.12.2, so the target cannot be part of a green build. (Remaining
  Intel-mac users: `iosArm64()`/`iosSimulatorArm64()` still cover the simulator.)
- Removed `android.builtInKotlin=false` and `android.newDsl=false` from `gradle.properties`
  (the AGP-9.0 default is `true`), and dropped `org.jetbrains.kotlin.android` from
  `plugins` of `:app:android` and the root project — the warnings said to migrate to the
  built-in Kotlin.
- Added `androidTarget { withHostTest() }` to the seven core modules with an android
  target — the "android host tests are not enabled" warnings.
- `applicationVariants`/`testVariants`/`unitTestVariants`/`libraryVariants` callers were
  not found in the repo; run the build with `-Pandroid.debug.obsoleteApi=true` and paste
  the full log (see below) — a couple of warnings still need their caller located.

**Verification still to do on your machine:** re-run
`./gradlew.bat :app:android:installDebug` and confirm the install succeeds with few or
no warnings, and paste the output here again — the workspace log still needs to be
reduced to the remaining-obsolete-API items.

## Verified in prior sessions / by existing repo checks (not re-run in this session)

| Check | Command | Result |
| --- | --- | --- |
| Shared UI compiles | `tools/verify-ui.sh …` | **0 errors** |
| Domain logic | `tools/test-domain.sh` | **OK (70 tests)** |
| Real UI renders | `tools/run-preview.sh --shots docs/shots` | last run produced 21 frames; the harness now lists **44** (not re-run) |
| Real UI window | `tools/run-preview.sh` | launches under Xvfb |
| SQLDelight codegen | `tools/store-check.sh` | generates from `Ariadne.sq`, `verifyMigrations` on |
| three real database shapes open correctly | `tools/store-check.sh` | **ok**: baseline, interim, fresh |
| `AriadneStore` vs real SQLite | `tools/store-check.sh` | **59 checks pass** |
| The capture flow, walked | `tools/flow-check.sh` | **54 checks pass** |
| Four entry points compile | `tools/entry-check.sh` | **all four** |
| Desktop app launches + persists | manual, under Xvfb | window opens, DB written |

The compile covers `:core:model`, `:core:time`, `:core:queue`, `:core:extract`,
`:core:fx`, `:core:search`, `:core:design` (Ink palette, components, plate,
motion), `:core:store` (`InMemoryStore`, `PhotoPaths`), the four tab destinations,
the five stacked pages, the tab bar with its notch, and `AppModel`, `Route`,
`AppGraph`, `AriadneApp` — against real Compose jars with the real Compose
compiler plugin, no Gradle.

The screenshots are produced by `ImageComposeScene` rendering that same
`AriadneApp`, so the layout, type scale and spacing on screen are the ones the four
app targets compile. Everything rendered before 2026-11-26 has been moved to
`docs/shots/archive-2026-11-26-pre-third-review/` and is **not evidence** for the
current design; the README there says what each frame predates.

## Not yet verified in this environment

| Area | Why not | Risk / next action |
| --- | --- | --- |
| Full Gradle build/tests on your machine or CI | not executed in this sandbox (no JVM, and egress is limited to github.com / registry.npmjs.org / pypi.org) | Run ./gradlew jvmTest :app:android:assembleDebug :app:desktop:compileKotlinJvm :app:ios:compileKotlinIosSimulatorArm64 :app:wasm:wasmJsBrowserDistribution (or rely on CI) and record actual outcomes; already green in CI on main |
| Desktop OCR on a real machine | no Tesseract installed here, and the CLI is the binding | `tesseract --version`; the recognizer returns a clean "tesseract not installed" failure when absent — the capture still reaches Nightfall |
| Phone video of the shutter path | Xvfb + ImageMagick not asserted here | `tools/record-phone-video.sh` (see step 3 of the user's order); the preview script needs a JVM |
| Android camera + ML Kit on device/emulator | no device/emulator here | Install debug build, verify shutter ? OCR/fallback ? Nightfall ? card flow, and record ~30s footage |
| Live GitHub Pages URL | Pages enablement/URL not controlled here | Enable Pages for main, confirm deployment, and add the final URL at the top of README.md |
| Wasm persistence across reloads | browser app currently uses in-memory store | Demo is acceptable if stated honestly; persistent web storage remains follow-up work |

## Hierarchy pass — one blue thing per page

Looking at the rendered frames side by side, the recurring defect was not colour
or type: it was **two competing primary actions**, which is what a generated
layout does when it has no opinion about which choice matters.

- **Nightfall (wide)** had "Confirm" and "Close the day" as two identical
  full-width blue bars, one above the other. Closing the day is how you *leave*,
  not a second decision, so it is now a hairline bar under a rule with a caption
  that says what it costs you. Confirm is the only blue on the page.
- **Nightfall (wide)** also had the plate on a fixed 4:5 box inside a weighted
  row, which overran the viewport and cut the caption off the bottom. The plate
  now takes the space that is left, and the stack position and Earlier/Later
  moved under it where they belong.
- **The trip's page** stacked "See every day" over "Close the trip into a card".
  Closing is now the single action; the record is a quiet exit under it. The
  place and the budget also appear here now, hung under the dates with their
  glyphs, because they are facts about the trip and this is the trip's page.
- **Home's index** was four rows of bare serif. Each row now carries its glyph in
  the hanging margin, so the block scans as a list of places instead of four
  lines that happen to be different words. The same treatment reached Prefs.

## The camera

`AndroidCamera` is a real CameraX binding: `ProcessCameraProvider` bound to the
activity's lifecycle, a `PreviewView` behind the ruled frame, and an `ImageCapture`
case that fires from the shutter. The pieces that matter, in the order they break:

- **Permission** is asked when the shutter page is entered, not at launch. Refused
  means the plate is drawn and the caption says "Camera permission declined" — the
  app stays completely usable, which is the stance everywhere else too.
- **The lifecycle** is unbound in `onDispose`, not left to the activity. A camera
  held open behind a page is warm hardware and a flat battery.
- **The bytes** are decoded, rotated by `imageInfo.rotationDegrees`, scaled to
  1600px on the long edge and re-encoded at quality 88. A 12MP sensor frame is
  ~4MB; a receipt does not need it. `ImageProxy` is closed in a `finally` —
  skipping that locks the capture pipeline and the next shot never arrives.
- **Resolution** uses `ResolutionSelector` with
  `FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`, not the deprecated
  `setTargetResolution`, so a sensor that cannot do exactly 1600 still gives its
  nearest larger size instead of a postscript stamp.
- **Photos** go to `filesDir/photographs/<trip>/<day>/<id>.jpg` via `AndroidFiles`,
  written to a `.part` name first so a kill mid-write cannot leave a half-JPEG
  under the real name. Never the shared gallery: the photo backup is exactly the
  thing this app promises not to do.

`CameraBinding` also gains `capture()`, and `shutterAction()` in the shared UI
wires the shutter to whichever binding is in the `CompositionLocal`. With none,
the old placeholder path runs — so the desktop preview, the Wasm build and the
iOS target compile and pass the same 50 flow checks.

**Not verified:** the camera has not been run on a device. It compiles against
stubs whose signatures were checked against the CameraX 1.4 reference, and the
shared UI is unchanged in behaviour, but "compiles" is not "opens the sensor".

## Stale-control pass — dead pages out, icons in

A sweep of every control in the app found three classes of rot.

**Two whole pages were never rendered.** `SettingsPage.kt` and `TripsPage.kt` were
built, styled, and then superseded by `PrefsPage` and `HomePage` — and left in the
tree. Their dead ends were visible from the surface: `Route.Settings` resolved to
`PrefsPage`, so the old page's "Delete this trip" lambda was empty and unreachable
at the same time. Both files are gone; git has them if they are ever wanted.

**Two live controls did nothing.** "Delete this trip" and "Share" were
`{ }`. Both are now real: deletion is two taps (arm, then confirm) because there
is no copy anywhere else, and it removes the trip's photographs as well as its
rows. Share copies the card as text, generated from the same state the page
draws, so the shared numbers cannot disagree with the printed ones.

**Two routes carried lies.** Prefs linked to `TripCard("0")`, a hardcoded id that
resolved to whatever `state.trip` was; it now closes the trip properly. The
preference rows ("Date format", "Paper", "Reading") looked like settings and were
not — the two that can change (base and home currency) are now real pickers, and
the two that cannot are set as read-outs that say so.

**Icons.** There was one icon system in the app — the four tab glyphs, drawn as
line art in `TabBar.kt` — and nothing else. A Material icon font would arrive with
its own weight and joins and immediately look bolted on beside this type, so
`Glyphs.kt` draws eighteen glyphs on the same 0..1 grid at the same 1.4dp stroke:
trip, date, place, budget, camera, photograph, night, search, record, stats, gear,
waiting, remove, share, back, done, ledger, edit, paper. They scale from 14dp
beside a label to a page ornament, and they read as cut from the same plate as the
rules and the letters.

**One bug the flow check caught:** deletion iterated `confirmedForTrip`, so
photographs of *unfinished* captures stayed in the folder forever. `capturesForTrip`
now returns every state, and the check asserts that a deleted trip's folder is
empty while a neighbouring trip's is untouched.

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
3. Bind OCR per target behind `TextRecognizer`: ML Kit (Android), Tesseract CLI
   (desktop), none (Wasm — by design; iOS is an open follow-up).
4. Wasm: replace `InMemoryStore` with the IndexedDB driver before the Pages deploy.
5. A real camera/photo picker behind `PlatformPhotos`.

## Reliability + motion pass (same day, after a side-by-side test round)

Three reports came back from someone running the app on their own machine: "Open
the trip" did nothing, the app felt laggy, and there were no modular effects.

**The button.** Not a UI bug. `trip` had gained `place` and `budgetMinor` while
the SQLDelight schema stayed at version 1, so an existing device never ran a
migration; the INSERT named columns the table did not have, threw inside a
`SupervisorJob` coroutine, and was swallowed. A fresh install worked, which is
why it passed every gate. Fixed with `1.sqm`, `verifyMigrations.set(true)`, and
`openOrMigrate` in `core/store` used by both the desktop entry point and the
store gate. Proven by building a v1 file by hand and migrating it.

**The silence.** A failure now reaches the page: `commitDraft` catches, shows a
sentence on the form, and records `failureNote`, which Preferences displays.

**The lag.** `ExtractionService.drain()` was single-flight already this round;
the remaining source was recomposition width - `Amount(state, paper)` took the
whole `AppUiState`, so a keystroke recomposed the plate, band and keypad. Those
take primitives now, and the Nightfall columns are keyed on the card and the
typed text.

**The effects.** `core/design/Blocks.kt` (NavRow, SectionBreak, Framed,
FigureRow, TapSurface) and four effects in `Motion.kt` (Reveal, page drift,
animatedLong, motionSpec), all honouring `LocalMotionPreference`.

## The schema was never versioned (second report, same button)

The fix above did not work on the user's machine, and the reason is worse than
the first diagnosis: **no build of this app has ever written `PRAGMA
user_version`.** The generated `create` does not do it. So every database in
existence reports version 0, `openOrMigrate` read that as "empty file", skipped
the migration, and every query then failed - `no such column: capture.category`,
`table trip has no column named place`. The migration test passed because it
built a "version 1" fixture that no build ever produced.

What changed:

- The baseline schema is captured verbatim in `tools/storecheck/baseline-v1.sql`,
  from commit 81df38b, and is one of the three shapes the store gate opens.
- `openOrMigrate` decides from the file - does a `trip` table exist - not from
  the pragma, and writes the version after `create` as well as after `migrate`.
- A failed `migrate` is retried by column (`reconcile`), so a half-upgraded file
  cannot be made worse.
- `AppModel.start()` guards every step separately. `outbox.resume()` used to run
  before the route was chosen, so one exception left the app on its default
  screen with no trip state - which is the "empty landing page" that was reported.
- "Begin empty" routed to a page that rendered the same form behind it; it opens
  a blank form now.

Still not verified here: the fix on the user's actual machine, and the Android
camera. Both need hardware this sandbox does not have.
