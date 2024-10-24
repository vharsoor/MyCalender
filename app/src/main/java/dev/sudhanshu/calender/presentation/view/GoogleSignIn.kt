package dev.sudhanshu.calender.presentation.view

import android.Manifest
import android.app.Activity
import dev.sudhanshu.calender.R

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.sudhanshu.calender.presentation.view.GoogleSignInHelper.Companion.calendarID
import java.net.HttpURLConnection
import java.net.URL


private const val PREFS_NAME = "GoogleSignInPrefs"
private const val REFRESH_TOKEN_KEY = "refresh_token"

class GoogleSignInHelper(private val context: Context) {

    private var mGoogleSignInClient: GoogleSignInClient? = null


    // Function to start Google Sign-In
    fun initiateGoogleSignIn(signInLauncher: ActivityResultLauncher<Intent>) {
        Log.d("CalendarIntegration", "Inside initiateGoogleSignIn")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestServerAuthCode(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()

        Log.d("CalendarIntegration", "Built gso : $gso")
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        Log.d("CalendarIntegration", "mGoogleSignInClient : $mGoogleSignInClient")

        val signInIntent = mGoogleSignInClient?.signInIntent
        Log.d("CalendarIntegration", "signInIntent : $signInIntent")
        if (signInIntent != null) {
            Log.d("CalendarIntegration", "Launching sign in intent")
            signInLauncher.launch(signInIntent)
        }
    }

    // Handle sign-in result
    fun handleSignInResult(task: Task<GoogleSignInAccount>, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("CalendarIntegration", "Got account in handleSignInResult: $account")
            val authCode = account?.serverAuthCode
            Log.d("CalendarIntegration", "authCode: $authCode")
            if (authCode != null) {
                Log.w("CalendarIntegration", "authCode: $authCode")
                getAccessToken(authCode, onSuccess, onError)
            }
        } catch (e: ApiException) {
            onError(e.statusCode)
            Log.e("CalendarIntegration", "signInResult:failed code=${e.statusCode}")
        }
    }

