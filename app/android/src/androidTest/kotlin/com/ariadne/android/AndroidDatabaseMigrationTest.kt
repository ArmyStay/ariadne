package com.ariadne.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import com.ariadne.store.db.AriadneDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises SQLiteOpenHelper's real onCreate/onUpgrade paths, not just a JDBC driver. */
@RunWith(AndroidJUnit4::class)
class AndroidDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-instrumented.db"

    @After
    fun cleanUp() { context.deleteDatabase(name) }

    @Test
    fun unversionedDatabaseIsUpgradedWithoutLosingTrip() = checkUpgrade(0)

    @Test
    fun baselineVersionIsUpgradedWithoutLosingTrip() = checkUpgrade(1)

    private fun checkUpgrade(version: Int) {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val sql = context.assets.open("migration/baseline-v1.sql").bufferedReader().use { it.readText() }
            // The fixture's comments contain semicolons, so discard comments first.
            val statements = sql.lineSequence().filterNot { it.trimStart().startsWith("--") }
                .joinToString("\n").split(';')
            statements.map { it.trim() }.filter { it.isNotEmpty() }.forEach(db::execSQL)
            db.execSQL("INSERT INTO trip (id,name,startDate,endDate,baseCurrency,homeCurrency) VALUES ('kept','Kept','2026-11-01','2026-11-02','INR','INR')")
            db.execSQL("INSERT INTO capture (id,tripId,localDate,photoPath,createdAt,state,amountMinor,currency,merchant) VALUES ('receipt','kept','2026-11-01','photo.jpg',123,'CONFIRMED',1250,'INR','Tea')")
            db.execSQL("INSERT INTO dayCard (id,tripId,localDate,closedAt) VALUES ('day','kept','2026-11-01',456)")
            db.version = version
        }
        repeat(2) {
            val driver = openAndroidDatabase(context, name)
            try {
                val nameFromDb = driver.executeQuery(null, "SELECT name FROM trip WHERE id = 'kept'", { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
                }, 0).value
                assertEquals("Kept", nameFromDb)
                val capture = driver.executeQuery(null, "SELECT photoPath, amountMinor, merchant FROM capture WHERE id = 'receipt'", { cursor ->
                    QueryResult.Value(if (cursor.next().value) listOf(cursor.getString(0), cursor.getLong(1).toString(), cursor.getString(2)) else emptyList())
                }, 0).value
                assertEquals(listOf("photo.jpg", "1250", "Tea"), capture)
                val closedAt = driver.executeQuery(null, "SELECT closedAt FROM dayCard WHERE id = 'day'", { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
                }, 0).value
                assertEquals(456L, closedAt)
                val columns = driver.executeQuery(null, "PRAGMA table_info(capture)", { cursor ->
                    val found = mutableListOf<String>()
                    while (cursor.next().value) cursor.getString(1)?.let(found::add)
                    QueryResult.Value(found)
                }, 0).value
                assertTrue(columns.contains("category"))
                val actualVersion = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
                }, 0).value
                assertEquals(AriadneDb.Schema.version.toLong(), actualVersion)
            } finally {
                driver.close()
            }
        }
    }
}
