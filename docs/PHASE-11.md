# Phase 11 — a photograph is the note

## What this is

You ran the build and sent three frames back. Two of them were the kept photograph
with its headline cut off behind its own buttons, and both shutter labels ending in an
ellipsis. The third was the shutter working.

That is fixed here — and under it, the thing you actually described: the app exists so
that a photograph can stand in for a note, so the ask had better be one gesture. With
the glasses paired it is: look, press, and the day has the picture whether or not the
phone left the pocket. And with **SAY THE AMOUNT** the figure can come out loud, at the
stall, instead of at Close Day.

## The two frames you sent back

**The kept photograph.** It was a scrolling column, so a photograph tall enough to be
worth looking at pushed the headline under the pinned answers — a cut word, which is
the worst thing a page about a kept thing can do. Nothing scrolls now: the card is
given the height left between the head and the answers, and the photograph inside it
takes what the card's own head leaves. The picture is still the largest thing on the
page, and the amount can no longer hide behind a button.

**The shutter.** `FROM THE LIBRA…` and `4 WAITING ACRO…` are `LIBRARY` and `N waiting`.
The reference truncates both, but a bar that ends in an ellipsis reads as a bug.

## The glasses, as one mark

The capsule is gone. In its corner of the header there is now a 38dp mark: two rims in
`inkSoft`, or in `blue` on a `blueWash` disc once paired. Its spoken label is the state
itself — *"Meta AI glasses: Connected."*

Tapping it opens the one setup: a card that says whether they are paired, what that
means for the shutter, the platform's raw state word (`Not set up`, `Pairing in Meta
AI`, `Connecting`, `Connected`, `Bluetooth permission needed`, `No glasses answered`),
and **CONNECT** — or **DISCONNECT** — plus **DONE**.

And then the shutter does the thing you asked for: when the glasses are paired,
`wearable.capture()` takes the picture and it is filed on the day it was taken. The
fallbacks stay, in order: the glasses, the phone's sensor, a ruled plate.

## The amount, said out loud

**SAY THE AMOUNT**, on the kept photograph, while the entry is still open. The platform
recogniser opens; the transcript goes to `SpokenExpense.parse`; the figure is filed and
the entry lands **confirmed**, because a spoken amount is a decision rather than a
reading.

`SpokenExpense` is a pure function in `core/model` with eight tests, and it reads:

| said | filed |
| --- | --- |
| `12.50` | 1250 |
| `twelve fifty` | 1250 |
| `five euros fifty` | 550, EUR |
| `twenty three` | 2300 (not twenty and three cents) |
| `one hundred twenty three` | 12300 |
| `coffee twelve fifty` | 1250, merchant `coffee` |
| `450 rupees` | 45000, INR |

A sentence it cannot read is not a figure: the entry stays open and nothing is
guessed. Deliberately not clever — a misheard amount is one tap from fixed in the
editor, which costs less than a parser that pretends to understand everything.

## The microphone, made a button; the aeroplane, made visible

The first build on the phone came back with two more things. *Log with voice* should be
a microphone, not a line of type — and the home page should show the aeroplane.

- **`MicMark`** draws the shape: capsule, cradle, stem, the line it stands on, at one
  weight in ink. Nothing animates.
- **`MicAction`** is the kept photograph's row, in place of the old underlined
  `Say the amount`: that mark beside the words in a hairline capsule, blue-washed and
  reading `Listening…` while the recogniser is open, `No microphone in this build`
  where there is none.
- **`MicDisc`** is the home page's: a 52 dp disc with the same rim, beside **Add an
  expense manually**. It opens `HomeVoicePanel` over the page (a new `overlay` slot on
  `TabPage`, so the sheet covers the page and not the four destinations). The sheet
  listens on open, files a parsed sentence against today through `saveManualEntry` and
  closes; a sentence with no figure reads *"No amount in that. Try again, or type it."*
  and **Type it instead** is the way to the keypad.
- **The hero artwork** comes down (offset −155 → −96 dp, −120 → −100 wide) and the paper
  wash now ends by 84 % of the width instead of veiling the whole of it. The aeroplane
  was never outside the crop: it was inside the band the hero showed and under about
  36 % paper at that x. Both changed, so it reads.

## What was broken under all of it

The Android build had been red since the wearable commit, and none of the UI you asked
for could ship on top of a build that does not exist. Three causes, each fixed here:

1. **The dependency coordinates never existed.** `com.meta.wearable.mwdat:mwdat:1.0.0`
   and `…:mockdevicekit:1.0.0` are 404 on Maven Central. The published artifacts are
   `com.meta.wearable:mwdat-core`, `:mwdat-camera` and `:mwdat-mockdevice`, and they go
   up to 1.1.0.
2. **The SDK requires `minSdk 29`** and the app was on 26, so the manifest merger
   refused the library outright. The Android floor is 29 now — the price of the
   glasses.
3. **`MainActivity` reads `BuildConfig.DEBUG`** and AGP 8 stops generating that class
   unless `buildFeatures { buildConfig = true }` asks for it. That error had never been
   reached, because the build never got that far.

`assembleDebug` is green for the first time since that commit, and the `shared` CI job
with it. The manifest also declares `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`, carries the
`CLIENT_TOKEN` meta-data the SDK reads, and defaults both the app id and the token to
`0` — the documented developer-mode pair — with a note that a release build overrides
them from the Wearables Developer Center.

## Verified, and not

- **Compiled on every target**, by CI, on this branch's head:
  `:app:android:assembleDebug` **0**, `:app:wasm:compileKotlinWasmJs` **0**,
  `:app:desktop:compileKotlinJvm` **0**, `./gradlew jvmTest` **0**.
- `SpokenExpense` passes its eight tests in the local harness (`passed=8 failed=0`).
- **CI is green on all three jobs** — `shared` (jvm + wasm + android), `desktop`, and
  `ios` — for the first time since the wearable commit. The `ios` job had been red on
  every commit for a different reason, found by asking the macOS runner to print the
  compiler's errors: the export code used `NSData.create` and `NSData.writeToFile`,
  which do not resolve in this module's interop, and read `view.bounds.size.width`
  through the cascade. It now writes the file with `fopen`/`fwrite`, centres the share
  sheet with `CGRectGetMidX`/`CGRectGetMidY`, and names `VNRecognizedText` before
  reading it — the Vision bridge hands the candidates back as `Any`.
- **`dependency review` passes but reviews nothing:** the repository has its dependency
  graph switched off, which the action reports as "not supported on this repository".
  The workflow now asks the graph first and skips the review when there is none, with a
  warning that says where the setting is. Turn it on (Settings → Code security →
  Dependency graph) and the check starts doing its job again.
- **`wasm demo → GitHub Pages` is skipped on pull requests by design** — it is gated on
  `github.ref == 'refs/heads/main'`.
- **Not run on a phone by me, and not run against glasses at all.** The DAT calls are
  the documented ones in the documented order, and they compile against the published
  SDK 1.1.0 — but nothing here has watched a pair of Ray-Bans answer. The mock device
  (`mock_glasses` intent extra, debug builds) exists for exactly that.
- **Not rendered.** The card-instead-of-scroll change and the corner mark are reasoned
  and compiled, not looked at. The frames you sent are the only evidence of this
  interface that exists.
