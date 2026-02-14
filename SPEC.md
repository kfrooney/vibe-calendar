# Vibe Calendar - Product Spec (v1)

## Overview

An Android calendar viewer built on CalendarContract that displays events from all
device-synced calendars with intelligent deduplication. Events sharing the same
iCalendar UID across multiple calendars are displayed once, with color-coded
stripes indicating which calendars contain the event.

## Target Platform

- Android (minSdk 26 / Android 8.0)
- Kotlin
- Jetpack Compose + Material 3

## Data Source

- Android CalendarContract content provider
- Permission: `READ_CALENDAR`

## Core Features (v1)

### 1. Day View

- Full-day time grid (24h, scrollable)
- All-day events shown in a top section
- Timed events positioned by start/end time
- Current time indicator line
- Swipe or button navigation between days

### 2. Event Deduplication

- Events are grouped by iCalendar UID (`Events.UID_2445`)
- Events with matching UIDs are displayed as a single visual entry
- The displayed event uses merged data (title, time, location from the first instance)
- Events without UIDs are never deduplicated

### 3. Multi-Calendar Color Stripes

- Each event card has a vertical stripe region on its left edge
- Single-calendar events: one solid stripe in that calendar's color
- Deduplicated events: stacked/segmented stripes, one per calendar the event appears on
- Tapping a deduplicated event shows which calendars it belongs to

### 4. Calendar Selection

- Drawer or bottom sheet listing all calendars grouped by account
- Toggle individual calendar visibility
- Each calendar shows its display name and color
- Selection persists across app restarts

## Out of Scope (v1)

- Event creation, editing, deletion
- Agenda, week, month views
- Widgets
- Notifications / reminders
- Search
- Settings beyond calendar visibility

## Future Iterations

- v2: Week and month views
- v3: Event creation and editing (read-write)
- v4: Agenda view, search, widgets
