package com.ariadne.store

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.ariadne.store.db.AriadneDb

/**
 * Opening a database that already exists.
 *
 * **The bug this file now exists to prevent, twice over.**
 *
 * No build of this app before this change ever wrote `PRAGMA user_version`. The
 * generated `create` does not do it — only `migrate` does — so every database
 * that has ever run this app reports version **0**, whether it was created
 * yesterday by the baseline schema or holds real trips from a later one. Reading
 * `0` as "empty file" and calling `create` on it throws "table already exists";
 * treating it as "current" and doing nothing leaves the tables a version behind,
 * and every query then fails on a column that is not there. That second case is
 * what the user saw: `no such column: capture.category`, `table trip has no
 * column named place`, on a database that had a full trip in it.
 *
 * So the decision is made from the **file**, not from the pragma:
 *
 * - no `trip` table at all       -> it is new; create at the current version
 * - a `trip` table but version 0 -> it predates versioning; stamp it and, if it
 *                                   is missing columns, repair by column
 * - an older version             -> migrate up
 * - the current version          -> nothing at all
 *
 * `1.sqm` is the baseline CREATE, so a brand-new file reports version 1 the
 * moment `create` runs - migration numbering is deliberately offset by one from
 * the schema version. That is why "predates versioning" is version 0 and not 1.
 *
 * The version is written after `create` as well as after `migrate`, which is the
 * omission that caused all of this.
 *
 * Note the shape of every `catch` below: the version pragma and the table probe
 * are *different questions*, and answering 0 to the first because the second
 * failed is how a full database gets `create`d over.
 */
public object SchemaVersion {
    /** The baseline: a `trip` table exists but the file was never versioned. */
    public const val Unversioned: Long = 0L
}

public fun openOrMigrate(
    driver: SqlDriver,
    schema: SqlSchema<QueryResult.Value<Unit>> = AriadneDb.Schema,
): SqlDriver {
    val current = schema.version.toLong()
    val reported = driver.userVersion()

    // Two independent facts. On a file that has never been touched both are
    // false and the answer is "create". On a file this app has written, `hasTrip`
    // is true no matter what the pragma says.
    val hasTrip = driver.hasTable("trip")

    when {
        // Empty file: the only case that is allowed to create.
        !hasTrip && reported <= SchemaVersion.Unversioned -> {
            schema.create(driver)
            driver.setUserVersion(current)
        }

        // A file this app has written before anything was versioned. Every build
        // up to this one land here, so this is the branch that matters: it holds
        // the *baseline* schema, or a later one, and which one is decided by the
        // columns actually present rather than by a number nobody wrote.
        reported <= SchemaVersion.Unversioned -> {
            if (driver.hasEveryCurrentColumn()) {
                // Already the current shape - an interim build created it and
                // simply never stamped it. Stamping is the whole job.
                driver.setUserVersion(current)
            } else {
                driver.upgrade(driver.assumeBaselineVersion(), schema)
                driver.setUserVersion(current)
            }
        }

        reported < current -> {
            driver.upgrade(reported, schema)
            driver.setUserVersion(current)
        }

        // Current, or from the future. There is nothing safe to do, and doing
        // nothing is what keeps a full database intact.
        else -> Unit
    }
    // A table added after a file was stamped is the one thing `ALTER TABLE` cannot
    // express and a version number cannot repair, so it is created here: `IF NOT
    // EXISTS` makes this a no-op on a file that already has it, and a file written
    // by a build that predates the table gets it without losing a single row.
    driver.createMissingTables()
    return driver
}

/**
 * Every table this app has added since the baseline.
 *
 * The counterpart of [AddedColumns] for whole tables: `6.sqm` declares the traveller
 * tables for the migration path, and this declares them for the repair path — a file
 * that reports a version which already includes them but was written by a build
 * that did not create them.
 */
private val CurrentTables: List<String> = listOf(
    """CREATE TABLE IF NOT EXISTS traveller (
        id              TEXT NOT NULL PRIMARY KEY,
        tripId          TEXT NOT NULL,
        name            TEXT NOT NULL,
        addedAt         INTEGER NOT NULL,
        codeFingerprint TEXT NOT NULL
    )""",
    """CREATE TABLE IF NOT EXISTS travellerEntry (
        id               TEXT NOT NULL PRIMARY KEY,
        travellerId      TEXT NOT NULL,
        localDate        TEXT NOT NULL,
        amountMinor      INTEGER NOT NULL,
        currency         TEXT NOT NULL,
        category         TEXT,
        merchantInitials TEXT,
        digest           INTEGER NOT NULL
    )""",
    "CREATE INDEX IF NOT EXISTS traveller_trip ON traveller(tripId)",
    "CREATE INDEX IF NOT EXISTS traveller_entry_trip ON travellerEntry(travellerId)",
)

/** Creates whichever of [CurrentTables] the file is missing. Safe to run twice. */
public fun SqlDriver.createMissingTables() {
    CurrentTables.forEach { statement ->
        runCatching {
            execute(identifier = null, sql = statement, parameters = 0)
        }
    }
}

