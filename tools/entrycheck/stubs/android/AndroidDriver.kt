package app.cash.sqldelight.driver.android
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
/** Stub mirroring the real constructor: (schema, context, name). */
class AndroidSqliteDriver(
    schema: SqlSchema<*>,
    context: Context,
    name: String,
) : SqlDriver by error("stub driver: not instantiable")
