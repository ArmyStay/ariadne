package com.ariadne.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

/**
 * JVM file-based preferences, stored outside the app sandbox in a normal user file.
 */
internal fun desktopPreferences(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    produceFile = { File(".ariadne_preferences.pb") },
)
