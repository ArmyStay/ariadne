# What is left for a human

Everything here needs a device, a browser, an account, or a decision — the four things a
sandbox does not have. Nothing here is code. Each item says what to do, why it matters,
and what "done" looks like.

Ordered by what a judge meets first.

---

## 1. Check the demo, logged out (2 minutes)

**Where:** <https://armystay.github.io/ariadne/> in a private window.

**Already done:** the Pages source is **GitHub Actions** (it was the blocker in earlier
guides), and the deploy no longer guesses where Gradle put the page — `pages.yml` finds
`index.html`, serves the site locally under `/ariadne/` before uploading it, and then a
`verify` job fetches the public URL with no credentials and asserts a 200 whose HTML
carries `ariadne.js`. A green deploy now means the link works, not that an upload
finished.

**Why you and not CI:** a job proves the bytes are served; it cannot see the first frame.

**Done when:** the URL opens on the first instalment — `01 — THE FIRST INSTALMENT`,
*A trip, kept in photographs.*, three beats, one blue **BEGIN A TRIP** — and
[`?seed=demo`](https://armystay.github.io/ariadne/?seed=demo) opens on a trip that is
already kept. If either 404s, the `verify` job on the last `main` run is the first place
to look: it prints the status code it got.

---

## 2. The About box (2 minutes)

**Where:** repository home → the gear beside *About*.

The API refuses this from the integration (`403 Resource not accessible by integration`),
so it has to be typed. Copy-paste:

- **Description:** `A photo-first trip ledger in Kotlin Multiplatform: one press of the
  shutter, the figures at Close Day, the record kept on the device. Android, iOS, desktop
  and the browser from one Compose UI.`
- **Website:** `https://armystay.github.io/ariadne/`
- **Topics:** `kotlin-multiplatform`, `compose-multiplatform`, `android`, `ios`, `wasm`,
  `expense-tracker`, `travel`

**Why:** the current description ends "capture now, tally at nightfall" — the word
"nightfall" appears nowhere in the app, and the About box is the first sentence anyone
reads about the project.

---

## 3. Film the loop (30 minutes, the highest-value half hour here)

**What:** a phone screen recording, 45–60 seconds, no music, no title cards:

1. Home with a trip open;
2. the shutter, one photograph taken;
3. the frame that comes back — amount typed, kept;
4. **Close day** and the day's figure moving;
5. the record for that day;
6. the trip card.

**Why:** it is the one artefact that proves the ritual is as fast as the README says. The
stills prove the layout; only a recording proves the loop.

**Done when:** the file is in `docs/shots/` and the README embeds it above the two stills.
(`tools/record-phone-video.sh` automates the adb side of the capture.)

---

## 4. Two stills, navy dock (10 minutes)

`docs/shots/current/01-home-phone.png` and `02-record-phone.png` were taken before the
dock's shutter changed to `paper.navy` (`#1C3155`). The README caption now admits it ("the
dock's shutter has since gone navy"), which is honest but weaker than a still that is
right.

**Done when:** both stills are re-shot on a current build and the caption's admission is
deleted rather than repeated.

---

## 5. Install the APK and tell me what breaks (15 minutes)

**Where:** <https://github.com/ArmyStay/ariadne/releases> — `ariadne.apk` on
`v1.0.0-rc1`. It is a debug build, signed with the debug key, about 120 MB (five bundled
OCR models):

```bash
adb install -r ariadne.apk
```

**What to look at, in this order:**

1. The first page — three beats, one blue button. Is it crisp, or is the imprint crowded?
2. **Begin a trip** → name, dates → the ledger Home.
3. The shutter → a real receipt → the amount the recogniser read.
4. **Close day** → the day's figure.
5. Trip page → **the codes** → copy the trip code, paste it into a note to check it is the
   sixteen characters the page claims.
6. Settings → **the evening** → the reminder row → pick the next hour, **SET THE
   REMINDER**, allow notifications when Android asks. Then close the app from the
   switcher and wait for that hour: the notification should arrive with the app closed,
   and tapping it should open Ariadne. If nothing arrives, say so — the fallback line
   inside the app is the only thing tested so far, and the alarm path has never run on a
   device.

**Why:** none of this has been seen on a screen. CI compiles; it does not look.

---

## 6. A second phone, one ledger (20 minutes, two devices)

This is Phase 3's claim and it needs two phones.

1. Phone A: open a trip → **the codes** → **Copy the trip code**.
2. Phone B: first page → **Somebody gave you a code?** → paste → the readback should name
   the same dates and the same budget → name it → **Join this trip**.
3. Phone A: photograph something, confirm a figure, **Close day**.
4. Phone A: **the codes** → **Copy the day**.
5. Phone B: **the codes** → paste the line → **Add their figures** → open the day's record:
   the traveller's name and figure sit under **Tallied**, with **With them** below.
6. Paste the same line again: it must be refused ("That ledger is already on this trip.").

**Done when:** both phones show the same plan and the same combined day figure, with no
network between them.

---

## 7. Accessibility at 200% (20 minutes)

**What:** set the system font size to 200% and open, in order: Home, the record for a day,
the shutter, the codes page. Photograph anything that clips, overlaps or scrolls sideways.

**Why:** the design claims a reading typography and no measured proof exists. Three stills
at 200% would be the proof.

---

## 8. iOS: the decision, not the work

The README and `docs/UI-AND-FLOW.md` now say what is true: **iOS is the ledger, search and
the trip card — no camera, no photo import.** No `PlatformPhotos` bridge exists, so the
shutter has no sensor and the picker has no library.

The work, if you want parity, is one `PHPickerViewController` bridge in
`app/ios/src/iosMain/.../MainViewController.kt` plus the `NSPhotoLibraryUsageDescription`
Info.plist key — a day, most of it in Swift/Xcode rather than in this repository.

**Done when:** either the bridge exists and the target table says "camera and library", or
the table stays as it is now and nobody has to guess.

---

## 9. Hardware: glasses and voice

- **Glasses.** The mock path is compiled and tested (`--ez mock_glasses true`); no real
  Meta Connect device has ever been in this loop. Pairing needs the developer switch in
  Meta AI and the app id/token from the Wearables Developer Center
  (`manifestPlaceholders` in `app/android/build.gradle.kts` are `0`/`0` today).
- **Voice.** Needs a device with an `android.speech.RecognitionService`. In an emulator
  without one, the microphone correctly says so rather than pretending.

---

## 10. Merge, then tag

The PRs are yours to merge — that has been the rule throughout.

After merge, if you want a real release rather than a release candidate:

```bash
git checkout main && git pull
git tag -a v1.0.0 -m "Ariadne 1.0"
git push origin v1.0.0
```

`release.yml` builds and attaches `ariadne.apk` and `ariadne-web.zip` from the tag, with
notes that say the APK is a debug build.

---

## 11. The essay

`ESSAY.md` is a draft at 312 words in your voice and about your reasons. It should ship in
your words or not at all — that was the rule from the start, and nothing in this pass
changed it.
