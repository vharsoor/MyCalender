package dev.sudhanshu.calender.presentation.view

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.content.BroadcastReceiver
import android.util.Log
import dev.sudhanshu.calender.R




class NotificationReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")?:"Notification"
        val message = intent.getStringExtra("message")?:"You have a new notification"
        Log.d("Notification", "onReceive called with title: $title, message: $message")
        showNotification(context, title, message)

    }

    private fun showNotification(context: Context, title: String, message: String){
        Log.d("Notification", "Running showNotification ... ")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager == null) {
            Log.e("NotificationReceiver", "NotificationManager is null")
            return
        }
        val channelID = "123"

        val existingChannels = notificationManager.notificationChannels
        existingChannels.forEach{channel->Log.d("Notification",
            "existing channels ${channel.id}, Name: ${channel.name}")}



        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()


        Log.d("Notification", "Notification is built! $notification")
        notificationManager.notify(1, notification)

        Log.d("Notification", "Notified with Title $title and  Message: $message")
    }

}
