package com.vibecalendar.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import com.vibecalendar.model.CalendarAccount
import com.vibecalendar.model.DeviceCalendar
import com.vibecalendar.model.DisplayEvent
import com.vibecalendar.model.RawCalendarEvent

class CalendarRepository(private val contentResolver: ContentResolver) {

    /**
     * Query all calendars from the device, grouped by account.
     */
    fun getCalendarAccounts(): List<CalendarAccount> {
        val calendars = mutableListOf<DeviceCalendar>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
        )

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val accountNameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val displayNameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val colorIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            val visibleIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

            while (it.moveToNext()) {
                calendars.add(
                    DeviceCalendar(
                        id = it.getLong(idIdx),
                        accountName = it.getString(accountNameIdx) ?: "",
                        displayName = it.getString(displayNameIdx) ?: "",
                        color = Color(it.getInt(colorIdx)),
                        visible = it.getInt(visibleIdx) == 1,
                    )
                )
            }
        }

        return calendars
            .groupBy { it.accountName }
            .map { (accountName, cals) ->
                CalendarAccount(
                    accountName = accountName,
                    accountType = "",
                    calendars = cals,
                )
            }
    }

    /**
     * Query all calendars as a flat list.
     */
    fun getAllCalendars(): List<DeviceCalendar> {
        return getCalendarAccounts().flatMap { it.calendars }
    }

    /**
     * Query events for a given day (midnight to midnight), for the specified calendar IDs.
     * Returns deduplicated DisplayEvents.
     */
    fun getEventsForDay(dayStartMillis: Long, dayEndMillis: Long, calendarIds: Set<Long>): List<DisplayEvent> {
        if (calendarIds.isEmpty()) return emptyList()

        val rawEvents = queryRawEvents(dayStartMillis, dayEndMillis, calendarIds)
        return deduplicateEvents(rawEvents)
    }

    private fun queryRawEvents(dayStartMillis: Long, dayEndMillis: Long, calendarIds: Set<Long>): List<RawCalendarEvent> {
        val events = mutableListOf<RawCalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.UID_2445,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
        )

        // Build Instances URI with time range (handles recurring event expansion)
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, dayStartMillis)
        ContentUris.appendId(builder, dayEndMillis)

        // Build calendar ID filter
        val calendarIdList = calendarIds.joinToString(",")
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($calendarIdList)"

        val cursor: Cursor? = contentResolver.query(
            builder.build(),
            projection,
            selection,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances._ID)
            val calendarIdIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val uidIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.UID_2445)
            val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val locationIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val colorIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)

            while (it.moveToNext()) {
                events.add(
                    RawCalendarEvent(
                        id = it.getLong(idIdx),
                        calendarId = it.getLong(calendarIdIdx),
                        uid = it.getString(uidIdx),
                        title = it.getString(titleIdx) ?: "(No title)",
                        description = it.getString(descIdx),
                        location = it.getString(locationIdx),
                        startMillis = it.getLong(beginIdx),
                        endMillis = it.getLong(endIdx),
                        allDay = it.getInt(allDayIdx) == 1,
                        color = it.getInt(colorIdx).let { c -> Color(c) },
                    )
                )
            }
        }

        return events
    }

    /**
     * Deduplicate events by iCalendar UID.
     *
     * Events with the same non-null UID and overlapping time windows are merged
     * into a single DisplayEvent listing all calendar IDs.
     * Events with null UIDs are never merged.
     */
    private fun deduplicateEvents(rawEvents: List<RawCalendarEvent>): List<DisplayEvent> {
        val result = mutableListOf<DisplayEvent>()
        val grouped = mutableMapOf<String, MutableList<RawCalendarEvent>>()

        for (event in rawEvents) {
            val uid = event.uid
            if (uid != null) {
                grouped.getOrPut(uid) { mutableListOf() }.add(event)
            } else {
                // No UID - always show as individual event
                result.add(event.toDisplayEvent())
            }
        }

        for ((_, events) in grouped) {
            // Group by approximate time to handle the same UID appearing at different
            // times on different days (recurring events expanded by Instances query).
            val timeGroups = events.groupBy { it.startMillis }
            for ((_, timeGroup) in timeGroups) {
                val primary = timeGroup.first()
                result.add(
                    DisplayEvent(
                        uid = primary.uid,
                        title = primary.title,
                        description = primary.description,
                        location = primary.location,
                        startMillis = primary.startMillis,
                        endMillis = primary.endMillis,
                        allDay = primary.allDay,
                        calendarIds = timeGroup.map { it.calendarId }.distinct(),
                    )
                )
            }
        }

        return result.sortedWith(
            compareBy<DisplayEvent> { it.allDay }.reversed()
                .thenBy { it.startMillis }
                .thenBy { it.title }
        )
    }

    private fun RawCalendarEvent.toDisplayEvent() = DisplayEvent(
        uid = uid,
        title = title,
        description = description,
        location = location,
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
        calendarIds = listOf(calendarId),
    )
}
