# Ariadne — the pages, for a design tool

A brief you can paste into Miro AI (or any design generator). It says **what each
page is and what it is for**, not how it should look — no colours, no typefaces,
no layout instructions. If a generator is given visual direction it will produce
a generic dashboard; if it is given intent it has a chance of producing ours.

---

## The product in one paragraph

Ariadne is a **trip ledger with a camera**. You photograph what you spend on
while you are travelling — a receipt, a stall, a ticket, a souvenir — and you do
not type anything. No amount, no category, no form, in the moment. The numbers
wait. In the evening, over a few minutes, you sit down with the photographs of
that day and finish them: each one takes about two taps to become a real figure
in a record. At the end of the trip you have a card you keep: every day, what it
came to, and the photographs behind it.

It is a personal record, not an expense report. There is no account, no server,
no sync, no analytics, and nothing leaves the device. The whole product works in
airplane mode, forever. That is not a privacy feature bolted on — it is the
product's one promise, and every screen is designed to be honest about it.

**The central inversion, stated on the first screen:** the photograph is the
memory, and the numbers wait for night. Most money apps make you book a
transaction and then forget it. This one keeps the moment and asks about the
money later, together, once.

---

## The overall vision

**It should feel like a small printed book that happens to be an app.** Not a
dashboard, not a banking product, not a "wellness" app. A ledger with plates. The
reference points are a photographer's contact sheet, a museum wall label, an
accounts book kept by hand — things that are quiet, ruled, and confident enough
not to decorate themselves.

Three commitments, and they should be visible on every page:

1. **The photograph is the largest thing on the page, always.** Wherever a
   photograph appears, it dominates. Text sits under it or beside it, never over
   it, never competing.
2. **Nothing is required.** Every field is optional, including the amount. An
   unfinished photograph is a valid record, not an error, and the app never
   scolds, celebrates, or uses a warning colour.
3. **It is a sequence, not a set of screens.** The pages are numbered plates
   (01, 02, 03 …) and read in order. A person should always know where in the
   book they are.

**Tone of voice:** plain, dry, a little wry. Short declaratives. No exclamation
marks, no "Let's get started!", no emoji. Copy states facts and admits
limitations. When the app cannot do something it says so rather than dressing it
up. It never flatters the user.

**What it must never look like:** a fintech dashboard, a habit tracker, a
productivity app, a Material Design template, anything with a floating action
button, rounded filled buttons, or white cards on a grey background.

---

## The pages

Twelve surfaces. Each entry: what the page is, what it is for, and what happens
on it.

### 01 — Create a trip (the front door)

The first page on a fresh install, and nothing exists yet. A trip has to exist
before a photograph can be filed against it. It asks for **a name, where you are
going, a start date, an end date, a currency, and optionally what you intend to
spend**. The budget is explicitly optional and is described as a fence, not a
target. Mistakes are refused with a sentence that names the field, never silence.

*Purpose:* get from install to holding a camera in one screen.

### 02 — Home (the day you are in)

The page the app opens to once a trip exists. It shows the trip's name, the
day's total so far, how many photographs are still waiting to be finished, and
one large action to start the evening ritual. Under that, a short ruled index of
the other places you can go: the timeline, the record, search, the trip itself.

*Purpose:* answer "where am I today, and is there anything waiting for me?"

### 03 — The shutter

The camera — the real one — and nothing else. A single framing plate filling most of the screen,
one shutter control, and two quiet ways out (home, and the evening ritual). No
amount field, no category picker, no form. It exists to make taking the
photograph the easiest thing in the app.

*Purpose:* capture in under a second, with zero decisions.

### 04 — The frame, held up

Immediately after the shutter fires, the photograph you just took fills the
page. Two choices, and neither loses the photograph: **keep it for tonight**
(nothing to type — it joins the queue) or **fill it in now** (amount, what it
was, a note). This page is why the app does not feel like it swallowed your
photo.

*Purpose:* let the person decide, at the moment they remember, whether this one
is worth describing now.

### 05 — The ritual (finishing the day)

The heart of the product. The photographs taken today are presented **one at a
time**, as a stack of cards. Each card shows the photograph large, whatever the
camera's text recognition could read from it, and one number pad. You type the
amount, or tap "no amount" for a souvenir, and it is done — roughly two taps.
Then the next card. When the stack is empty, the day is closed.

This is the only place in the app that asks for money, and it asks for it once,
in one sitting, with the photographs in front of you. It should feel like
settling up at the end of a day, not like data entry.

*Purpose:* turn a day of photographs into a day of figures, in minutes.

### 06 — The record (a day, in full)

The day's photographs as a ruled list, each with its merchant, its amount, its
category, and the hour it was taken, sitting above a **24-hour band** that shows
where in the day the spending actually happened. This is the "reading" of the
day — what a contact sheet is to a roll of film.

