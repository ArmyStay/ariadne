# Testing Ariadne

Three ways, ordered by how little you have to install. None of them need an
Apple developer account, an emulator, or a network connection.

> **Workspace note:** Android OCR is now bound to on-device ML Kit in source, extraction refreshes UI state after settling, and Nightfall can batch-confirm ready captures. These changes were not device/Gradle-verified in this session; run real-target checks on your machine and record outcomes.

---

## 1. Look at it (10 seconds)

```bash
open docs/shots/contact-sheet.png
```

Fourteen frames, rendered from the **real** shared Compose UI — not a mockup, not
a Figma export. `docs/shots/` holds each page at full size, including the desktop
spread and the night paper.

This exists because "it compiles" is not the same as "it looks right", and the
only honest way to check a layout is to see the frame.

---

## 2. Run it and touch it (2 minutes)

```bash
./tools/run-preview.sh            # 1180×820 desktop window
./tools/run-preview.sh --phone    # 420×860, the phone layout
./tools/run-preview.sh --night    # the night paper
```

This compiles the shared `ui/` module with `kotlinc` and launches it in a real
window — no Gradle, no Android Studio, no emulator. What opens is the same
`AriadneApp` composable that Android, iOS, desktop and Wasm all call, wired to an
in-memory store and a preview recognizer that returns OcrResult.Failure (a designed fallback path: the capture stays queued and the amount field stays editable). Android app builds are wired to on-device ML Kit OCR in source.

**What to try, in order** — this is the whole product in about ninety seconds:

1. **Begin with the demo roll.** You land on Home with five photographs waiting,
   the shutter in the notch and four destinations under it.
2. **Finish 5 photographs** (the blue block) → Nightfall. The tea glass fills the
   plate; the merchant and the amount were "read from the photograph". Type over
   the number. Both decisions — **Confirm** and **No amount** — are on screen
   above the keypad, so nothing needs a scroll.
3. **No amount** on the next card — the record keeps things you didn't price.
4. Keep confirming; the last card's second button becomes **Close the day**. The
   day locks and the total appears on Home and under Stats.
5. **Timeline → a day** opens the record: the five lines are the day you just
   finished, and the figures match what you confirmed.
6. **Prefs → Close the trip into a card** → the card page.

Keyboard on desktop, at Nightfall only: `0–9` and `.` type, `Backspace` deletes,
`Enter` confirms, `J`/`K` move through the stack, `Esc` leaves.

> The window is real AWT. On a headless box you need `Xvfb`:
> `xvfb-run -s "-screen 0 1400x1000x24" ./tools/run-preview.sh`

To regenerate the stills after a UI change:

```bash
./tools/run-preview.sh --shots docs/shots
```

This renders through `ImageComposeScene` — Skia straight to PNG, no window and no
display at all. It is also the fastest way to see a layout change.

---

## 3. Check the logic (30 seconds)

```bash
./tools/test-domain.sh
```

`OK (70 tests)` — money never a float, `HALF_UP` only at the FX boundary, the
retry backoff surviving a restart, JPY at scale 0, the parser's confidence gate,
search ranking, and the queue state machine. Pure Kotlin, no Gradle, no device.

---

## 4. Check the store against real SQLite (30 seconds)

```bash
./tools/store-check.sh
```

Generates the SQLDelight interface from `Ariadne.sq`, compiles `AriadneStore`
against it, and drives it over an actual SQLite file — 25 checks: money stays in
minor units beside its currency code, the retry backoff hides and reveals
captures, the day closes, the file is written. The last group covers the detail
page and the breakdown: a category is written and read back, a capture with no
category is legal, totals are converted to base currency through the same FX table
the ledger uses, an unfinished capture is *not* yet a figure, and a currency with
no stored rate reports its raw amount rather than a zero.

```bash
./tools/entry-check.sh
```

Compiles all four platform entry points (Android, iOS, desktop, Wasm) against the
real schema and small stubs for the platform APIs. It cannot prove Gradle resolves
the Android SDK, but it does prove the Ariadne code in each entry point type-checks.

## 5. The real targets

The preview proves the UI and the Gradle-free scripts prove domain/store/flow behavior in this checkout's harnesses. Full platform builds, device behavior, and Pages deployment still need to be run and recorded on your machine/CI.

```bash
# Shared tests
./gradlew jvmTest

# Android debug APK (includes CameraX + ML Kit recognizer wiring)
./gradlew :app:android:assembleDebug

# Desktop compile/run
./gradlew :app:desktop:compileKotlinJvm
./gradlew :app:desktop:run

# Wasm dev server and production distribution (for Pages artifact)
./gradlew :app:wasm:wasmJsBrowserDevelopmentRun
./gradlew :app:wasm:wasmJsBrowserDistribution

# iOS frameworks (on macOS with Xcode)
./gradlew :app:ios:compileKotlinIosSimulatorArm64 :app:ios:compileKotlinIosArm64 :app:ios:compileKotlinIosX64
```

Human/device verification still required:
- Install and run Android on a real device/emulator; verify shutter ? capture ? OCR extraction/fallback ? Nightfall ? card flow and record ~30s footage.
- Confirm GitHub Pages is enabled and the deployed Wasm URL works, then add it to the top of `README.md`.
- If you run Gradle/CI successfully, update `BUILD-STATUS.md` and this file with the exact commands, dates, and outcomes.

## What is deliberately unfinished

| Gap | Where | Consequence today |
| --- | --- | --- |
| No OCR binding on any target | `app/*/…Services.kt` | Every capture falls back to a typed amount. The whole flow still works. |
| ~~`AriadneStore` never compiled~~ | — | **Done:** `tools/store-check.sh` generates and tests it. |
| Wasm store is in-memory | `app/wasm` | A reload loses the trip. IndexedDB lands with the Pages deploy. |
| No photo picker on desktop | `MediaPicker` | "From photos" jumps to the record. |
| Home's "Start a trip" is unbound | `AppModel.startEmptyTrip` | Creates a trip with no demo roll, which is correct, but there is no date picker yet. |
| iOS untested | `app/ios` | Written, not linked. No Mac here. |

The design decisions behind all of this are in `docs/DESIGN.md`.
