package com.vibecalendar.ui.dayview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibecalendar.model.DeviceCalendar
import com.vibecalendar.model.DisplayEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR_HEIGHT: Dp = 64.dp
private val TIME_LABEL_WIDTH: Dp = 56.dp
private val STRIPE_WIDTH: Dp = 4.dp
private val EVENT_LEFT_PADDING: Dp = 2.dp
private val SWIPE_THRESHOLD = 100f

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun DayView(
    date: LocalDate,
    events: List<DisplayEvent>,
    calendarsById: Map<Long, DeviceCalendar>,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allDayEvents = events.filter { it.allDay }
    val timedEvents = events.filter { !it.allDay }
    val scrollState = rememberScrollState()
    var cumulativeDrag by remember { mutableFloatStateOf(0f) }

    // Auto-scroll to current time on today
    val density = LocalDensity.current
    LaunchedEffect(date) {
        if (date == LocalDate.now()) {
            val currentHour = LocalTime.now().hour
            val targetPx = with(density) { (HOUR_HEIGHT * (currentHour - 1).coerceAtLeast(0)).toPx() }
            scrollState.scrollTo(targetPx.toInt())
        } else {
            val eightAmPx = with(density) { (HOUR_HEIGHT * 8).toPx() }
            scrollState.scrollTo(eightAmPx.toInt())
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // All-day events section
        if (allDayEvents.isNotEmpty()) {
            AllDaySection(
                events = allDayEvents,
                calendarsById = calendarsById,
            )
        }

        // Timed events grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(scrollState)
                .pointerInput(date) {
                    detectHorizontalDragGestures(
                        onDragStart = { cumulativeDrag = 0f },
                        onDragEnd = {
                            if (cumulativeDrag > SWIPE_THRESHOLD) onSwipeRight()
                            else if (cumulativeDrag < -SWIPE_THRESHOLD) onSwipeLeft()
                            cumulativeDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            cumulativeDrag += dragAmount
                        },
                    )
                },
        ) {
            TimeGrid()

            // Current time indicator
            if (date == LocalDate.now()) {
                CurrentTimeIndicator()
            }

            // Timed events
            timedEvents.forEach { event ->
                TimedEventCard(
                    event = event,
                    date = date,
                    calendarsById = calendarsById,
                )
            }
        }
    }
}

@Composable
private fun AllDaySection(
    events: List<DisplayEvent>,
    calendarsById: Map<Long, DeviceCalendar>,
) {
    Surface(tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            events.forEach { event ->
                EventChip(
                    event = event,
                    calendarsById = calendarsById,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TimeGrid() {
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        for (hour in 0..23) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HOUR_HEIGHT)
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 0.5f,
                        )
                    },
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = formatHourLabel(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(TIME_LABEL_WIDTH)
                        .padding(end = 8.dp, top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator() {
    val now = LocalTime.now()
    val fractionOfDay = now.hour + now.minute / 60f
    val color = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = HOUR_HEIGHT * fractionOfDay),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
            )
        }
        Canvas(modifier = Modifier.padding(start = TIME_LABEL_WIDTH - 4.dp)) {
            drawCircle(color = color, radius = 5f)
        }
    }
}

@Composable
private fun TimedEventCard(
    event: DisplayEvent,
    date: LocalDate,
    calendarsById: Map<Long, DeviceCalendar>,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    // Clamp event to the visible day
    val clampedStart = event.startMillis.coerceIn(dayStart, dayEnd)
    val clampedEnd = event.endMillis.coerceIn(dayStart, dayEnd)

    val startHourFraction = ((clampedStart - dayStart).toFloat() / 3_600_000f)
    val durationHours = ((clampedEnd - clampedStart).toFloat() / 3_600_000f).coerceAtLeast(0.25f)

    val topOffset = HOUR_HEIGHT * startHourFraction
    val eventHeight = HOUR_HEIGHT * durationHours

    Box(
        modifier = Modifier
            .offset(y = topOffset)
            .padding(start = TIME_LABEL_WIDTH + EVENT_LEFT_PADDING)
            .fillMaxWidth()
            .height(eventHeight)
            .padding(end = 8.dp, bottom = 1.dp),
    ) {
        EventChip(
            event = event,
            calendarsById = calendarsById,
            modifier = Modifier.fillMaxSize(),
            showTime = true,
        )
    }
}

@Composable
fun EventChip(
    event: DisplayEvent,
    calendarsById: Map<Long, DeviceCalendar>,
    modifier: Modifier = Modifier,
    showTime: Boolean = false,
) {
    val calendarColors = event.calendarIds.mapNotNull { calendarsById[it]?.color }
    val backgroundColor = calendarColors.firstOrNull()?.copy(alpha = 0.15f)
        ?: MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable { /* future: event detail */ },
    ) {
        // Multi-calendar color stripes
        CalendarStripes(
            colors = calendarColors,
            modifier = Modifier.width(STRIPE_WIDTH * calendarColors.size.coerceAtLeast(1)),
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = calendarColors.firstOrNull() ?: MaterialTheme.colorScheme.onSurface,
            )
            if (showTime) {
                val zone = ZoneId.systemDefault()
                val startTime = Instant.ofEpochMilli(event.startMillis).atZone(zone).toLocalTime()
                val endTime = Instant.ofEpochMilli(event.endMillis).atZone(zone).toLocalTime()
                Text(
                    text = "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarStripes(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    if (colors.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val stripeWidthPx = size.width / colors.size
        colors.forEachIndexed { index, color ->
            drawRect(
                color = color,
                topLeft = Offset(stripeWidthPx * index, 0f),
                size = androidx.compose.ui.geometry.Size(stripeWidthPx, size.height),
            )
        }
    }
}

private fun formatHourLabel(hour: Int): String {
    return when (hour) {
        0 -> "12 AM"
        in 1..11 -> "$hour AM"
        12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
