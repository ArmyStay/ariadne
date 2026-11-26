# Phase 5 — the shutter, in place

## Why this phase exists

You tested the app and could not feel a change in the flow. You were right, and the
reason is in the shape of the work rather than in its quality: Phases 2 and 3 rebuilt
the **shell** and the **four destinations** — the bar, the back stack, Today, Days,
Trip, Stats, Preferences — and touched the loop you actually walk through exactly
once, at the end of it, with the capture sheet.

So a test run went: shutter → the old viewfinder page → the new sheet → save → the old
day record → the old close-day pager → the old card. Five of those six screens were
untouched, and one tap had moved. Of course it felt the same.

This phase changes the thing you said was missing: **the shutter**.

## What changed

The shutter used to be a **route**. Tapping it navigated to a viewfinder page, and the
photograph then arrived on a second page. Three screens to keep one receipt — two of
which were the same nothing — and the screen you actually wanted back had to be
rebuilt when you returned, because the route had replaced it.

The shutter is now a **mode over the page you are on**:

- Tap the shutter. The camera rises over Today (or Days, or the day's record). **The
  route does not change.** The bar is still underneath; the tab is still the tab.
- Take the frame. The camera closes and **the sheet opens over the same page** — the
  photograph, the keypad, the currency and category chips.
- Save, or "add the amount later". The sheet closes. You are **exactly where you
  stood**: same page, same scroll, with the day's figure already moved and the waiting
  count updated.

Zero navigations for the whole loop. That is the difference between a shutter that is
part of the page and a shutter that is a page.

### In the model

Two pieces of state, not two routes:

```kotlin
public val shutterOpen: Boolean = false
public val sheetCaptureId: String? = null
```

- `openCapture()` sets `shutterOpen` instead of `go(Route.Capture)`. The old page is
  still reachable — and still the right answer — when there is **no trip, or a closed
  one**, because a photograph with nowhere to belong should be explained rather than
  taken.
- `capturePhoto()` closes the camera and opens the sheet, leaving `route` untouched.
- `keepFrameForLater()` and `saveDetail()` close the sheet in place. Saving from the
  full detail page still leaves for the day, because that page *was* a destination.
- `back()` closes whatever is open over the page **before** it moves between pages, and
  `canGoBack` is true while an overlay is open — so the system gesture and the
  predictive-back animation both do the right thing with a camera on screen.
- `go()` puts both overlays away: an overlay belongs to the page it was raised from,
  not to the app.

### In the UI

`CaptureOverlay` (`ui/app/.../pages/CaptureOverlay.kt`) is the camera as a cover over
the current page: close, the live plate (or the ruled plate when the target has no
sensor), the shutter, the library, the waiting count. It reuses the same
`CameraPlate`, `Shutter` and picker as the old page, so there is one shutter in the
codebase, not two.

`AriadneApp` draws the camera and then the sheet above the page and the bar, and
Escape closes them exactly as Back does.

### In the flow checks

`preview/FlowCheck.kt` now asserts the behaviour you could not feel:

- the shutter opens **without navigating**;
- Back with the camera open closes the camera, not the page;
- a capture keeps the page it was taken from, and the frame becomes the sheet over it;
- "add the amount later" closes the sheet and leaves the person where they were;
- the photograph is still in the record after all of it.

## What is still untouched

The rest of the loop, in the order a person meets it:

1. **The day record** (`RecordPage`) — the screen you land on from the Days tab.
2. **The close-day pager** (`CloseDayPage`) — renamed and given a summary, but still
   the old layout.
3. **The card** (`TripCardPage`) and **the first run / new-trip form**.
4. **Detail** (`DetailPage`) — the full page, now only reached deliberately.

Each of those is a page of the old build with a new coat of paint in places. If the
next test still feels like the old app, that is where it will be coming from.
