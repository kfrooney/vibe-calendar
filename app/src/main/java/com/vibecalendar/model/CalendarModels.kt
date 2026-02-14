package com.vibecalendar.model

import androidx.compose.ui.graphics.Color

/**
 * Represents a calendar account (e.g. "john@gmail.com") containing multiple calendars.
 */
data class CalendarAccount(
    val accountName: String,
    val accountType: String,
    val calendars: List<DeviceCalendar>,
)

/**
 * Represents a single calendar from the device's CalendarContract.
 */
data class DeviceCalendar(
    val id: Long,
    val accountName: String,
    val displayName: String,
    val color: Color,
    val visible: Boolean,
)

/**
 * A raw event as read from CalendarContract, before deduplication.
 */
data class RawCalendarEvent(
    val id: Long,
    val calendarId: Long,
    val uid: String?,
    val title: String,
    val description: String?,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Color?,
)

/**
 * A deduplicated event that may span multiple calendars.
 * If multiple CalendarContract events share the same iCalendar UID,
 * they are merged into one DisplayEvent with multiple calendarIds.
 */
data class DisplayEvent(
    val uid: String?,
    val title: String,
    val description: String?,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarIds: List<Long>,
)
