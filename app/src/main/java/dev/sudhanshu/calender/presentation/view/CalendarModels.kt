package dev.sudhanshu.calender.presentation.view

import com.google.gson.annotations.SerializedName

data class GoogleCalendarEvent(
    @SerializedName("id") val id: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("start") val start: EventDateTime,
    @SerializedName("end") val end: EventDateTime
)

data class EventDateTime(
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("timeZone") val timeZone: String
)


data class GoogleCalendarEventResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String
)