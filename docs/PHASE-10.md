# Phase 10 — the shutter, in blue

## What this is

The camera is the app's front door and it looked like a form: white paper, a black
rectangle, four small words. The reference you sent is a travel scanner — white paper
with a wash of blue where the light falls, a route drawn across it, a plate framed with
crop marks, the glasses offered in a capsule, and one blue disc for a shutter.

Most of the structure was already right (that is the previous phase's work): the frame
is the page, the hint sits under it, the shutter is in the middle of the library and
the day's open figure. What was wrong was the *weather* — there was none.

## The backdrop is drawn, not pasted

You asked for a background that is responsive, portrait or landscape. A bitmap cannot
be: the reference's wash is a photograph of watercolour, and one crop gives a phone
the edge of the painting and a wide window the middle of it. So the weather is
`ScannerBackdrop` in `ScannerArt.kt` — every mark placed in fractions of the canvas:

- three soft radial fields of `blueWash` (85 % and down) and one of `blueEdge` at 18 %;
- two *tides* at the foot, cubic curves filled with a vertical gradient that fades
  upward — a coast, not a gradient;
- a dashed arc (5dp on, 7dp off) at 0.50 alpha and a solid faint one at 0.26;
- an outlined globe with a meridian at 0.20, and a two-triangle paper plane at 0.70,
  at 70 % across and 8.5 % down.

Portrait: the same page, taller. Landscape: the fields move to the corners that exist,
the arcs keep their sweep, the tides stay at the foot. No asset, no crop, and nothing
in the APK to carry. The same backdrop sits behind `CapturePage.kt`, so the two
surfaces are one design.

## The page

- **Head** — **CLOSE** on the left, the journey's name on the right under a 44 dp
  hairline (`KOREA`, or `Ariadne` when nothing is named).
- **The frame** — dark, 22 dp corners, with four crop marks 12 dp in from the corners
  (20 dp arms, 2 dp thick) drawn in `paper.raised`, because crop marks are white on a
  plate. Still one tap target: `"Viewfinder. Tap to keep the photograph."`, fired with
  `rowTap` so TalkBack has something to press.
- **The hint** — `Tap the frame to keep it` (or `No camera here — tap the frame for a
  plate`) between two hairlines that run out to the margins.
- **The glasses** — **CONNECT AI GLASSES** in a hairline capsule with a drawn glasses
  mark; **DISCONNECT AI GLASSES** once paired. Shown only where the platform actually
  has a wearable (`LocalWearableCapture`), so no device is offered a control that
  cannot work.
- **The shutter row** — **FROM THE LIBRARY** on the left, the shutter in the middle,
  `nothing waiting across this trip` / `N waiting across this trip` on the right.
- **The foot** — `no figure here · the amount comes after` in a `blueWash` capsule; a
  failed capture prints its line above it, as before.

The arrangement follows the window rather than the `wide` flag: `MainActivity` reports
`wide = false` for every phone, so a phone on its side would have got the tall layout in
390 dp of height and a frame two fingers wide. Anything wider than it is tall now takes
the two-column page — frame on the left, controls on the right — which is what the wash
and the arcs were drawn for. Only a taller-than-wide window stacks, with the frame
taking the height and the controls beneath it.

## The shutter itself

Was a 66 dp ink dot inside a 66 dp hairline ring, with a `pointerInput` for a tap. It
is now an 88 dp paper ring around a 68 dp disc in `paper.blue`, carrying a camera mark
in `paper.onBlue` — the one blue thing on the page, and the only control shaped like a
lens. The tap goes through `rowTap`, so it is a button with a role rather than an
invisible rectangle. `CapturePage.kt` uses the same composable, so it changed too.

## Third-party changes that came with the merge

This branch was 3 commits behind `main` when the shutter was rebuilt, so `main` was
merged first (wearable capture, the DAT dependency, image-security hardening). The
merge was clean; nothing in this phase depends on it beyond the glasses capsule, which
only appears where `LocalWearableCapture` is provided.

## Verified

- **Not rendered.** No JDK, display or Compose jar cache exists in this sandbox, so
  `tools/verify-ui.sh` and `tools/shoot.sh` could not run: the wash, the crop marks, the
  capsule and the page in landscape are written and compiled, not looked at.
- **Compiled on every target** — `jvmTest`, `:app:android:assembleDebug`,
  `:app:wasm:compileKotlinWasmJs` and `:app:wasm:wasmJsBrowserDistribution` were run
  against this branch (see the pull request for the run).
- The `shared` check and the `ios` check are red on `main` before this branch; neither
  is caused by it.

## Deliberately not in the reference

The reference's bottom bar, its floating pill navigation and its soft drop shadows are
not here: the app's bar is flat, its type is the book's own, and nothing in this
interface casts a shadow. The wash is the reference's; the chrome is Ariadne's.
