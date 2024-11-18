package dev.sudhanshu.calender.presentation.view

import android.content.Context

import android.app.Activity
import dev.sudhanshu.calender.R

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.identity.util.UUID
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
import java.io.Serializable

class InsertTask(private val context: Context) {

    fun insertEventToGoogleCalendar(
        accessToken: String,
        summary: String,
        startDateTime: String,
        endDateTime: String,
        createMeetLink: Boolean,  // New argument to control Google Meet link creation
        onSuccess: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CalendarService::class.java)

        // Build the event details, conditionally adding conferenceData for Meet link
        val event = GoogleCalendarEvent(
            summary = summary,
            start = EventDateTime(dateTime = startDateTime, timeZone = "MST"),  // Adjust timezone as needed
            end = EventDateTime(dateTime = endDateTime, timeZone = "MST"),
            conferenceData = if (createMeetLink) {
                ConferenceData(
                    createRequest = CreateConferenceRequest(
                        requestId = java.util.UUID.randomUUID().toString(),
                        conferenceSolutionKey = ConferenceSolutionKey(type = "hangoutsMeet")
                    )
                )
            } else {
                null
            }
        )

        // Make the API request
        val call = service.insertEvent(
            authorization = "Bearer $accessToken",
            event = event,
            conferenceDataVersion = if (createMeetLink) 1 else 0
        )

        Log.d("CalendarIntegration", "Event Details: $event")

        call.enqueue(object : Callback<GoogleCalendarEventResponse> {
            override fun onResponse(
                call: Call<GoogleCalendarEventResponse>,
                response: Response<GoogleCalendarEventResponse>
            ) {
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    val meetLink = eventResponse?.hangoutLink
                    Log.d("CalendarIntegration", "Event ID: ${eventResponse?.id}, Meet Link: $meetLink")
                    if (createMeetLink && meetLink != null) {
                        event.conferenceData = event.conferenceData?.createRequest?.let {
                            ConferenceData(
                                createRequest = it,
                                hangoutLink = meetLink
                            )
                        }
                    }
                    Log.d("CalendarIntegration", "Event ID: ${eventResponse?.id}, Meet Link: ${event.conferenceData?.hangoutLink}")
                    if (meetLink != null) {
                        onSuccess(meetLink)
                    }
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
            @Body event: GoogleCalendarEvent,
            @retrofit2.http.Query("conferenceDataVersion") conferenceDataVersion: Int = 1
        ): Call<GoogleCalendarEventResponse>
    }

    data class GoogleCalendarEvent(
        @SerializedName("summary") val summary: String,
        @SerializedName("start") val start: EventDateTime,
        @SerializedName("end") val end: EventDateTime,
        @SerializedName("conferenceData") var conferenceData: ConferenceData? = null
    )

    data class EventDateTime(
        @SerializedName("dateTime") val dateTime: String,
        @SerializedName("timeZone") val timeZone: String
    )

    data class ConferenceData(
        @SerializedName("createRequest") val createRequest: CreateConferenceRequest,
        @SerializedName("hangoutLink") val hangoutLink: String? = null
    )

    data class CreateConferenceRequest(
        @SerializedName("requestId") val requestId: String,
        @SerializedName("conferenceSolutionKey") val conferenceSolutionKey: ConferenceSolutionKey
    )

    data class ConferenceSolutionKey(
        @SerializedName("type") val type: String // "hangoutsMeet" for Google Meet
    )

    data class GoogleCalendarEventResponse(
        @SerializedName("id") val id: String,
        @SerializedName("status") val status: String,
        @SerializedName("hangoutLink") val hangoutLink: String? // Google Meet link
    )

}

@Composable
fun TaskTypeDialog(onDismiss: () -> Unit, onTaskTypeSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Task Type") },
        text = {
            Column {
                Button(onClick = { onTaskTypeSelected("InsertTask") }) {
                    Text("Schedule a Task/Reminder")
                }
                Button(onClick = {  }) {
                    Text("Schedule a Meeting")
                }
                Button(onClick = { onTaskTypeSelected("ShoppingList") }) {
                    Text("Shopping/Grocery List")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}