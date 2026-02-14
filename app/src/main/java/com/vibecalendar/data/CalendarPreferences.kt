package com.vibecalendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calendar_prefs")

/**
 * Persists which calendar IDs the user has toggled on/off.
 * Stores the set of *hidden* calendar IDs. Calendars not in this set are shown.
 */
class CalendarPreferences(private val context: Context) {

    private val hiddenCalendarsKey = stringSetPreferencesKey("hidden_calendar_ids")

    val hiddenCalendarIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        prefs[hiddenCalendarsKey]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun toggleCalendar(calendarId: Long, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[hiddenCalendarsKey]?.toMutableSet() ?: mutableSetOf()
            if (hidden) {
                current.add(calendarId.toString())
            } else {
                current.remove(calendarId.toString())
            }
            prefs[hiddenCalendarsKey] = current
        }
    }
}
