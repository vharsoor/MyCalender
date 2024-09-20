package dev.sudhanshu.calender.presentation.view

import android.app.Activity
import dev.sudhanshu.calender.R

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

class GoogleSignInHelper(private val context: Context) {

    private var mGoogleSignInClient: GoogleSignInClient? = null


    // Function to start Google Sign-In
    fun initiateGoogleSignIn(signInLauncher: ActivityResultLauncher<Intent>) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestScopes(Scope("https://www.googleapis.com/auth/admin.directory.resource.calendar"))
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
            //.requestScopes(Scope("https://www.googleapis.com/auth/calendar.read"))
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestServerAuthCode(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)

        val signInIntent = mGoogleSignInClient?.signInIntent
        if (signInIntent != null) {
            signInLauncher.launch(signInIntent)
        }
    }

    // Handle sign-in result
    fun handleSignInResult(task: Task<GoogleSignInAccount>, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        try {
            val account = task.getResult(ApiException::class.java)
            //authenticateWithCalendarAPI(account!!)
            val authCode = account?.serverAuthCode
            if (authCode != null) {
                Log.w("CalendarIntegration", "authCode: $authCode")
                getRefreshToken(authCode, onSuccess, onError)
            }
        } catch (e: ApiException) {
            onError(e.statusCode)
            Log.e("CalendarIntegration", "signInResult:failed code=${e.statusCode}")
        }
    }

    // Get the refresh token using the authCode
    private fun getRefreshToken(authCode: String, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OAuthService::class.java)
        val call = service.getToken(
            code = authCode,
            clientId = context.getString(R.string.web_client_id),
            clientSecret = context.getString(R.string.client_secret),
            redirectUri = "",
            grantType = "authorization_code"
        )

        call.enqueue(object : Callback<GoogleTokenResponse> {
            override fun onResponse(call: Call<GoogleTokenResponse>, response: Response<GoogleTokenResponse>) {
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    Log.d("CalendarIntegration", "Access Token: ${tokenResponse?.accessToken}")
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

    fun insertEventToGoogleCalendar(
        accessToken: String,
        summary: String,
        startDateTime: String,
        endDateTime: String,
        onSuccess: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        Log.d("CalendarIntegration", "Inside insertEventToGoogleCalendar")
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CalendarService::class.java)

        // Build the event details
        val event = GoogleCalendarEvent(
            summary = summary,
            start = EventDateTime(dateTime = startDateTime, timeZone = "UTC"),  // Adjust timezone as needed
            end = EventDateTime(dateTime = endDateTime, timeZone = "UTC")
        )

        // Make the API request
        val call = service.insertEvent(
            authorization = "Bearer $accessToken",
            event = event
        )

        call.enqueue(object : Callback<GoogleCalendarEventResponse> {
            override fun onResponse(
                call: Call<GoogleCalendarEventResponse>,
                response: Response<GoogleCalendarEventResponse>
            ) {
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    Log.d("CalendarIntegration", "Event ID: ${eventResponse?.id}")
                    onSuccess(eventResponse?.id ?: "")
                } else {
                    Log.e("CalendarIntegration", "Error inserting event")
                    onError(response.code())
                }
            }

            override fun onFailure(call: Call<GoogleCalendarEventResponse>, t: Throwable) {
                Log.e("CalendarIntegration", "Error: ${t.message}")
                onError(-1)
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
    }

    interface CalendarService {
        @POST("/calendar/v3/calendars/primary/events")
        fun insertEvent(
            @Header("Authorization") authorization: String,
            @Body event: GoogleCalendarEvent
        ): Call<GoogleCalendarEventResponse>
    }


    // Data model for the Google Token Response
    data class GoogleTokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("refresh_token") val refreshToken: String?,
        @SerializedName("scope") val scope: String?
    )

    data class GoogleCalendarEvent(
        @SerializedName("summary") val summary: String,
        @SerializedName("start") val start: EventDateTime,
        @SerializedName("end") val end: EventDateTime
    )

    data class EventDateTime(
        @SerializedName("dateTime") val dateTime: String,
        @SerializedName("timeZone") val timeZone: String
    )

    data class GoogleCalendarEventResponse(
        @SerializedName("id") val id: String,
        @SerializedName("status") val status: String
    )


    companion object {
        fun insertEventToGoogleCalendar(accessToken: String, summary: String, startDateTime: String, endDateTime: String, onSuccess: Any, onError: Any) {

        }

        private const val RC_SIGN_IN = 113
        const val REQUEST_AUTHORIZATION = 126// Any integer constant
        private lateinit var _googleCalendarClient: Calendar
        //var mGoogleSignInClient: GoogleSignInClient? = null
        var accesstoken: String? = null

        val googleCalendarClient: Calendar
            get() = _googleCalendarClient
    }
}