    // Get the refresh token using the authCode
    private fun getAccessToken(authCode: String, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com?access_type=offline?prompt=consent")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OAuthService::class.java)
        val call = service.getToken(
            code = authCode,
            clientId = context.getString(R.string.web_client_id),
            clientSecret = context.getString(R.string.client_secret),
            redirectUri = "",
            grantType = "authorization_code"
            //access_type = "offline",
            //prompt = "consent"
        )

        call.enqueue(object : Callback<GoogleTokenResponse> {
            override fun onResponse(call: Call<GoogleTokenResponse>, response: Response<GoogleTokenResponse>) {
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    Log.d("CalendarIntegration", "Access Token: ${tokenResponse?.accessToken}")
                    accesstoken = tokenResponse?.accessToken
                    saveRefreshToken(tokenResponse?.refreshToken ?: "")
                    Log.d("CalendarIntegration", "Refresh Token: ${tokenResponse?.refreshToken}")
                    fetchPrimaryCalendarId("${tokenResponse?.accessToken}")
                    onSuccess(tokenResponse?.accessToken ?: "")
                } else {
                    Log.e("CalendarIntegration", "Error in token response")
                    onError(response.code())
                }
            }

            override fun onFailure(call: Call<GoogleTokenResponse>, t: Throwable) {
                Log.e("CalendarIntegration", "Error fetching token: ${t.message}")
                onError(-1)
            }
        })
    }

    // Function to save refresh token securely
    fun saveRefreshToken(refreshToken: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(REFRESH_TOKEN_KEY, refreshToken)
            apply()
        }
    }

    // Function to load refresh token
    fun loadRefreshToken(): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }

    fun clearRefreshToken() {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(REFRESH_TOKEN_KEY)
            apply()
        }
    }

    fun getNewAccessTokenFromRefreshToken(
        refreshToken: String,
        onSuccess: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        Log.d("CalendarIntegration", "Retrofit instance created $retrofit")
        val service = retrofit.create(OAuthService::class.java)
        Log.d("CalendarIntegration", "Service instance created $service")
        val call = service.refreshToken(
            refreshToken = refreshToken,
            clientId = context.getString(R.string.web_client_id),
            clientSecret = context.getString(R.string.client_secret),
            grantType = "refresh_token"
        )
        Log.d("CalendarIntegration", "Call instance created $call")

        call.enqueue(object : Callback<GoogleTokenResponse> {
            override fun onResponse(call: Call<GoogleTokenResponse>, response: Response<GoogleTokenResponse>) {
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    Log.d("CalendarIntegration", "New Access Token: ${tokenResponse?.accessToken}")
                    accesstoken = tokenResponse?.accessToken
                    onSuccess(tokenResponse?.accessToken ?: "")
                } else {
                    Log.e("CalendarIntegration", "Error in token response")
                    onError(response.code())
                }
            }

            override fun onFailure(call: Call<GoogleTokenResponse>, t: Throwable) {
                Log.e("CalendarIntegration", "Error fetching token: ${t.message}")
                onError(-1)
            }
        })
    }

    fun fetchPrimaryCalendarId(accessToken: String?) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val calendarService = retrofit.create(CalendarService::class.java)
        Log.d("FCM Token","access : $accesstoken")

        // Make API call to fetch the calendar list
        val call = calendarService.getCalendarList("Bearer $accesstoken")
        call.enqueue(object : Callback<CalendarListResponse> {
            override fun onResponse(
                call: Call<CalendarListResponse>,
                response: Response<CalendarListResponse>
            ) {
                if (response.isSuccessful) {
                    val calendarList = response.body()
                    val primaryCalendar = calendarList?.items?.find { it.primary }
                    primaryCalendar?.let {
                        Log.d("FCM Token", "Primary calendar ID: ${it.id}")
                        calendarID = it.id
                        val myFirebaseMessagingService = MyFirebaseMessagingService()
                        Log.d("FCM Token", "Calling in between the calendar and token")
                        myFirebaseMessagingService.sendTokenToServer(MyFirebaseMessagingService.fcmtoken, "$calendarID")
                    } ?: Log.d("FCM Token", "Primary calendar not found")
                } else {
                    Log.d("FCM Token", "Failed to fetch calendars: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<CalendarListResponse>, t: Throwable) {
                Log.d("FCM Token", "Failed to fetch calendars: ${t.message}")
            }
        })

    }



    // Define the OAuth service interface for token exchange
    interface OAuthService {
        @FormUrlEncoded
        @POST("/token")
        fun getToken(
            @Field("code") code: String,
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("redirect_uri") redirectUri: String,
            @Field("grant_type") grantType: String
        ): Call<GoogleTokenResponse>

        @FormUrlEncoded
        @POST("/token")
        fun refreshToken(
            @Field("refresh_token") refreshToken: String,
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("grant_type") grantType: String
        ): Call<GoogleTokenResponse>
    }

    interface CalendarService {
        @GET("calendar/v3/users/me/calendarList")
        fun getCalendarList(
            @Header("Authorization") authHeader: String
        ): Call<CalendarListResponse>
    }

    data class CalendarListResponse(
        val items: List<CalendarListEntry>
    )

    data class CalendarListEntry(
        val id: String,
        val summary: String,
        val primary: Boolean
    )

    // Data model for the Google Token Response
    data class GoogleTokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("refresh_token") val refreshToken: String?,
        @SerializedName("scope") val scope: String?
    )

    data class EventDateTime(
        @SerializedName("dateTime") val dateTime: String,
        @SerializedName("timeZone") val timeZone: String
    )

    companion object {

        private const val RC_SIGN_IN = 113
        const val REQUEST_AUTHORIZATION = 126// Any integer constant
        //var mGoogleSignInClient: GoogleSignInClient? = null
        var accesstoken: String? = null
        var calendarID: String=""

    }
}


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM Token", "New token: $token")
        fcmtoken = token
        // Send the new token to your backend
        //sendTokenToServer(token)
    }

    fun sendTokenToServer(token: String, calendarId: String) {
        val url = URL("https://backend.stresswatch.net/save-token")  // Replace with your server URL

        // Create a new thread to send the token asynchronously
        Thread {
            try {
                // Open connection to the server
                val urlConnection = url.openConnection() as HttpURLConnection

                // Set up the request
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.doOutput = true

                // Create the JSON payload

                val jsonToken = "{\"calendarID\": \"${calendarID}\", \"fcm_token\": \"$token\"}"

                // Write the token to the output stream
                val outputStream = urlConnection.outputStream
                outputStream.write(jsonToken.toByteArray())
                outputStream.flush()
                outputStream.close()

                // Get the response from the server
                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("FCM Token", "Token sent successfully")
                } else {
                    Log.e("FCM Token", "Failed to send token: $responseCode")
                }

                urlConnection.disconnect()
            } catch (e: Exception) {
                Log.e("FCM Token", "Error sending token: ${e.message}")
            }
        }.start()
    }


    // Optional: Manually regenerate token if needed
    fun regenerateToken() {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { newTask ->
                    if (newTask.isSuccessful) {
                        val newToken = newTask.result
                        Log.d("FCM Token", "Regenerated token: $newToken")
                        //sendTokenToServer(newToken)
                    }
                }
            } else {
                Log.e("FCM Token", "Failed to delete old token", task.exception)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
            Log.d("Firebase","Got a notification")
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val notificationBuilder = NotificationCompat.Builder(this, "default_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {

        var fcmtoken: String = ""
    }
}