/**
 * Walk [from] up to the schema's version.
 *
 * The declared `.sqm` migrations are tried first: they are what SQLDelight
 * verifies at build time, so they are the path that should run. But a file can
 * report an old version while already carrying some of the newer columns -
 * any build before versioning existed produced one - and then `ALTER TABLE ...
 * ADD COLUMN` fails with "duplicate column name" halfway through, leaving the
 * file in a worse state than it started.
 *
 * So a failed run is repaired by column instead. `reconcile` is idempotent and
 * adds only what is missing, which is the question the version number was always
 * only trying to approximate.
 */
private fun SqlDriver.upgrade(from: Long, schema: SqlSchema<QueryResult.Value<Unit>>) {
    val migrated = runCatching { schema.migrate(this, from, schema.version) }.isSuccess
    if (!migrated) reconcile()
    // Whatever happened, finish by making the shape right. This catches both the
    // half-migrated file above and any single column a migration could not add.
    reconcile()
}

/**
 * What version to pretend an unversioned file is at.
 *
 * The baseline schema was the first thing ever created, so 1 is the honest guess
 * - and `upgrade` repairs it by column if the guess is wrong, which it is for
 * anybody who ran an interim build.
 */
private fun SqlDriver.assumeBaselineVersion(): Long = 1L

/** True when every column the current queries name is present. */
private fun SqlDriver.hasEveryCurrentColumn(): Boolean =
    AddedColumns.all { (table, column, _) -> hasTable(table) && hasColumn(table, column) }

/** `PRAGMA user_version`. 0 for a file SQLite has never versioned - which is not the same as empty. */
public fun SqlDriver.userVersion(): Long = try {
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).value
} catch (e: Exception) {
    // Deliberately 0 and not fatal: `hasTable` below is the question that decides
    // whether anything is created, and it will report the real problem.
    0L
}

/**
 * Every column this app has ever added, in the order it added them.
 *
 * This table duplicates `1.sqm` and `2.sqm` on purpose. The `.sqm` files are the
 * declared migrations and stay the primary path; this exists only to repair a
 * file that a previous build left half-upgraded, where re-running `ALTER TABLE`
 * is not possible. `tools/store-check.sh` builds exactly that file and proves
 * both paths agree.
 */
private val AddedColumns: List<Triple<String, String, String>> = listOf(
    // table, column, declared type. The type matters: SQLite assigns affinity
    // from it, and a money column declared TEXT would store the digits as text.
    Triple("capture", "category", "TEXT"),
    Triple("capture", "note", "TEXT"),
    Triple("trip", "place", "TEXT"),
    Triple("trip", "budgetMinor", "INTEGER"),
    // 7: the evening reminder.
    Triple("settings", "reminderEnabled", "INTEGER"),
    Triple("settings", "reminderHour", "INTEGER"),
    Triple("settings", "reminderMinute", "INTEGER"),
    Triple("settings", "reminderAsked", "INTEGER"),
)

/** Add whichever of [AddedColumns] the file is missing. Safe to run twice. */
public fun SqlDriver.reconcile() {
    AddedColumns.forEach { (table, column, type) ->
        if (hasTable(table) && !hasColumn(table, column)) {
            // Never fatal: one stubborn column must not stop the other three, and
            // the caller re-checks the shape afterwards.
            runCatching {
                execute(identifier = null, sql = "ALTER TABLE $table ADD COLUMN $column $type", parameters = 0)
            }
        }
    }
}

/** Whether [column] exists on [table]. */
public fun SqlDriver.hasColumn(table: String, column: String): Boolean = try {
    executeQuery(
        identifier = null,
        sql = "PRAGMA table_info($table)",
        mapper = { cursor ->
            var found = false
            while (cursor.next().value) if (cursor.getString(1) == column) found = true
            QueryResult.Value(found)
        },
        parameters = 0,
    ).value
} catch (e: Exception) {
    false
}

public fun SqlDriver.setUserVersion(version: Long) {
    execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}

/** Whether a table exists in this file. The only reliable "have I been here before". */
public fun SqlDriver.hasTable(name: String): Boolean = try {
    executeQuery(
        identifier = null,
        sql = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = '$name'",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).value > 0L
} catch (e: Exception) {
    false
}

/**
 * Throw away a database this version cannot use.
 *
 * There is one situation where the shape and the pragma genuinely disagree: an
 * early build that ran `create` without writing the version, then grew columns.
 * Such a file is indistinguishable from a current one by probe alone, and
 * guessing wrong either loses data or leaves broken tables. The caller decides;
 * on desktop the answer is to move the file aside and keep it, never to delete.
 */
public fun SqlDriver.dropAll(schema: SqlSchema<QueryResult.Value<Unit>> = AriadneDb.Schema) {
    listOf("capture", "dayCard", "rate", "settings", "travellerEntry", "traveller", "trip").forEach { table ->
        execute(identifier = null, sql = "DROP TABLE IF EXISTS $table", parameters = 0)
    }
    schema.create(this)
    setUserVersion(schema.version.toLong())
}
