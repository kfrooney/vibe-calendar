package com.vibecalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vibecalendar.model.DeviceCalendar

@Composable
fun CalendarDrawerContent(
    calendars: List<DeviceCalendar>,
    visibleCalendarIds: Set<Long>,
    onToggleCalendar: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Calendars",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        val grouped = calendars.groupBy { it.accountName }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            grouped.forEach { (accountName, accountCalendars) ->
                item {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }

                items(accountCalendars, key = { it.id }) { calendar ->
                    CalendarToggleRow(
                        calendar = calendar,
                        isVisible = calendar.id in visibleCalendarIds,
                        onToggle = { onToggleCalendar(calendar.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarToggleRow(
    calendar: DeviceCalendar,
    isVisible: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Checkbox(
            checked = isVisible,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = calendar.color,
            ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(calendar.color),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = calendar.displayName,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
