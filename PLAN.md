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

## 6. Phase 2 — Be believable (done in `#24`, minus what needs a human)

**Goal:** the things the README calls proven are the things a sceptic can check.

- [x] **The shutter where there is no camera.** In the browser and on the desktop the
      frame opens the file dialog, and the dock's round button goes straight to the
      picker; the drawn plate is the fallback for a target with neither.
- [x] **Delete one entry**, two taps like a trip, in the detail page — bytes and row.
- [x] **The scope bug in §10.** The rail now counts `tripPending`, the same list the
      dock's badge counts, and says "across this trip" / "N on this day".
- [x] **The other §10 item was already fixed** when checked: Stats' "by currency, before
      conversion" reads `tripPerCurrency`, which `loadTripDays` fills from the whole trip.
- [x] **iOS, decided out loud.** The README's target table now says what iOS can do —
      the ledger, search, the trip card, the codes; **no camera, no photo import** — and
      names the `PHPicker` bridge as the work that would change it.
- [x] **§11 is a verification table** with CI jobs per claim, plus what is honestly not
      measured (layout, contrast, 200% text).
- [ ] **Film the loop** and **two stills at navy dock** and **200% pass** — a device, a
      hand and an eye; see `docs/HUMAN-TASKS.md` §3, §4, §7.

**Done when:** every item in "What is proven where" has an artefact behind it — the
asterisks that remain are the ones only a camera can remove.

## 7. Phase 3 — Win it (done in `#24`): two phones, one ledger, no server

This was the answer to "group trip planning with a trip code", in the only form that does
not betray the app's thesis. **No accounts, no backend, no sync.** Implemented as:

- **The trip code** — `AR1-K7QM-2X9B-4HDR-8TQP`, sixteen symbols after the prefix: start
  day (14 bits, days since 2020), nights (8), base currency (15, three letters at 5 bits),
  a budget flag and thirty-two bits of budget. A ten-bit checksum over the payload; the
  alphabet drops `0`, `1`, `I` and `O`. The name is not carried — the join page asks for
  it, and `docs/UI-AND-FLOW.md` §5.1b explains why.
- **`Join a trip`** on the first page and a `JoinTrip` page that reads the code back
  before creating anything.
- **The ledger code** (`AR2…`) — one line per confirmed entry: day, amount, currency,
  category, two merchant initials, and an eight-bit digest per line. Pasted, not typed.
- **`HandoffPage`** — the plan going out, each closed day's line, and the pasted line
  coming back, on one page, with `That ledger is already on this trip.` for a second paste.
- **`Traveller` and `TravellerEntry`** in their own tables (`6.sqm`), quoted on the day's
  record as `Priya ₹4,120.00` and `With them`, never merged into a capture or a card.
- **Property and edge tests** in `:core:model`: round trips, a refund, four hundred dates
  against the confusable characters, a single wrong character, a budget too large to
  carry, case and separator tolerance, a damaged ledger line.

**What is still true and still stated in the UI:** a code is a copy, not a link; the
checksum catches accident, not fraud; a code can be handed over twice and is refused; and
nothing updates on its own. The two-phone test itself is
[`docs/HUMAN-TASKS.md` §6](docs/HUMAN-TASKS.md) — it needs two phones.

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
