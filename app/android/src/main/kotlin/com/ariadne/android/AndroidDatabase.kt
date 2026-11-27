package com.ariadne.android

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ariadne.store.db.AriadneDb
import com.ariadne.store.openOrMigrate

/**
 * The old app wrote databases with user_version=0, even when they had tables.
 * SQLiteOpenHelper calls onCreate for those files, so its default callback would
 * attempt to create the schema over the user's existing trip. Inspect the actual
 * schema in both paths instead of assuming that version zero means an empty file.
 */
internal fun openAndroidDatabase(context: Context, name: String): AndroidSqliteDriver =
    AndroidSqliteDriver(
        schema = AriadneDb.Schema,
        context = context,
        name = name,
        callback = object : AndroidSqliteDriver.Callback(AriadneDb.Schema) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                openOrMigrate(AndroidSqliteDriver(db))
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                openOrMigrate(AndroidSqliteDriver(db))
            }
        },
    )
