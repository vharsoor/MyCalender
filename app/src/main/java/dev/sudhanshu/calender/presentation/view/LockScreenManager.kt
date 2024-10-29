package dev.sudhanshu.calender.presentation.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LockScreenReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("LockScreenReceiver", "Screen Locked!")
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("LockScreenReceiver", "Screen unlocked!")
            }
        }
    }
}


