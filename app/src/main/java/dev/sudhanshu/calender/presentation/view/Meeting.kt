// File: Meeting.kt
package dev.sudhanshu.calendar.presentation.view

import com.google.gson.annotations.SerializedName
import dev.sudhanshu.calender.presentation.view.EventDateTime

data class Meeting(
    val headline: String,
    val dateTime: String, // ISO 8601 format, e.g., "2024-12-20T17:00:00-07:00"
    val timeZone: String = "America/Phoenix", // Default timezone
    val agenda: String,
    val inviteeEmails: List<String>
)

data class Meeting_Event(
    val event_name: String, // Meeting headline
    val description: String, // Meeting agenda
    val event_start: EventDateTime,
    val event_end: EventDateTime,
    val attendees: List<EventAttendee>
)

data class EventDateTime(
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("timeZone") val timeZone: String
)

data class EventAttendee(
    @SerializedName("email") val email: String
)
