package dev.sudhanshu.calender.presentation.view

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class LockScreenReceiver: BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {

        when(intent.action){
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("LockScreenReceiver", "Screen Locked!")

            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("LockScreenReceiver", "Screen unlocked!")
                // GoogleSignIn get access token
                val googleSignInUnlock = GoogleSignInUnlock(context)
                googleSignInUnlock.checkRefreshToken()
            }
        }
    }


}


