package dev.sudhanshu.calender.presentation.view

import android.content.Context

import android.app.Activity
import dev.sudhanshu.calender.R

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

class InsertTask(private val context: Context) {

    fun insertEventToGoogleCalendar(
        accessToken: String,
        summary: String,
        startDateTime: String,
        endDateTime: String,
        onSuccess: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CalendarService::class.java)

        // Build the event details
        val event = GoogleCalendarEvent(
            summary = summary,
            start = EventDateTime(dateTime = startDateTime, timeZone = "UTC"),  // Adjust timezone as needed
            end = EventDateTime(dateTime = endDateTime, timeZone = "UTC")
        )

        // Make the API request
        val call = service.insertEvent(
            authorization = "Bearer $accessToken",
            event = event
        )

        call.enqueue(object : Callback<GoogleCalendarEventResponse> {
            override fun onResponse(
                call: Call<GoogleCalendarEventResponse>,
                response: Response<GoogleCalendarEventResponse>
            ) {
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    Log.d("CalendarIntegration", "Event ID: ${eventResponse?.id}")
                    onSuccess(eventResponse?.id ?: "")
                } else {
                    Log.e("CalendarIntegration", "Error inserting event")
                    onError(response.code())
                }
            }

            override fun onFailure(call: Call<GoogleCalendarEventResponse>, t: Throwable) {
                Log.e("CalendarIntegration", "Error: ${t.message}")
                onError(-1)
            }
        })
    }


    interface CalendarService {
        @POST("/calendar/v3/calendars/primary/events")
        fun insertEvent(
            @Header("Authorization") authorization: String,
            @Body event: GoogleCalendarEvent
        ): Call<GoogleCalendarEventResponse>
    }

    data class GoogleCalendarEvent(
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

}