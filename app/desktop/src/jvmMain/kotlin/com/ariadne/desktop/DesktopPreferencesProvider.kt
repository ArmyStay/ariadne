package com.ariadne.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

/**
 * JVM file-based preferences, so flags and counters are shared between the desktop
 * app and the web app on a machine. Not inside the sandbox — a normal user file.
 */
internal fun desktopPreferences(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    file = File(".ariadne_preferences.pb"),
)
