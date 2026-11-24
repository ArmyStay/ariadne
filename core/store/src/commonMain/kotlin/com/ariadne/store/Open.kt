package com.ariadne.store

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.ariadne.store.db.AriadneDb

/**
 * Opening a database that already exists.
 *
 * The rule this file exists to enforce: **almost nothing here should call
 * `create`.** `create` throws once the tables are there, and if that throw
 * happens inside a launched coroutine it is swallowed and the app looks broken
 * rather than noisy — which is exactly how "Open the trip" came to do nothing at
 * all on a device that had run an older build.
 *
 * So: read the schema version the file reports, and take one of three roads.
 * A brand-new file gets the schema; an older file gets the migrations; a file of
 * the current version gets nothing at all. Only the first road creates.
 *
 * Kept in `commonMain` and used by both the desktop entry point and the store's
 * own smoke test, so the upgrade path that ships is the upgrade path that is
 * tested. There is no second copy to drift.
 */
public fun openOrMigrate(
    driver: SqlDriver,
    schema: SqlSchema<QueryResult.Value<Unit>> = AriadneDb.Schema,
): SqlDriver {
    val version = driver.schemaVersion()
    when {
        version == 0L -> {
            schema.create(driver)
            driver.setSchemaVersion(schema.version.toLong())
        }
        version < schema.version -> {
            schema.migrate(driver, version, schema.version)
            driver.setSchemaVersion(schema.version.toLong())
        }
        else -> Unit
    }
    return driver
}

/** `PRAGMA user_version`: 0 for a file SQLite has never written a schema into. */
public fun SqlDriver.schemaVersion(): Long = try {
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            QueryResult.Value(
                if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L,
            )
        },
        parameters = 0,
    ).value
} catch (e: Exception) {
    // An unreadable file is treated as new rather than as fatal; `create` will
    // then fail loudly with the real reason instead of this one.
    0L
}

public fun SqlDriver.setSchemaVersion(version: Long) {
    execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}
