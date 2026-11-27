# Ariadne — the timeline

The whole history in one file: what was built, when, and the decision behind each step.
This replaces the twelve phase notes, the audit, the brief and the status
files that used to live here. They are not lost — `git log --diff-filter=D --name-only`
finds last month's version of any of them, and each section below says which commit or
pull request it came from.

Companion documents that survive, because they are not history:

| File | What it is |
| --- | --- |
| [`../README.md`](../README.md) | the front door: demo link, stills, targets, promises |
| [`../ESSAY.md`](../ESSAY.md) | the 300-word entry essay |
| [`DESIGN.md`](DESIGN.md) | the design language: palette, type, spacing, motion |
| [`UI-AND-FLOW.md`](UI-AND-FLOW.md) | every screen, with the words it actually prints |
| [`SDK-SETUP.md`](SDK-SETUP.md) | the versions, the SDK path, and what each target needs |
| [`RUN.md`](../RUN.md), [`TESTING.md`](../TESTING.md) | how to run it, and what has been verified |
| [`RECOGNISER-TESTING.md`](RECOGNISER-TESTING.md) | exercising the receipt fixtures |
| `shots/` | stills from a phone, and the two-still loop the README uses |

---

## 24 November 2026 — the repository

| Pull request | What it did |
| --- | --- |
| *(initial commits)* | Kotlin Multiplatform skeleton — `core/model`, `core/store`, `core/extract`, `core/design`, `ui/app`, and the four app targets. Apache-2.0. |
| *(same day)* | `Ink: the reference palette, and the reference's bones` — the first design-system commit: `Bone`, `Paper`, the grid. |
| #1 | Desktop OCR bound to Tess4J; the tab bar as Home/Timeline/Trip/Stats; contrast fixes; the first docs. |
| #2 | Judge-readiness pass: manifest proof of the single `INTERNET` use, semantics, the cross-target test runner, `BUILD-STATUS.md`. |

The shape was set here: money as `Long` minor units, a `QueueState` machine, extraction
that fails into an editable blank rather than an error, and no red anywhere in the
palette.

## 26 November — the redesign, phase by phase

The day the app became the thing in the reference rather than a form with fields. The
audit (`docs/REDESIGN-AUDIT.md`, since removed) named six bugs and the line each lived on;
the phases fixed them in order, then rebuilt the screens the camera touches.

| Phase | Pull request | What shipped |
| --- | --- | --- |
| 1 | #7 | The six bugs fixed at the source; a new flow; the visual system as a system. |
| 2 | #7, #8 | Empty states, a settling bar, a tested budget; the trip page which keeps dates, progress and card together. |
| 3 | #8 | Theme, type scale, components, spacing, motion — `Bone`/`Paper` extended, not replaced. |
| 4 | #8 | The arithmetic, tested: the domain suites, `tools/test-domain.sh`, `flow-check.sh`. |
| — | #3 | Dropped the iOS x64 target; AGP 9 built-in Kotlin; per-target CI. |
| — | #4 | Tiers 2 and 3 ported from the KMP contest starter kit (schema, store, migrations, ASO copy). |
| — | #5 | README restructured to the PeopleInSpace order: identity line, platforms, then proof. |
| — | #6 | Third review: one startup rule, the register-bar contract, the closed-trip matrix, the 44 dp sweep. |
| 5 | #9 | The shutter moved *in place* — no separate camera page; Home opens on the trip. |
| 7 | #10 | The snap sheet became the receipt detail: the pair, the rate, the sections. |
| 8 | #12 | Timeline rebuilt to the journey reference: a globe hung off the corner, a spine of days. |
| — | #11 | `docs/UI-AND-FLOW.md`: every surface described down to the words on the buttons. |

## 27 November — the phone in the loop

The build went onto a real device, and the device found what CI cannot.

| Pull request | What shipped |
| --- | --- |
| #19 | Two screens the camera touches: the kept photograph, and the shutter in blue — the cap/sheet geometry, the receipt card, the crop marks. |
| #20 | *A photograph is the note*: the whole Android build repaired — the published DAT coordinates (`com.meta.wearable:mwdat-*:1.1.0`), `minSdk 29`, `buildConfig` — plus the glasses as one corner mark with a real connect panel, `SpokenExpense` (a pure parser with 8 tests) wired to two microphones, and the iOS job made green for the first time. |
| #21 | The app's icon (the supplied artwork, five densities plus an adaptive foreground), the home hero's artwork moved down to show its aeroplane, the dock's shutter turned navy `#1C3155` with the spark mark, and the loading page rebuilt as a cover. |
| #18, #22 | Dependabot: `datastore-preferences-core` 1.3.0-alpha10 → 1.3.0, CameraX 1.4.2 → 1.7.0. |
| #13–#17 | Closed, not merged: the action majors whose checks fail while every workflow pins SHAs deliberately. |

