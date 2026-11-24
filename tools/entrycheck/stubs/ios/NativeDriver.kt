package app.cash.sqldelight.driver.native
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
/** Stub mirroring the real constructor: (schema, name). */
class NativeSqliteDriver(
    schema: SqlSchema<*>,
    name: String,
) : SqlDriver by error("stub driver: not instantiable")
