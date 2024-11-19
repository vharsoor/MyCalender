package dev.sudhanshu.calender.presentation.view

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

class EventScheduler : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("eventTitle")
        val eventLink = intent.getStringExtra("eventLink")
        Log.d("Reminder", "Received reminder for in alarm receiever, event link is: $eventLink")
        //eventTitle?.let {
            //showToast(context, "Reminder: $it is starting soon!")
        //}
        val intent = Intent("dev.sudhanshu.calender.REMINDER_EVENT").apply {
            putExtra("eventTitle", eventTitle)
            putExtra("eventLink", intent.getStringExtra("eventLink"))
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        //LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    // Fetches and schedules reminders
    suspend fun fetchAndScheduleAllReminders(context: Context, accessToken: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val eventMap: MutableMap<String, dev.sudhanshu.calender.presentation.view.model.Event> = mutableMapOf()

        val fetchEvents = FetchEvents(context)
        CoroutineScope(Dispatchers.IO).launch {
            val result1 = withContext(Dispatchers.IO) {
                fetchEvents.fetchEventsFromCloud(accessToken, eventMap)
            }

            eventMap.values.forEach { event ->
                val eventTimeMillis = event.eventStart?.let { convertRFC3339ToMillis(it) } ?: return@forEach
                if (eventTimeMillis > System.currentTimeMillis()) {
                    scheduleEventReminder(context, alarmManager, event, eventTimeMillis)
                }
            }
        }
    }

    private fun scheduleEventReminder(
        context: Context,
        alarmManager: AlarmManager,
        event: dev.sudhanshu.calender.presentation.view.model.Event,
        eventTimeMillis: Long
    ) {
        val requestCode = event.eventId.hashCode()
        val reminderTime = eventTimeMillis - TimeUnit.MINUTES.toMillis(10)

        Log.d("Reminder", "Scheduled reminder for ${event.eventLink}")
        val gson = Gson()
        val eventLinkJson = gson.toJson(event.eventLink)

        val intent = Intent(context, EventScheduler::class.java).apply {
            putExtra("eventTitle", event.eventName)
            putExtra("eventId", event.eventId)
            putExtra("eventLink", eventLinkJson)
            Log.d("Reminder", "Future eventLink: $eventLinkJson")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            //PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )

        Log.d("Reminder", "Scheduled reminder for ${event.eventName} at $reminderTime")
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun convertRFC3339ToMillis(rfc3339: String): Long? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            formatter.parse(rfc3339)?.time
        } catch (e: Exception) {
            Log.e("EventScheduler", "Error parsing date: $rfc3339")
            null
        }
    }

    data class Event(val eventId: String, val eventName: String, val eventStart: String)
}

