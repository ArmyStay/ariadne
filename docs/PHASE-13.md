# Phase 13 — the demo that is actually an app, and a README that reads like a front door

## What this is

A review, not a bug report: the demo link served the README instead of the app, `main`
had a red run, the screenshots were pre-review and the README announced them as no
evidence at all, the first screen argued with itself in two places, and the entry essay
was a stale draft that named the wrong tabs.

This phase is the response, in the order the review put it.

## 1. There is a browser build now

`armystay.github.io/ariadne` was the repository's Pages **branch** deployment: Jekyll
publishing `README.md` as a web page. `.github/workflows/pages.yml` replaces it with an
artifact deployment:

- `on: push: branches: [main]`, plus `workflow_dispatch`;
- `./gradlew :app:wasm:wasmJsBrowserDistribution`;
- `actions/configure-pages`, `upload-pages-artifact` from
  `app/wasm/build/dist/wasmJs/productionExecutable`, then `deploy-pages` in its own job;
- `concurrency: pages`, `cancel-in-progress` — one deploy at a time, the newest wins.

It is deliberately **not** a job inside `ci.yml` (where it used to be, gated on the
matrix): a failed test run must not leave the live demo pointing at an old build, and a
demo build must not wait on the iOS job. The `pages` job has been removed from `ci.yml`.

**One repository setting is required and it cannot be set from a workflow.** Pages must
be switched to *Settings → Pages → Build and deployment → Source: **GitHub Actions***,
which turns off the branch deployment (`build_type` is `legacy` at the time of writing;
the REST call that would change it is refused for this token). Until that switch is
flipped, `deploy-pages` will fail with "Pages is not configured for GitHub Actions" —
loudly, on purpose, rather than quietly serving a README.

## 2. The README is a front door

New first screen, in the reviewer's order: logo, one sentence, the demo link, two stills
as a looping GIF, the five-target table, the promise table — and only then the prose.

- the "one tab, one stack" / four-pillars contradiction is gone; the four tabs are named
  once, accurately;
- the public-share-link claim is **deleted** (the route is not shipped, so it is not
  advertised; the status section says so instead);
- capture is ordered camera → voice → glasses, with **no hardware is needed** stated in
  the glasses paragraph;
- "Nightfall" became **Close Day**, which is what the app actually says;
- "Everything is gradle-free for now" is gone: the gates are shell scripts that run
  without a full toolchain, and the Gradle build is the shipping path;
- status is now *what is proven where* — CI, a phone, and not-yet — which is the same set
  of facts with a defensible order;
- `minSdk 29` keeps its one-line reason: the glasses SDK moved the floor.

## 3. Screenshots

`docs/shots/current/` holds two stills taken on a phone on 27 November 2026, and
`docs/shots/demo-stills.gif` loops between them. The GIF is **two stills, not a
recording**, and the README says so in the caption; a screen recording replaces it when
the next build is on the device. The pre-review archive stays in the repository as
history, and the README no longer announces it as the only evidence there is.

## 4. The essay

`ESSAY.md` at the repository root — 300-ish words, in the chai-and-shoes register the
brief asks for: the receipt you stop being able to explain, why a notes app loses that
race every time, the inversion (photograph now, figure at Close Day), why one shared
codebase was the only way to keep both halves honest, and what was not synced.

`docs/ESSAY.md` (a stale draft that described the old four-tab bar and “Nightfall”) is
deleted rather than left to contradict the product.

## 5. Housekeeping

Dependabot's queue was cleared: the two dependency bumps whose checks passed were
merged, the five that failed were closed rather than left to age. Merged `arena/*`
branches were deleted.

## Verified, and not

- **Verified by CI:** this branch's `four targets` run — shared JVM tests, Android
  `assembleDebug`, desktop JVM, iOS arm64 + simulator. The workflow files parse and the
  deploy job's inputs (`commit`ed actions by SHA) match the versions already in `ci.yml`.
- **Not verified here:** the Pages deployment itself. The source is still `legacy` and
  switching it is a repository setting outside this token's reach, so the first green
  `web demo` run will be the first proof. Nothing in this phase claims the URL works
  before that.
