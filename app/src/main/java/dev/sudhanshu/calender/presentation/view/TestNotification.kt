package dev.sudhanshu.calender.presentation.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.util.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestNotification : ComponentActivity(){
    private lateinit var settingsPreferences: SettingsPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsPreferences = SettingsPreferences.getInstance(this)
        setContent{
            NotificationScreen(settingsPreferences)
        }

        requestAlarmPermission()
        createNotificationChannel(this)
    }

    @Composable
    fun NotificationScreen(settingsPreferences: SettingsPreferences){
        val context = LocalContext.current
        var hasNotificationPermission by remember {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            else{
                mutableStateOf(true)
            }
        }
        Log.d("Notification", "hasNotificationPermission: $hasNotificationPermission")
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {
                    isGranted->
                hasNotificationPermission = isGranted
            }
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Button( onClick ={
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    Log.d("Notification", "Request Post Notification Permission")
                }

            }){
                Text(text = "Request Permission")
            }
            Button( onClick ={
                if(hasNotificationPermission){
                    showNotification(context) //(context as MainActivity).showNotification(context)
                }
                else{
                    Log.d("Notification", "Permission not granted")
                }

            }){
                Text(text = "Show Notification")
            }
        }
    }

    private fun createNotificationChannel(context: Context){
        val channelID = "123"
        val channelName = "MyChannel"
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, channelName, importance).apply {
                description = "My notification channel description"
                enableVibration(true)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            // listing all the channels I got
            val existingChannels = notificationManager.notificationChannels
            existingChannels.forEach{channel->
                Log.d("Notification",
                "existing channels ${channel.id}, Name: ${channel.name}")}

        }

    }
    fun showNotification(context: Context){
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val fullScreenIntent = Intent(context, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // turn off the do not disturb mode && unpin the app
        val notification = NotificationCompat.Builder(this, "123")
            .setContentText("This is some content text")
            .setContentTitle("Hello World")
            .setDefaults(Notification.DEFAULT_ALL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1, notification) // show the default notification card

        Log.d("Notification", "show notification")
    }
    // sdk > 12 requires requesting alarm permission at runtime
    fun requestAlarmPermission(){
        AlertDialog.Builder(this)
            .setTitle("Request Exact Alarm Permission")
            .setMessage("This app needs permission to schedule exact alarms. Please grant permission")
            .setPositiveButton("Open Settings"){
                    _,_ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}