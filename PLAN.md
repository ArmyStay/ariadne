# A judge reads Ariadne

Written as if I were on the reviewing side of a Kotlin Multiplatform contest and had your
submission in front of me for twenty minutes: what I would think, what I would check,
what I would call out, and the three phases I would spend the remaining time on.

This is an assessment, not a changelog. Every claim below was checked against the
repository as it stands at `fdd90c7`.

---

## 1. The verdict

**On the work: a finalist. On the submission as it is right now: not yet — and for boring
reasons, not for lack of craft.**

The product has a point of view, the code is real, the rule about days has tests, and the
docs are better than most professional projects. What it does not have, today, is a
submission a judge can *touch*: the demo link serves the README instead of the app, there
is no build to download, and about half a dozen sentences in the docs say things the code
does not do. In a contest where a reviewer opens fifteen entries, those three facts decide
whether the good parts are ever seen.

If the score sheet is the usual one, here is how I would fill it in:

| What I would score | Now | Available | Why |
| --- | --- | --- | --- |
| **Does it run when I click?** | **2/10** | 10 | `armystay.github.io/ariadne` is a Jekyll render of `README.md`; Pages is still on `build_type: legacy`, source `main:/`. Nothing in that page is an app. |
| **Can I install it?** | **1/10** | 9 | no releases, no tags, no APK asset. Building it needs JDK 21, AGP 9.5, SDK 37. |
| **Product idea** | **9/10** | 9 | photograph first, figure later; *no amount* is a legal entry. That is a real insight and the essay argues it well. |
| **Craft in the code** | **8/10** | 9 | 8 domain modules with tests, a state machine, money as `Long`, a design system as code, `RecordDates` with ten tests. |
| **KMP depth** | **7/10** | 9 | four real targets and a fifth that compiles; the shared UI is genuinely shared; the platform edges are `CompositionLocal`s rather than `expect/actual` sprawl. |
| **Design** | **8/10** | 9 | `docs/DESIGN.md` is the best artefact in the repo; the stills are real; the shots predate the navy dock. |
| **Truthfulness of the docs** | **5/10** | 10 | six specific claims are false (below). Ironically this is your own stated rule, and the file that states it is where two of them live. |
| **Originality of the demo** | **5/10** | 9 | the glasses path is the memorable thing and it has never been filmed; the browser demo has no camera, no OCR, and until this week no file input. |

Total, roughly: **45/80 today, 73/80 achievable in a week of the right work.** Nothing on
the "achievable" column is research; it is deployment, an APK, honesty, and one feature.

---

## 2. The first three minutes, as a judge actually spends them

1. **I open the repo.** Description good, no homepage, no topics, no releases, no stars.
   README is strong: logo, a sentence I believe, a demo link, two stills, a promise
   table. I click the demo link.
