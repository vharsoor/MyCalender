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
            //val accessToken = GoogleSignInHelper.accesstoken
            val accessToken = "ya29.a0AcM612yWhndV8PSt1LuXM6Jpza3Z_MplfrWv7RSYSeO80D1vK6dgYiLpmmK5Xk2MoX3KhztalfkdqOsEVpaJ8t-TdqyzRst7cxCJlENN04I65U9QJS1F5QODpxhltnJAvk27origpz6AKiZXPeE3fjKcJzvWpwrpKhTKwIRDaCgYKAZoSARISFQHGX2Mi2JB6PIk_74-aSkLlqYSozQ0175"
            Log.d("Reminder EventScheduler", "Fetched accesstoken : $accessToken")
            if(accessToken != null){
                Log.d("Reminder EventScheduler", "fetching events...")


                fetchEvents.fetchAllGoogleCalendarEvents(accessToken,
                    onSuccess = { events ->
                        events.forEach { event ->
                            val eventId = event.id ?: "unknown_id"
                            val eventStart = event.start?.dateTime ?: "unknown_start_time"  // Assuming `dateTime` is a property of `EventDateTime`

                            if (!eventMap.containsKey(eventId)) {
                                eventMap[eventId] = mutableMapOf()
                            }

                            // Add the event using the start time as the key
                            event.start?.let { startTime ->
                                eventMap[eventId]?.put(startTime, event)
                            }

                            Log.d("Reminder EventScheduler","fetched event summary : $event.summary")
                            // Log each event in the eventMap
                            eventMap.forEach { (id, startTimeMap) ->
                                startTimeMap.forEach { (start, evt) ->
                                    Log.d("Reminder EventScheduler", "Event ID: $id, Start time: ${start.dateTime}, Event: $evt")
                                }
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