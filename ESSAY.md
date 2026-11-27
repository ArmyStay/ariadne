# Ariadne — 300 words

The first thing I lose on a trip is not money. It is the *reason* I spent it.

A chai at a railway stall in Kerala, 20 rupees. A pair of sandals in Seoul because the
ones I was wearing gave up on the third day — 32,000 won. Two currencies, one wallet, and
by evening I cannot remember which of the four receipts in my pocket was the shoe. A
notes app would have taken all four. But a notes app asks me to open it, find the right
note, and type a number and a thought while the vendor waits for the change. That is four
seconds too many, every time, so the note never happens.

Ariadne inverts the order. The shutter makes one photograph — under two seconds, the
phone already in my hand — and the amount is allowed to be nobody's problem until the
end of the day. Close Day is where the figures land: a small stack of cards, confirmed in
a few taps or left as photographs, because *no amount* is a legal entry in this book.

Both halves had to live in one codebase. The same domain model, the same receipt parser,
the same design system, the same tests run on Android, iOS, desktop and in a browser —
one shutter, one queue, one migration path, checked on every target on every push.
Nothing syncs, because syncing means trusting someone else's server with a trip's worth
of small, private decisions. Money is a `Long`, conversion is rounded once at the
boundary, and the record never leaves the phone.

What I did not expect was the glasses: a camera that is not the phone. The same shutter,
one mark in the corner of the frame, and the picture exists before the phone leaves my
pocket. That is the version of this idea I actually wanted.
