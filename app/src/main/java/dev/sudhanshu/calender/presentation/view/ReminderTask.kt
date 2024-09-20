package dev.sudhanshu.calender.presentation.view

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.presentation.view.GoogleSignInHelper.CalendarService
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

data class CalendarEventListResponse(
    val items: List<CalendarEvent> // List of events
)

data class CalendarEvent(
    val id: String,
    val summary: String?,
    val description: String?,
    val start: EventDateTime,
    val end: EventDateTime
)

data class EventDateTime(
    val dateTime: String?,  // This could be null if it's an all-day event (using "date" field instead)
    val timeZone: String?
)


interface CalendarApi{
    @GET("/calendar/v3/calendars/primary/events")
    fun getCalendarEvents(
        @Header("Authorization") authHeader: String
    ): Call<CalendarEventListResponse>
}




fun getRetrofitInstance(accessToken: String){
    val BASE_URL = "https://www.googleapis.com/"
    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(CalendarApi::class.java)
    val call = service.getCalendarEvents(authHeader=accessToken)
    call.enqueue(object: Callback<CalendarEventListResponse>{
        override fun onResponse(
            call: Call<CalendarEventListResponse>,
            response: Response<CalendarEventListResponse>
        ){
            if(response.isSuccessful){
                val eventResponse = response.body()
                Log.d("Reminder", "eventResponse: ${eventResponse}")
                onSuccess(eventResponse?.items)
            }
            else{
                Log.e("Reminder", "Error: ${response.code()}")
            }
        }
        override fun onFailure(call: Call<CalendarEventListResponse>, t: Throwable) {
            // Handle the error, such as network failure or other exceptions
            Log.e("Reminder", "Error fetching events: ${t.message}")
        }
    })
}
fun onSuccess(events: List<CalendarEvent>?) {
    events?.forEach { event ->
        Log.d("Reminder", "Event ID: ${event.id}, Summary: ${event.summary}")
        // Handle each event here
    }
}