The decisions of that day are recorded in the code and in `UI-AND-FLOW.md`: the glasses
are a 38 dp mark in a corner and never a pill; a spoken amount is a decision and lands
confirmed; a sentence the parser cannot read is not a figure; and the shutter takes the
best camera that is actually there — glasses, then phone, then a ruled plate.

## 27 November — the entry pass

`#23`, the pull request this file arrives in:

- **the demo is the app**: `pages.yml` deploys `:app:wasm:wasmJsBrowserDistribution` as a
  Pages **artifact** in its own workflow, instead of the branch (Jekyll) deployment that
  was publishing the README as a web page;
- **the README reads as a front door**: logo, one sentence, demo link, stills, the
  five-target table, the promise table, and the prose below;
- **the essay** landed at `ESSAY.md`;
- **the contradictions** went: no "one tab, one stack" beside four pillars, no
  public-share-link claim, `Nightfall` → **Close Day**, "gradle-free" → shell gates and a
  Gradle shipping path;
- **this file**: eighteen documents into one, and the rest of the queue cleared.

## 27 November — the demo, proved

The deploy had been green while the root URL 404'd, which is the worst shape a broken
link can take. The cause was a second workflow: the starter `static.yml` the Pages UI
adds when a repository moves to "GitHub Actions", uploading `path: '.'` — the whole
repository, with no `index.html` in it — and racing `pages.yml` for the same concurrency
group. It is gone. `pages.yml` now finds `index.html` rather than assuming Gradle's
output directory, serves the site under `/ariadne/` with `tools/demo-check.sh` before
uploading it (`ci.yml` runs the same gate on every pull request), and a `verify` job
fetches the public URL with no credentials afterwards. The same pass deleted what read as
template rather than as this repository: the `:shared` boilerplate module
(`com.example.shared`, `Platform.kt`, two example tests), the duplicate font tree under
`core/design/src/commonMain/resources/fonts/` (six copies of the fonts `composeResources`
already ships, plus an unused `InterTight-Variable.ttf`), and a second
`eng.traineddata` that had been committed under `core/extract/bin/`. The two OFL licence
files moved to `core/design/licenses/`, which is the only thing that tree was still
earning.

## 27 November — the rules, and the other way in

`#23`, still open, gained two things that change what the app allows rather than how it
looks.

**A day that has not happened is not a record.** `RecordDates` in `core/model` — four
predicates with tests — and every surface now asks it: the shutter files into today, or
into the day whose record it was opened from, and the day line under the frame says
which (`THIS GOES ON TODAY, 27 NOV`). Tap it and a day sheet lists the trip's days that
have happened, newest first, four to a row; tomorrow is not disabled there, it is
absent. A trip that starts next month has no shutter until it starts, a typed date
later than today is refused in one sentence, and Close Day refuses a day that is still
ahead. This is the first rule in the app that is older than the feature that needed it.

**The library, in the browser.** The demo had a shutter and nothing else: with no camera
behind it, the plate. `BrowserPhotoPicker` fixes the gap the way each other target
already had — an `<input type="file" accept="image/*">`, up to twelve images, each read
as a data URL and rescaled to the same 1600px the Android picker uses, then handed to
the same `importPhotos` path as a camera frame. On iOS there is still no picker, so
**LIBRARY** is not drawn there rather than drawn dead.

## 27 November — the first instalment

`#24`, after the merge of `#23`. The judge's read of the repository (`PLAN.md`) said the
work was a finalist and the submission was not, because a reviewer could not install
anything, could not read a page that was true, and landed on a form.

- **The landing page is now an introduction.** One page that says what the object is
  (three beats: the shutter, Close Day, the record), and one blue thing to press:
  **Begin a trip**. Under it, quietly, **Or look at a sample trip** — the same demo roll,
  as a door instead of a secret. `?seed=demo` on the browser build skips the page for
  anyone who wants to link straight to a kept trip.