2. **I land on a rendered README.** There is no app. I have already formed the sentence
   "the demo does not work" and it colours everything after it. *(This is fixable from the
   repository's Settings, and it is the single highest-value action in this document.)*
3. **I check whether I can install it instead.** Releases: empty. I am not going to
   install JDK 21 and the Android SDK 37 toolchain to look at a spending app.
4. **I skim the code.** This is where you win me back: `core/model`, `core/queue`,
   `core/fx`, `core/extract` with tests; `AppModel` is 2,000 lines but it is a real state
   model; the UI is one shared Compose tree; the design tokens are a file, not a Figma
   link. I notice `git log` is clean and the docs are unusually good.
5. **I read `docs/UI-AND-FLOW.md`** — screen by screen, with the real strings. Very few
   entries have this. Then I hit §9: *"Not implemented anywhere in the UI: … voice
   capture"*, which contradicts §5.3. Now I distrust the whole document, and it was doing
   the heaviest lifting in the submission.
6. **I try to find the parts you claim are hard.** Glasses: compiles, mock device, never
   filmed. iOS: compiles, and cannot take, keep or read a photograph. Browser: no camera,
   no OCR, and a file input added today that nobody has clicked.

That is the whole review. The gap between (4) and (2) is the submission.

---

## 3. What is broken, dummy, or claiming more than it does

Severity here means "costs you the judge's trust", not "code quality".

### P0 — breaks the submission before anyone reads the code

| # | Finding | Evidence |
| --- | --- | --- |
| 1 | **The demo link does not open the app.** Pages is `build_type: legacy`, source `main:/`, last build from `7b9345a`. `.github/workflows/pages.yml` exists but has never run, because it only triggers on `push: main`. A judge sees the README rendered as a web page. | `gh api repos/ArmyStay/ariadne/pages`; `pages.yml` `on:` block |
| 2 | **There is nothing to install.** No releases, no tags, no APK. The only path to the phone is a full toolchain. | `gh release list` → empty; `gh api .../tags` → empty |
| 3 | **Even once deployed, the first screen is the onboarding form.** `?seed=demo` is described in a comment — *"the Wasm demo page uses it to open on the demo roll when the URL carries `?seed=demo`"* — but no platform ever calls `onStarted()`, and `app/wasm/.../Main.kt` reads no query string. So the judge's first frame is "create a trip", not the populated app in the stills. | `AppModel.kt:293`, `onStarted` unused outside `AppModel`; `app/wasm/.../Main.kt` |

### P1 — claims the code does not honour (these are cheap and urgent)

| # | Claim | Reality |
| --- | --- | --- |
| 4 | `docs/UI-AND-FLOW.md:970` — Wasm OCR has an "optional `window.ariadneRecognize` hook, else a polite failure" | **no such hook exists** anywhere in the repo. `WasmBrowserRecognizer` returns `OcrResult.Failure("browser OCR is disabled…")` and nothing else. `app/wasm/.../Main.kt:25` promises *"the optional browser recognition hook below"* and there is nothing below. |
| 5 | `README.md:189` — "The `TripShare` route exists in the router" | **there is no `TripShare` route.** `Route` has 21 members and none is that. |
| 6 | `docs/UI-AND-FLOW.md:992` — "Not implemented anywhere in the UI: … **voice capture**" | voice capture *is* implemented and documented by the same file in §5.3 and §5.7 (`SpokenExpense`, `HomeVoicePanel`, the mic on the kept photograph). |
| 7 | `docs/UI-AND-FLOW.md:959` and §11 — "Nothing below has been seen on a screen", "iOS compile not run", "`FlowCheck` … never executed", "`docs/shots/` holds only the pre-redesign archive" | the phone stills exist and are in the README, CI compiles iOS on every push, and `tools/flow-check.sh` is a first-class gate. The table is two phases out of date. |
| 8 | `docs/SDK-SETUP.md:94` — "the only permission requested is `INTERNET`" | the manifest also declares `CAMERA`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` and a speech `queries` block. (My sentence; my error.) |
| 9 | `README.md:113` — "Stills from a phone running the 27 November build" | `01-home-phone.png` shows the dock's shutter in **blue**; the code has been `paper.navy` since #21. The caption is true of a build, not of this code. |

### P2 — thin, dummy, or missing

| # | Finding | Why it matters |
| --- | --- | --- |
| 10 | **iOS is a shell.** `:app:ios` has no camera, no photo picker, no `PhotoFiles`, so `Vision` OCR can never be handed a file. "Five targets" is really three and a half. | a KMP judge goes straight for the weakest target |
| 11 | **The shutter on web/desktop "captures" a place** — `ByteArray(0)`, filed as a photograph. It is labelled honestly, but a judge tapping it gets an empty record for their trouble. Now that the browser has a picker, the shutter there should ask for a file instead of inventing one. | it is the most-tapped control in the demo |
| 12 | **You cannot delete a single entry.** Trips yes (two-tap consent), entries no. | first thing anyone tries after a mistyped amount |
| 13 | **Two scope bugs, already documented and left standing**: Stats' "by currency" reads the opened day under a trip total; Home's rail and its badge count different scopes (`docs/UI-AND-FLOW.md` §10 items 2–3). | you found them and didn't fix them; a judge reads that as "ran out of time" |
| 14 | **Repo metadata is bare.** No homepage (should be the demo URL), no topics, and the description still says "tally at nightfall" — a word the product no longer uses. | it is the first text on the page |
| 15 | **The essay is 312 words** under a heading that says 300. | only matters if the rule is exact |

Not broken, and worth saying: no `TODO`s, no dead routes reachable from the UI, relative
links and anchors all resolve (19 checked), and every `tools/*.sh` named in the docs exists.

---

## 4. What would actually wow a judge

Ranked by "expensive to fake, cheap to be impressed by".

1. **Two phones, one ledger, no server.** A trip code that hands a *trip* to a friend, and
   a ledger code that hands back a *day's figures*, so four people at one table can end up
   with one total without an account, a backend or a sync engine. This is the right version
   of "group trip planning" for this app, and §6 plans it.
2. **A 45-second recording of the real loop on a real phone**: receipt in hand → shutter →
   card at Close Day → the day's total moving. You have `tools/record-phone-video.sh` and
   two stills; the README already admits the GIF is two stills, which reads as a confession.
3. **The glasses, filmed against the mock device.** The pitch is "a camera that is not the
   phone". One clip of the shutter firing with the phone face-down wins the memory of every
   judge who watched fifteen entries.
4. **A one-tap install.** A tagged release with `app-debug.apk` attached and an
   `adb install -r ariadne.apk` line in the README.
5. **The rule with tests.** `RecordDates` is the most judge-friendly thing in the repo: a
   product rule, one definition, ten tests, obeyed by every surface. Lead with it.

---

## 5. Phase 1 — Be clickable and truthful (done, `#24`)

**Goal:** a judge with no toolchain can see the app in one click and install it in one
tap, and nothing they read contradicts what they see.

- [x] **Pages deploys the app.** `pages.yml` publishes the wasm distribution as an
      artifact; the deploy job succeeds on `main`. (The Pages *source* switch is a
      repository setting, not a file — see the note at the end of this section.)
- [x] **The landing page is an introduction, not a form.** `OnboardingPage` is now one
      page: what this is in three beats — the shutter, Close Day, the record — one blue
      **Begin a trip**, and a quiet **Or look at a sample trip**. `?seed=demo` in the
      browser skips straight to a kept trip for anyone who wants to be sent a link.
- [x] **A release workflow.** `.github/workflows/release.yml` builds `ariadne.apk` (debug,
      debug-key signed) and `ariadne-web.zip` and attaches both to a tag, so a judge can
      `adb install -r ariadne.apk` instead of installing a toolchain.
- [x] **Repo About:** homepage = the demo URL, topics set, description without the word
      "nightfall".
- [x] **The six claims corrected**: the `window.ariadneRecognize` hook (never built), the
      `TripShare` route (never existed), voice capture in the "not implemented" list, the
      manifest's permission list, §11's verification table, and the stills' caption.
- [x] **`tools/claim-check.sh`**, and it runs in CI: every `Route.*`, `window.*` hook,
      `android.permission.*` and backticked path a document names must exist. It was
      tested against the real defect: pointed at a document naming the share route that
      never existed, it fails — which is the whole point.
- [ ] **Re-shoot the two stills** on this build. The caption now says they predate the
      navy dock, which is honest; a still with the navy dock is better.

**Still yours to click, once:** Settings → Pages → Source = **GitHub Actions**, if the
deployed URL still shows the README. The workflow succeeds either way, but the branch
build wins the race until the source is changed.

**Done when:** a stranger in a browser sees a page that explains the app and one button;
a stranger with a phone installs the APK; the honesty gate is green.

## 6. Phase 2 — Be believable (2–4 days, one PR)

**Goal:** the things the README calls proven are the things a sceptic can check.

- [ ] **Record the loop** (phone, 45–60s, no music): Home → shutter → card → Close Day →
      the day's figure moving → the record. Embed the video, keep the stills below it.
- [ ] **Make the shutter useful where there is no camera**: on Wasm and desktop, tapping
      the plate opens the file chooser instead of filing an empty capture. Same for the
      dock's shutter. Delete the "tap for a plate" path from the demo build only if the
      picker is present.
- [ ] **Delete one entry** (two-tap, like trips), and undo the last edit if it is cheap.
- [ ] **Fix the two scope bugs** in §10, then delete them from that section — a doc of
      known-wrong numbers is a liability once you have time to fix them.
- [ ] **iOS, decided out loud.** Either implement `PhotoFiles` + a `PHPicker` bridge
      (≈ a week, needs an Info.plist key) or change the target table in the README to
      "iOS: ledger, search, trip card; no camera, no photo import". Do not leave it
      implying parity.
- [ ] **Accessibility pass**: Home, Record and the shutter at 200% text; three stills; fix
      whatever clips.
- [ ] **`docs/UI-AND-FLOW.md` §11** becomes a real verification table: what CI proves, what
      a device proved, what a browser proved, with dates and commit hashes.

**Done when:** every item in "What is proven where" has an artefact behind it, and the
only asterisks left are the glasses and speech.

## 7. Phase 3 — Win it (3–5 days, one PR): two phones, one ledger, no server

This is the answer to "group trip planning with a trip code", in the only form that does
not betray the app's thesis. **No accounts, no backend, no sync.** A code is a hand-off:
one person types six characters, another pastes one line, and the arithmetic is the same
on all five targets because the codec is pure Kotlin in `core/model`.

### 7.1 The trip code (a plan, handed over)

- Encode the trip plan — name (≤ 24 bytes), start, end, base currency, optional budget —
  plus a version byte and a 4-character checksum into a **human-typable code**:
  `AR1-K7QM-2X9B` (base32, no vowels, no `0/1/O/I`). ~14 characters, reads aloud over a
  phone, survives being typed by a stranger.
- **`Join a trip` on Onboarding and on the register:** type or paste the code and the same
  trip exists on your phone. Dates, currency, budget — no entries, because those are
  personal until the ledger is handed back.
- Show the code on the trip page as one line with a copy action, and *"this is a plan, not
  a sync — nothing updates on its own"* under it.

### 7.2 The ledger code (figures, handed back)

- After a day is closed, **`Hand over the day`** produces a paste-able code: one line per
  confirmed entry — date, amount in minor units, currency, category, merchant initials, and
  a short hash. No photograph, no note, no path.
- The trip owner pastes it in: **`Add a traveller's ledger` → "Priya"**, and the trip gains
  a member and their figures. The day record shows `Priya ₹4,120` beside your own, the
  card shows the combined total, and Stats gains a per-traveller band.
- Store them as `Traveller(id, name, ledgerEntries)` — a separate table, never merged into
  `Capture`, so your own record stays exactly what you photographed.
- Honest edges, stated in the UI: a code can be handed over twice (dedupe by content hash),
  a code can be edited by hand (checksum catches accident, not fraud), and there is still
  no sync.

### 7.3 Why this and not a backend

A server would be a different product: accounts, invitations, conflicts, privacy policy,
uptime. The code hand-off is *this* product's idea of sharing — the same way the receipt is
photographed instead of typed: take the smallest thing that carries the meaning, and let
the rest wait. It is also the most KMP-flavoured feature you could add: a codec with
property tests, identical on Android, iOS, desktop and browser.

**Done when:** two devices with no network share one trip and one total, and you can film
it in fifteen seconds.

---

## 8. What I would not do before the deadline

- **No accounts, no sync, no backend.** It contradicts the essay and cannot be finished
  well in the time left.
- **No new screens.** The five tabs are right; the demo is not deployed yet.
- **No more prose.** The docs are already better than the demo. Every hour of writing from
  here should be an hour of doing something a judge can click, install, or watch.
- **No Play Store listing.** A tagged APK and a working URL are worth more than a review
  queue.

---

## 9. If I had one sentence to leave you with

You built the thing, the design, the rule and the tests — and then shipped a submission
where the judge cannot open the thing, cannot install it, and is told six things that are
not true. **Fix the three, film the fourth, and the work speaks for itself.**
