package dev.sudhanshu.calender.presentation.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log


class ReminderScheduler(private val context: Context){
    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis: Long = 20 * 60 * 60 * 1000 // 20 min
    val fetchEvents = FetchEvents(context)
    val eventMap: MutableMap<String, MutableMap<EventDateTime, GoogleCalendarEvent>> = mutableMapOf()

    private val reminderRunnable = object: Runnable{
        override fun run(){
            val accessToken = GoogleSignInHelper.accesstoken
            if(accessToken != null){
                Log.d("Reminder EventScheduler", "fetching events...")


                fetchEvents.fetchAllGoogleCalendarEvents(accessToken,
                    onSuccess = {events ->
                        events.forEach{
                            event ->
                            val eventId = event.id?:"unknown_id"
                            val eventStart = event.start?:"unknown_start_time"
                            if(!eventMap.containsKey(eventId)){
                                eventMap[eventId] = mutableMapOf()
                            }

                            eventMap[eventId]?.put(eventStart as EventDateTime, event)

                            eventMap.forEach{
                                (id, start) ->
                                Log.d("Reminder EventScheduler", "Event ID: $id, start time: $start")
                            }
                        }
                    },
                    onError = { errorCode ->
                        Log.e("Reminder EventScheduler", "Error fetching events: Error code: $errorCode")
                    }
                )
                // setup alarms

            }
            else{
                Log.e("Reminder EventScheduler", "accessToken is null")
            }
            Log.d("Reminder EventScheduler", "scheduling reminders for events")
            handler.postDelayed(this, intervalMillis) // this == runnable
        }
    }
    fun startTracking(){
        handler.post(reminderRunnable)
    }

    fun stopTracking(){
        handler.removeCallbacks(reminderRunnable)
    }
}