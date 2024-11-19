package dev.sudhanshu.calender.presentation.view

import android.content.Context
import retrofit2.Call
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import dev.sudhanshu.calender.presentation.view.model.Event
class FetchEvents (private val context: Context){

    fun getCurrentTimeRFC3339(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("MST")
        Log.d("EventSearch", "currentTime: ${dateFormat.format(Date())}")
        return dateFormat.format(Date())
    }

    fun setupCalendarRetrofit(accessToken: String): Call<GoogleCalendarResponse>{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(CalendarService::class.java)
        Log.d("Reminder","calendar service $service")


        Log.d("Reminder", "setupCalendar retrofit ${getCurrentTimeRFC3339()}")
        return service.getAllEvents(
            authorization = "Bearer $accessToken",
            timeMin = getCurrentTimeRFC3339(),
            orderBy = "startTime"
        )

    }

    fun fetchAllGoogleCalendarEvents(accessToken: String, onSuccess: (List<GoogleCalendarEvent>)->Unit, onError: (Int)->Unit){
        Log.d("Reminder", "enter fetchAllGoogleCalendarEvents $accessToken")
        val call: Call<GoogleCalendarResponse> = setupCalendarRetrofit(accessToken)
        call.enqueue(object: Callback<GoogleCalendarResponse> {
            override fun onResponse(
                call: Call<GoogleCalendarResponse>,
                response: Response<GoogleCalendarResponse>
            ){

                if(response.isSuccessful){
                    val eventResponses = response.body();
                    Log.d("Calendar Reminder","Got the events : $eventResponses")
                    eventResponses?.let{
                        eventList->
                        onSuccess(eventList.items)
                    }?: run{
                        onError(-1)
                    }
                }
                else{
                    Log.e("Calendar Reminder", "Error fetching an event")
                    onError(response.code())
                }

            }
            override fun onFailure(call: Call<GoogleCalendarResponse>, t: Throwable){
                Log.e("Calendar Reminder", "Error: ${t.message}")
                onError(-1)
            }

        })
    }
    suspend fun fetchEventsFromCloud(accessToken: String, eventMap: MutableMap<String, Event>): Unit =
        suspendCancellableCoroutine { continuation ->
            if (accessToken != null) {

                Log.d("Reminder EventScheduler", "Start Fetching...")
                fetchAllGoogleCalendarEvents(
                    accessToken,
                    onSuccess = { response ->
                        response.forEach { event ->
                            val eventId = event.id ?: "unknown_id"
                            val eventStart = event.start.dateTime ?: "unknown_start_time"  // Assuming `dateTime` is a property of `EventDateTime`
                            val eventEnd = event.end.dateTime ?: "unknown_end_time"
                            val eventSummary = event.summary ?: "No Summary"
                            val eventLink = event.hangoutLink ?: "No Link"
                            Log.d("Reminder EventScheduler", ">>Event ID: $eventId, Start time: $eventStart, Meet Link: $eventLink")

                            if (!eventMap.containsKey(eventId)) {
                                val newEvent = Event(eventId = eventId, eventName = eventSummary, eventStart = eventStart, eventEnd = eventEnd, eventLink = eventLink)
                                eventMap[eventId] = newEvent
                            }
                        }
                        // Resume the coroutine successfully
                        continuation.resume(Unit)
                    },
                    onError = { errorCode ->
                        Log.e("Reminder EventScheduler", "Error fetching events: Error code: $errorCode")
                        // Resume with an exception on error
                        continuation.resumeWithException(Exception("Error fetching events: $errorCode"))
                    }
                )

            } else {
                // If myAccessToken is null, resume with an exception
                continuation.resumeWithException(IllegalStateException("Access token is null"))
            }
        }

    data class GoogleCalendarResponse(
        val items: List<GoogleCalendarEvent>
    )


    interface CalendarService{
        @GET("/calendar/v3/calendars/primary/events")
        fun getAllEvents(
            @Header("Authorization") authorization: String,
            @Query("timeMin") timeMin: String,
            @Query("orderBy") orderBy: String = "startTime",
            @Query("singleEvents") singleEvents: Boolean = true,
        ): Call<GoogleCalendarResponse>

    }


}

