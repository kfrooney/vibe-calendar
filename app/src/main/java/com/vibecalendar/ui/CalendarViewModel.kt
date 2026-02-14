package com.vibecalendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibecalendar.data.CalendarPreferences
import com.vibecalendar.data.CalendarRepository
import com.vibecalendar.model.DeviceCalendar
import com.vibecalendar.model.DisplayEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application.contentResolver)
    private val preferences = CalendarPreferences(application)

    private val _allCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val allCalendars: StateFlow<List<DeviceCalendar>> = _allCalendars.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    /**
     * Set of calendar IDs that are currently visible (not hidden by user).
     */
    val visibleCalendarIds: StateFlow<Set<Long>> = combine(
        _allCalendars,
        preferences.hiddenCalendarIds,
    ) { calendars, hiddenIds ->
        calendars.map { it.id }.filter { it !in hiddenIds }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _events = MutableStateFlow<List<DisplayEvent>>(emptyList())
    val events: StateFlow<List<DisplayEvent>> = _events.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            loadCalendars()
        }
    }

    fun loadCalendars() {
        viewModelScope.launch {
            _allCalendars.value = repository.getAllCalendars()
            loadEvents()
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadEvents()
    }

    fun goToNextDay() {
        selectDate(_selectedDate.value.plusDays(1))
    }

    fun goToPreviousDay() {
        selectDate(_selectedDate.value.minusDays(1))
    }

    fun goToToday() {
        selectDate(LocalDate.now())
    }

    fun toggleCalendar(calendarId: Long) {
        viewModelScope.launch {
            val currentHidden = visibleCalendarIds.value
            val isCurrentlyVisible = calendarId in currentHidden
            preferences.toggleCalendar(calendarId, hidden = isCurrentlyVisible)
            loadEvents()
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val zone = ZoneId.systemDefault()
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val visibleIds = visibleCalendarIds.value
            _events.value = repository.getEventsForDay(dayStart, dayEnd, visibleIds)
        }
    }

    /**
     * Look up a DeviceCalendar by ID. Used for resolving colors in the UI.
     */
    fun getCalendar(calendarId: Long): DeviceCalendar? {
        return _allCalendars.value.find { it.id == calendarId }
    }
}
