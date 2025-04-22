/**
 * GoogleCalendarEventModels.kt
 *
 * This file defines data models and utilities for interacting with Google Calendar events.
 *
 * Main Components:
 * - `GoogleCalendarEvent`: Represents a calendar event with ID, summary, start/end time, and optional meet link.
 * - `EventDateTime`: Encapsulates event start/end time in RFC 3339 format with time zone.
 * - `GoogleCalendarEventResponse`: Holds the response returned after inserting a calendar event.
 * - `dateTimeStringToMills`: Converts a timeZone string (ISO_ZONED_DATE_TIME format) to epoch milliseconds.
 *
 * Used for parsing JSON responses and preparing requests to/from the Google Calendar API.
 */



package dev.sudhanshu.calender.presentation.view

import android.util.Log
import com.google.gson.annotations.SerializedName
import dev.sudhanshu.calender.presentation.view.InsertTask.ConferenceData
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.io.Serializable

data class GoogleCalendarEvent(
    @SerializedName("id") val id: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("start") val start: EventDateTime,
    @SerializedName("end") val end: EventDateTime,
    @SerializedName("hangoutLink") val hangoutLink: String? = null
)

data class EventDateTime(
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("timeZone") val timeZone: String
)


data class GoogleCalendarEventResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String
)

fun dateTimeStringToMills(dateTime: String, timeZone: String):Long?{
    try{
        val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        val zonedDateTime = ZonedDateTime.parse(
            timeZone, formatter
        )
        return zonedDateTime.toInstant().toEpochMilli()

    }catch(e: DateTimeParseException){
        Log.e("Notification", "Invalid dateTime format: ${e.message}")
        return null
    }
    catch(e: Exception){
        Log.e("Notification", "${e.message}")
        return null
    }

}