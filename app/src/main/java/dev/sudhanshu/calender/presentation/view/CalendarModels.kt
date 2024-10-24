package dev.sudhanshu.calender.presentation.view

import android.util.Log
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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