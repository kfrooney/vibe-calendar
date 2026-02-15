package com.vibecalendar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibecalendar.ui.calendar.CalendarDrawerContent
import com.vibecalendar.ui.dayview.DayView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeCalendarApp(viewModel: CalendarViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val events by viewModel.events.collectAsState()
    val allCalendars by viewModel.allCalendars.collectAsState()
    val visibleCalendarIds by viewModel.visibleCalendarIds.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val calendarsById = allCalendars.associateBy { it.id }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                CalendarDrawerContent(
                    calendars = allCalendars,
                    visibleCalendarIds = visibleCalendarIds,
                    onToggleCalendar = { viewModel.toggleCalendar(it) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        DayNavigationHeader(
                            date = selectedDate,
                            onPrevious = { viewModel.goToPreviousDay() },
                            onNext = { viewModel.goToNextDay() },
                            onToday = { viewModel.goToToday() },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Calendars",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            DayView(
                date = selectedDate,
                events = events,
                calendarsById = calendarsById,
                onSwipeLeft = { viewModel.goToNextDay() },
                onSwipeRight = { viewModel.goToPreviousDay() },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun DayNavigationHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = date.format(dateHeaderFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
            )
        }

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }

        if (date != LocalDate.now()) {
            IconButton(onClick = onToday) {
                Icon(Icons.Default.Today, contentDescription = "Go to today")
            }
        }
    }
}