- **A release workflow** (`release.yml`) builds `ariadne.apk` — debug, debug-key signed,
  installable with `adb install -r` — and `ariadne-web.zip`, and attaches both to a tag.
  A judge no longer needs JDK 21 and SDK 37 to look at a spending app.
- **The claims were corrected**: no `window.ariadneRecognize` hook (never built), no
  `TripShare` route (never existed), voice capture is implemented (§9 had it in the
  "not implemented" list), the permission paragraph now lists all four permissions, and
  §11's verification table says what CI proves rather than what a Windows machine once
  could not.
- **`tools/claim-check.sh`** runs in CI: every `Route.*`, every `window.*` hook, every
  `android.permission.*` and every backticked path a document names has to exist. It was
  written after finding five such claims in this repository, and it fails on the sixth.

## 27 November — two phones, one ledger

`#24`, the second half of the same push. A judge asked the obvious question of a trip
ledger — *can two people use it?* — and the answer had to be yes without betraying the one
promise the app makes: nothing leaves the device.

- **A trip code.** `AR1-K7QM-2X9B-4HDR-8TQP`: sixteen characters after the prefix, drawn
  from an alphabet with no `0`, `1`, `I` or `O`, carrying the start day (fourteen bits),
  the nights (eight), the base currency (fifteen) and the budget (one flag and
  thirty-two). A checksum catches a mistyped character; the readback says what the code
  means before a trip exists. The trip's *name* is deliberately not in it — it would
  more than double the code and it is the one part with no arithmetic in it.
- **A ledger code.** One line per confirmed figure on a closed day — day, amount, currency,
  category, two initials — with an eight-bit digest per line, so a damaged paste names the
  line instead of quietly changing the total.
- **A traveller is not a second user.** Their rows live in two tables of their own, are
  quoted beside the day's total (`Priya ₹4,120.00`, then `With them`) and are never merged
  into your captures or your card. The page states the three edges: a code can be handed
  over twice (refused), a code can be edited by hand (checksum catches accident, not
  fraud), and there is no sync.
- **Everything is text.** No socket is opened by any of this, and the codec is pure Kotlin
  in `:core:model`, so the browser and the phone produce identical symbols. The browser,
  desktop and iOS builds gained a clipboard for the first time so that a code can leave
  the phone at all.

Also in `#24`: deleting a single photograph (two taps, like a trip), the shutter's frame
opening the file dialog where there is no camera, the rail counting the same list the
dock's badge counts, and `docs/HUMAN-TASKS.md` — the things that need a device, a browser
or a decision rather than a commit.

## Where it stands

**Built**: the shared domain (money, queue, FX, search, store with `openOrMigrate`), the
design system, the four tabs and the stacked pages, CameraX capture with ML Kit text
recognition, extraction and confirmation, Close Day, the trip card, the archive export,
and — on Android — the glasses path over Meta's Wearables Device Access Toolkit.

**Proven**: CI on five targets (shared JVM tests, Android, desktop, iOS, wasm) on every
push; and on a phone, the capture → day → record loop, the date chips, and settings that
survive a restart.

**Not proven, and written down as such**: a screen recording (the README's loop is two
stills); the glasses against real hardware (the code compiles, a build pairs only with
your own app id, and the mock device is how the path is exercised); the Pages deployment
until the repository's Pages source is switched to *GitHub Actions*; and speech, which
needs a device with a recogniser app.

## Decisions that outlived their phase

- **One shutter press, no form.** The camera is never a page of its own; the sheet comes
  up over it.
- **A day that has not happened is not a record.** Not for the shutter, not for a typed
  date, not for Close Day. The list of days stops at today because tomorrow has nothing
  in it yet.
- **No red, ever.** Material's `error` is mapped to indigo; failure is a sentence.
- **The bar is flat**, four labels and a round button in a gap — never the curved bar and
  never the mockup's pill.
- **Settings live on the main page**; the landing page opens to the dashboard.
- **Photographs are the note.** The figure may arrive later, or never: *no amount* is a
  legal entry.
- **Money is a `Long`**, converted once at the boundary, never a float.
- **One outbound request**, optional, and no trip data in it.
- **`minSdk 29`** — moved from 26 for the glasses SDK. That is the whole reason.
- **Documentation quotes the real text** and describes what the code does, not what
  anyone did.
