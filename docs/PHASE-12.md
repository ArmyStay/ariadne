# Phase 12 — the icon, and the aeroplane where you can see it

## What this is

Two things from the phone: the home page's artwork was still sitting too high to be read
as a picture, and the app had no icon of its own — the launcher showed a vector loop
drawn in code.

## The home artwork, measured

The hero is 190 dp tall (218 wide). The artwork was 350 dp wide, which at the wallpaper's
0.533 ratio is a **656 dp-tall image** — so the hero was ever only showing a 190 dp band
out of 656, and where that band fell was decided by one offset.

The subject sits in the artwork's upper third: the spark at about 30 % of its height, the
aeroplane at 33 %, the globe's limb from 35 % down. A band 29 % tall can hold all three,
but only if the artwork is small enough for the three to be close together on screen.

- Width: **350 → 310 dp** (380 → 350 wide).
- Offset: **−96 → −56 dp** (−100 → −64 wide) — moved *down*, which is what was asked.

At 310 dp the aeroplane lands at 194 dp from the artwork's top; with a 56 dp offset it
sits at 138 dp — about three-quarters down the hero, right of the trip's name, with the
spark above it and the limb entering from the right. Nothing is clipped: the plane's
whole silhouette is inside the band.

The paper wash is unchanged from the last revision (`0 → 0.99`, `0.45 → 0.94`,
`0.66 → 0.28`, `0.84 → transparent`), so the name still reads on white.

## The icon

The artwork supplied was a 1024 × 1024 render — a rounded square of cream paper with the
wordmark, the spark, and the aeroplane crossing the globe, dropped on a grey field with a
shadow.

- Cropped **inside** the shadow (772 dp square at +124 +126 of the source) so the
  launcher icon is the paper itself, not a drop shadow in a box.
- `mipmap-mdpi … xxxhdpi`: `ic_launcher.png` and `ic_launcher_round.png` at 48, 72, 96,
  144, 192 px.
- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: an adaptive icon —
  background `@color/iconBackground` (`#E3E1D4`, sampled from the artwork), foreground
  `@mipmap/ic_launcher_foreground`, which is the artwork at 78 % of the 108 dp canvas
  with rounded corners, so that no mask — circle, squircle, teardrop — can crop the
  wordmark.
- `AndroidManifest.xml`: `android:icon` and `android:roundIcon` now point at the mipmap.
- `Theme.App.Starting`: `windowSplashScreenAnimatedIcon` is the same artwork.
- `docs/brand/ariadne-icon.png` (512 px) is the copy in this repository, and the README
  opens with it.

## The dock's shutter, and the loading page

Two more frames, both from the same design sheet.

**The shutter is navy.** The dock's circle was `paper.blue` (`#2563EB`) with the
aperture glyph — a scanner, not the app's own mark. It is now `paper.navy` (`#1C3155`,
sampled from the supplied dock artwork) with `SparkGlyph`: a four-point spark with
concave sides and a small diamond cut out of its centre in the circle's own colour. The
closed-trip state still falls back to `paper.edge`. `ApertureGlyph` is gone with it.

**The loading page is a cover.** `StartupPage` was three lines centred on blank paper
(a 34sp wordmark, a hairline rule, a 9sp label). It now carries the design sheet's
composition:

- `Ariadne` at 62sp, a 62×2dp blue rule, **OPENING YOUR RECORD** at 9sp/2.6sp tracking;
- the serif italic line *“Every journey / leaves a trace”*;
- the globe-and-flight artwork bleeding off the bottom-right (94 % width, offset
  `+30dp / +70dp`, 94 % opacity);
- at the foot a 150×3dp track with a navy segment sweeping along it, and
  **PREPARING YOUR JOURNEY** beneath it. The bar sweeps rather than filling, because
  the app cannot measure that wait and will not pretend to.

## Verified, and not

- **Compiled:** the `four targets` workflow — `shared` (JVM tests, wasm, android
  `assembleDebug`), `desktop`, `ios`. The android job is the one that proves the three
  mipmap sets, the adaptive XML and the manifest reference are all valid resources.
- **Rendered here:** the hero band was reconstructed from the wallpaper's own pixels at
  the new size and offset (see the arithmetic above), and the launcher icon was assembled
  and inspected as a square and through a circular mask. That is a measurement, not a
  phone.
- **Never rendered:** the icon on a real launcher, the splash on a real Android 12+
  device, and the hero on your phone. Those are yours to check.
