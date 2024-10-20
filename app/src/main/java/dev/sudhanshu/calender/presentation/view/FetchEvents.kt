package dev.sudhanshu.calender.presentation.view

import android.content.Context
import retrofit2.Call
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FetchEvents (private val context: Context){
    fun setupCalendarRetrofit(accessToken: String): Call<GoogleCalendarResponse>{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(CalendarService::class.java)
        Log.d("Reminder","calendar service $service")

        //val timeMin = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
        val timeMin = ZonedDateTime.now().minusMonths(1).format(DateTimeFormatter.ISO_INSTANT)
        Log.d("Reminder", "setupCalendar retrofit $timeMin")
        return service.getAllEvents(
            authorization = "Bearer $accessToken",
            timeMin = timeMin
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

