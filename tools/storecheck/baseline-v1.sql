-- The schema as it actually shipped at commit 81df38b, captured verbatim from
-- that commit. This file is the point: the previous migration test invented a
-- "version 1" that no build ever wrote to disk, so it passed while every real
-- database in the world failed. A fixture is only worth anything if it is the
-- thing the user has.
--
-- Regenerate with:
--   git show 81df38b:core/store/src/commonMain/sqldelight/com/ariadne/store/db/Ariadne.sq
-- and take everything up to the '-- -- queries' line.

CREATE TABLE trip (
    id              TEXT NOT NULL PRIMARY KEY,
    name            TEXT NOT NULL,
    startDate       TEXT NOT NULL,
    endDate         TEXT NOT NULL,
    baseCurrency    TEXT NOT NULL,
    homeCurrency    TEXT NOT NULL,
    closedAt        INTEGER
);

CREATE INDEX trip_start ON trip(startDate);

CREATE TABLE capture (
    id                      TEXT NOT NULL PRIMARY KEY,
    tripId                  TEXT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    localDate               TEXT NOT NULL,
    photoPath               TEXT NOT NULL,
    createdAt               INTEGER NOT NULL,
    state                   TEXT NOT NULL,
    amountMinor             INTEGER,
    currency                TEXT,
    merchant                TEXT,
    extractionConfidence    REAL,
    confirmedAt             INTEGER,
    markedNoAmount          INTEGER NOT NULL DEFAULT 0,
    -- Retry bookkeeping lives with the row, not in memory, so a process death
    -- in a dead zone resumes exactly where it stopped.
    failedAttempts          INTEGER NOT NULL DEFAULT 0,
    retryNotBefore          INTEGER
);

CREATE INDEX capture_trip_day ON capture(tripId, localDate, createdAt);
CREATE INDEX capture_state ON capture(state);
CREATE INDEX capture_merchant ON capture(merchant);

CREATE TABLE dayCard (
    id          TEXT NOT NULL PRIMARY KEY,
    tripId      TEXT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    localDate   TEXT NOT NULL,
    closedAt    INTEGER,
    UNIQUE(tripId, localDate)
);

CREATE TABLE rate (
    base        TEXT NOT NULL,
    quote       TEXT NOT NULL,
    rateDate    TEXT NOT NULL,
    rateScaled  INTEGER NOT NULL,
    PRIMARY KEY (base, quote, rateDate)
);

CREATE TABLE settings (
    id              INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
    baseCurrency    TEXT NOT NULL,
    homeCurrency    TEXT NOT NULL,
    theme           TEXT NOT NULL,
    ocrProvider     TEXT NOT NULL
);

