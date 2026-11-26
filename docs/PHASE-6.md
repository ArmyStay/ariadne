# Phase 6 — the landing page, opened up

## What was wrong with it

Phase 2 rebuilt Home around one question: *what have I spent today?* That was an
improvement on the old menu of links, but it was thin. Opening the app on a trip told
you today's figure and hid everything else:

- the trip's total was on Trip, behind the bar;
- the budget's shape was a pill with one number in it;
- the register of trips was behind a link at the top of the page;
- the day you had already finished was one line of text under today's photographs;
- settings were two taps deep in a corner of another page.

The reference you sent — a landing page that opens on the **trip**, its money, what
is waiting, and the whole feed of days, with settings within reach — is the right
criticism. Home should open on the trip, not on one day of it.

## What the page is now

Six questions, in the order someone unlocking their phone in a shop actually asks
them:

1. **Which trip, and how is it going?** The trip's name, switchable from its own
   label, the date, the base currency, how many photographs are waiting — and a
   **gear, top right, that goes straight to settings.** No menu.
2. **The trip in one line.** A bordered insight strip: *insights · 16% used …
   ₹417,858.08 left*, and it opens Stats. The same BudgetPlan the Trip and Stats
   pages use, so the three cannot round differently.
3. **What today has cost** — the largest figure on the page, on ink, with the day's
   currency and photograph count, and, when something is pending, *"2 still to
   settle · finish"*.
4. **What the trip has cost, against the fence** — the total, the budget, the bar
   with its pace tick, and whether the spending is ahead of the days or behind them.
5. **What is waiting** — the photographs themselves, not just a count, and a round
   button that goes and settles them.
6. **The record** — the trip as a feed of days, newest first, today open and every
   past day collapsed to one line: weekday, figure, photograph count, waiting count.
   Chips filter it: **All / Pending / Today**. Tapping a day opens that day's page;
   the chevron opens the photographs in place.

Then a footer strip — **trips · search · settings** — because those three should be
on the page and not behind a menu, which is a different failure from the old Home's
five links: the old page was *only* links, this is a page that ends with them.

## How it is built

Nothing new was needed in the model. The page is a reading of state the app already
loads for the timeline: `categoryTotals` for the trip's spend, `dayCards` and
`dayFigures` for the feed, `dayCapturesAll` for the photographs under each day,
`tripPending` for the tray, and `BudgetPlan` for every figure that compares money to
a fence.

New private parts of `TodayPage.kt`: `TripHead`, `InsightStrip`, `TodayCard` (the ink
one), `TripTotalCard` (the bar), `Waiting`, `Feed` with `DayGroup` and `Entry`,
`MoreStrip`, and the small `Chip` / `Tag` / `RoundButton` / `Chevron` / `Dot` marks.
Nothing is shared out into `:core:design` yet: these are this page's furniture, and a
component extracted after one use is a component shaped by one use.

Two decisions worth recording:

- **The budget bar is not drawn on the ink card.** `BudgetBar` draws its track from
  `paper.sunk` and its fill from `paper.ink`; on an ink surface the fill would be
  invisible. So the dark surface carries the *day* — the figure that moves — and the
  trip's total and its bar are on paper, where the measure reads correctly.
- **The gear is a control, not a link in a list.** Settings is the one thing you
  asked for on this page, so it is at the top right where a phone puts it.

## What is still the old build

The day record, the close-day pager, the trip card and the first-run form. The feed
on this page can already show you a day's photographs, so the day record is now the
next screen that will feel out of date — it is the one the feed's day rows open.
