package dev.sudhanshu.calender.presentation.view


import android.content.Context
import android.util.Log
import dev.sudhanshu.calendar.presentation.view.EventAttendee
import dev.sudhanshu.calendar.presentation.view.Meeting
import dev.sudhanshu.calendar.presentation.view.Meeting_Event
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import dev.sudhanshu.calender.presentation.view.model.Event

// Retrofit API Interface for Calendar Service
interface CalendarService {
    @POST("calendar/v3/calendars/{calendarId}/events")
    fun createEvent(
        @Path("calendarId") calendarId: String,
        @Header("Authorization") authHeader: String,
        @Body event: Meeting_Event
    ): Call<Event>
}

class MeetingScheduler(private val context: Context) {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val calendarService = retrofit.create(CalendarService::class.java)

    fun scheduleMeeting(
        accessToken: String,
        meeting: Meeting,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val eventDateTime = EventDateTime(meeting.dateTime, meeting.timeZone)
        val event = Meeting_Event(
            event_name = meeting.headline,
            description = meeting.agenda,
            event_start = eventDateTime,
            event_end = eventDateTime,
            attendees = meeting.inviteeEmails.map { EventAttendee(it) }
        )

        val call = calendarService.createEvent(
            calendarId = "primary",
            authHeader = "Bearer $accessToken",
            event = event
        )

        call.enqueue(object : Callback<Event> {
            override fun onResponse(call: Call<Event>, response: Response<Event>) {
                if (response.isSuccessful) {
                    onSuccess()
                    Log.d("MeetingScheduler", "Meeting scheduled successfully.")
                } else {
                    onError("Error scheduling meeting: ${response.errorBody()?.string()}")
                    Log.e("MeetingScheduler", "Error scheduling meeting: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Event>, t: Throwable) {
                onError("Network error: ${t.message}")
                Log.e("MeetingScheduler", "Network error: ${t.message}")
            }
        })
    }
}