*Purpose:* see the shape of a day, not just its total.

### 07 — One photograph, open

A single capture at full size with everything known about it: the amount, what
it was, what category it belongs to, a line to remember it by, and the currency.
All of it editable, all of it optional. Saving with no amount is a legitimate
outcome and the page offers it as a first-class choice.

*Purpose:* the place to correct or complete a single record.

### 08 — The timeline

The whole trip as a **run of days**. Each day gets a date, its total, and its
band of hours, down the page like a ledger spine. Closed days and open days are
distinguishable without a warning colour.

*Purpose:* see the trip's rhythm — which days were heavy, which were empty.

### 09 — What it came to

The trip's total, stated once and large. Under it, the same total broken down by
**what it was** (food, shopping, transport …) with a bar per category, then by
**currency before conversion**, then one honest sentence about the pattern — for
example, that most of the trip went on one thing. If a budget was set, the page
says how much room is left, or how far over, in exactly the same tone.

*Purpose:* answer "what did this trip cost, and what did it cost it on?"

### 10 — Search

Find a photograph by what you remember about it: the shop, the amount, the day,
the category. Results are the same ruled rows as the record.

*Purpose:* retrieve a specific memory.

### 11 — The trip's page

The trip as an object: its name, place, dates, currencies, how many days are
closed, and the single action that closes the trip and turns it into a card.

*Purpose:* the trip's own settings and its ending.

### 12 — The card (the thing you keep)

The payoff. Everything the app has collected, set as one column of type you
could print and keep: the name, the dates, the days, the currencies, one total,
and a line saying the record is yours and exists nowhere else. It can be shared
as text. A trip that has not finished yet still shows this page — it just says
so plainly.

*Purpose:* the ending. The reason the record exists.

### Preferences

Short by conviction: only settings that are real. Base currency, home currency,
the trip's identity, closing the trip, and deleting it. Each row either changes
something or states a fact — never a row that looks like a control and is not.
Deleting a trip takes two deliberate taps, because there is no copy anywhere
else.

*Purpose:* change the few things that can be changed, and be honest about the
rest.

---

## The objects that appear across pages

If the generator needs a vocabulary of parts rather than pages, these are the
recurring objects. They are listed by what they do, not how they look.

- **The plate** — a photograph, or a ruled stand-in when there is not one yet.
  Always the largest element on its page, always framed.
- **The band** — a 24-hour rail with a mark at each hour that carries something.
  Filled mark: finished. Outlined mark: still waiting.
- **The figure** — a single amount, set large enough to be the point of the page.
- **The rule** — a hairline that separates rows. The app's main structural device.
- **The plate number** — a two-digit mark at the top of each page, so pages read
  as a sequence.
- **The waiting count** — how many photographs are still unfinished, always
  visible, never alarming.

---

## One-sentence version, if the generator wants a title

> A photograph-first trip ledger: take the picture now, settle the numbers
> tonight, keep a card at the end — on your device, with no account and no
> server.

---

## A ready-to-paste prompt, if your tool wants one block

> Design a mobile app called Ariadne: a photograph-first trip ledger for
> travellers. The premise is an inversion — you photograph what you spend on
> during the day and type nothing; in the evening you sit down once and finish
> the day's photographs in about two taps each. There is no account, no server,
> and no sync: everything stays on the device.
>
> Design twelve screens, numbered as a sequence of plates. 01 create a trip (name,
> place, dates, currency, optional budget), 02 home for today with the day's
> total, the count of unfinished photographs, and one action to start the evening
> ritual, 03 the camera with a single shutter and no fields, 04 the photograph you
> just took held up with two choices — keep it for tonight, or fill it in now, 05
> the evening ritual: the day's photographs one at a time as a stack, each with
> one number pad, two taps to finish, 06 a day in full as a ruled list with a
> 24-hour band showing when spending happened, 07 one photograph open and fully
> editable, 08 the timeline: the whole trip as a run of days, 09 what it came to:
> one total, broken down by category and by currency, 10 search by shop, amount,
> day or category, 11 the trip's own page with the action that closes it, 12 the
> card you keep at the end — everything as one printable column of type.
>
> The feeling is a small printed book that happens to be an app: a ledger with
> photographs. Quiet, ruled, confident. The photograph is the largest thing on
> every page. Nothing is required of the user — an unfinished photograph is a
> valid record, not an error — and the app never scolds or celebrates. Avoid
> anything that looks like a fintech dashboard, a habit tracker, or a template.
> No floating action button, no rounded filled buttons, no white cards on grey.
> Tone of voice: plain and dry, short declaratives, no exclamation marks, no
> emoji, states limitations honestly.
