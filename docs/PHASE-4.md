# Phase 4 — the arithmetic, tested

Phases 1–3 rebuilt the flow and the visual system. This phase is about the part of
that work a machine can check, because most of it could not be: no Gradle, no
Compose, and a sandbox that cannot reach Maven. What could be checked was, and this
is what was built to make the checking real rather than asserted.

## The problem this phase answers

Three pages do the same sums. `Today` divides the budget by the days to say what a
day of it is. `Trip` divides spending by the budget for a bar and days closed by days
in the trip for a second mark. `Stats` does both again, with its own rounding.

None of that is difficult, which is exactly why it drifts: each page was written at a
different time, and a page that says "₹3,400 a day" beside a page that says
"₹3,399.99 a day" tells someone their record cannot be trusted. And because the sums
lived in composables, no test could reach them — the one place where the redesign's
largest surface had no coverage at all.

## `BudgetPlan` (`:core:model/BudgetPlan.kt`)

A pure value type: a budget, the days in the trip, what has been spent, how many days
have been closed. Out of it come the figures and fractions the pages draw —
`dailyMinor`, `leftMinor`, `isOver`, `spendFraction`, `paceFraction`,
`isAheadOfPace`, and `todayAgainstShare(todayMinor)` for Today's own line.

Four conventions, each of which was a way the pages could disagree:

- **A budget of zero is no budget.** `Trip.budgetMinor` is optional because a trip
  without one is still a trip. A stored zero has to mean "no fence" too, or every page
  reads "₹0 left" at somebody who never set one.
- **The day's share rounds down.** A share quoted to a person is a spendable number;
  rounding it up tells them they can spend more than the fence allows.
- **Pace is time, not money.** `paceFraction` is how much of the trip's *duration* has
  passed, because the honest comparison for "am I spending too fast" is spending
  against an even share of the days.
- **A trip with no days still divides by one.** A trip whose dates have not resolved
  yet has no days, and dividing by it would be an arithmetic error on a page somebody
  is looking at.

## Tests (`:core:model/src/commonTest/.../BudgetPlanTest.kt`)

Eleven cases, run green here: the divide-down share, no budget, a budget of zero,
left and over as one figure with a sign, spending exactly the fence, clamped
fractions, pace as time (including a stale day count past the end of the trip),
ahead-of-pace comparisons in both directions, today against the day's share rather
than the trip, a trip with no days, and the `None` state.

`tools/test-domain.sh` now runs `BudgetPlanTest` in its JVM leg alongside the rest of
the suite.

## `tools/bootstrap-toolchain.sh`

The environment this was written in can reach npm and PyPI but not Maven, and
`tools/test-domain.sh` expects a compiler under `~/.cache/tools`. So there is now a
script that builds that toolchain from the two registries that *are* reachable:

- the Kotlin compiler from the **npm** package `kotlin-compiler` — the tarball is the
  ordinary compiler distribution, `bin/` and `lib/` intact;
- the JDK from the **PyPI** wheel `jdk4py`, a Temurin runtime in a wheel;
- a small `kotlin.test` shim and a reflective runner, so the suites run with nothing
  but the stdlib on the classpath.

Run from a clean directory it compiles and runs both suites: **`:core:model` 47
passed, `:core:fx` 38 passed.**

It is documented as a fallback, not a replacement. It compiles the pure-Kotlin cores
only; nothing in it can build or render Compose.

## What is still not verified

- **The UI.** No layout, spacing, colour, animation or gesture in Phases 2 and 3 has
  ever been rendered in this environment. `tools/verify-ui.sh` followed by
  `tools/run-preview.sh --shots` on a machine with Gradle is the gate.
- **The new flow checks.** `preview/FlowCheck.kt` gained assertions for the back
  stack, the sheet's landing and undo. They need the real modules to run, so they have
  not.
- **The picks.** `pickMany` and the Android `PickMultipleVisualMedia` path are
  compile-checked only; a picker is a platform dialog.
