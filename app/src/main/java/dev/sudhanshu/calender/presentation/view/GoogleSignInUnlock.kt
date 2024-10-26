package dev.sudhanshu.calender.presentation.view

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dev.sudhanshu.calender.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleSignInUnlock(context: Context) : AppCompatActivity() {

    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    val googleSignInHelper = GoogleSignInHelper(context)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: get the refresh token

    }
    fun googleSignInTime(){
        // Initialize the ActivityResultLauncher
        signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data = result.data
                Log.d("UnlockSignIn", "Sign-in result code: ${result.resultCode}")
                if (result.resultCode == RESULT_OK && data != null) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    Log.d("UnlockSignIn", "Result ok and Sign-in task: $task")
                    googleSignInHelper.handleSignInResult(
                        task,
                        ::onSignInSuccess,
                        ::onSignInError
                    )
                } else {
                    Log.e(
                        "UnlockSignIn",
                        "Sign-in failed with resultCode: ${result.resultCode}"
                    )
                    googleSignInTime()
                }
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("UnlockSignIn", "Signed in successfully: ${account.email}")
                } catch (e: ApiException) {
                    Log.e("UnlockSignIn", "Sign-in failed with error: ${e.statusCode}")
                    Log.e("UnlockSignIn", "Error details: ${e.localizedMessage}")
                }
            }

        googleSignInHelper.initiateGoogleSignIn(signInLauncher)

    }

    fun checkRefreshToken(){
        val refreshToken = googleSignInHelper.loadRefreshToken()

        if (refreshToken != null) {
            // Use the refresh token to get a new access token
            Log.d("UnlockSignIn", "Refresh token available, will get new access token $refreshToken")
            googleSignInHelper.getNewAccessTokenFromRefreshToken(
                refreshToken,
                ::onSignInSuccess,
                ::onSignInError
            )
        } else {
            // No refresh token available, initiate Google Sign-In
            Log.d("UnlockSignIn", "Logging in for the first time")
            googleSignInTime()
        }
    }

    private fun onSignInSuccess(accessToken: String) {
        Log.d("UnlockSignIn", "Sign-In success: $accessToken")
    }

    // Callback when sign-in fails
    private fun onSignInError(errorCode: Int) {
        Log.e("UnlockSignIn", "Sign-in failed with error code: $errorCode")
        // Handle error cases accordingly
    }
}