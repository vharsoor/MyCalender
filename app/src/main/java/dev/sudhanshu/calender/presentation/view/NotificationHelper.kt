package dev.sudhanshu.calender.presentation.view

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


class NotificationHelper (private val context: Context){

    private val channelID = "be908f56-a08e-4de3-8bbc-734c73e066fa"
    private val channelName = "MyChannel"

    init{
        createNotificationChannel()
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(calendarEvent: GoogleCalendarEvent){
        val startTime = calendarEvent.start.dateTime
        val timeZone = calendarEvent.start.timeZone
        val title = calendarEvent.summary
        val message = ""
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // alarmManager to schedule notification
        val intent = Intent(context, NotificationReceiver::class.java).apply{
            putExtra("title", title)
            putExtra("message", "empty message")
        }
        // security : set PendingIntent FLAG_IMMUTABLE or FLAG_MUTABLE >= sdk 3
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val scheduledAt = dateTimeStringToMills(startTime, timeZone)
//        val scheduledAt =  System.currentTimeMillis() + 60000
        Log.d("Notification", "scheduleAt $scheduledAt")
        if(scheduledAt == null){
            Log.e("Notification", "Fail to call alarmManager due to invalid date/time")
            return
        }
        // check if app can schedule exact alarms
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if(alarmManager.canScheduleExactAlarms()){ // requires to call setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledAt, pendingIntent)
                Log.d("Notification", "notification is scheduled, please wait ... ")
            }
            else{
                Log.e("Notification", "Cannot schedule exact alarms. Please grant permission.")
                //requestAlarmPermission()
            }

        }
        else{ // sdk < 12
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledAt, pendingIntent)
        }

    }



}