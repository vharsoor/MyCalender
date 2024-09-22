package dev.sudhanshu.calender.presentation.view

import android.content.Context
import retrofit2.Call
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import dev.sudhanshu.calender.presentation.view.GoogleCalendarEventResponse as GoogleCalendarEventResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FetchEvents (private val context: Context){
    fun setupCalendarRetrofit(accessToken: String): Call<List<GoogleCalendarEvent>>{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(CalendarService::class.java)

        val timeMin = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
        Log.d("Reminder", "setupCalendar retrofit")
        return service.getAllEvents(
            authorization = "Bearer $accessToken",
            timeMin = timeMin
        )

    }

    fun fetchAllGoogleCalendarEvents(accessToken: String, onSuccess: (List<GoogleCalendarEvent>)->Unit, onError: (Int)->Unit){
        Log.d("Reminder", "enter fetchAllGoogleCalendarEvents $accessToken")
        val call: Call<List<GoogleCalendarEvent>> = setupCalendarRetrofit(accessToken)
        call.enqueue(object: Callback<List<GoogleCalendarEvent>> {
            override fun onResponse(
                call: Call<List<GoogleCalendarEvent>>,
                response: Response<List<GoogleCalendarEvent>>
            ){

                if(response.isSuccessful){
                    val eventResponses = response.body();
                    eventResponses?.let{
                        eventList->
                        onSuccess(eventList)
                    }?: run{
                        onError(-1)
                    }
                }
                else{
                    Log.e("Calendar Reminder", "Error fetching an event")
                    onError(response.code())
                }

            }
            override fun onFailure(call: Call<List<GoogleCalendarEvent>>, t: Throwable){
                Log.e("Calendar Reminder", "Error: ${t.message}")
                onError(-1)
            }

        })
    }

    interface CalendarService{
        @GET("/calendar/v2/calendars/primary/events")
        fun getAllEvents(
            @Header("Authorization") authorization: String,
            @Query("timeMin") timeMin: String,
            @Query("orderBy") orderBy: String = "startTime",
            @Query("singleEvents") singleEvents: Boolean = true,
        ): Call<List<GoogleCalendarEvent>>
    }


}

