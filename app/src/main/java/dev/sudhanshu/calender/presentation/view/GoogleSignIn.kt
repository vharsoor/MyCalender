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
                    saveRefreshToken(tokenResponse?.refreshToken ?: "")
                    Log.d("CalendarIntegration", "Refresh Token: ${tokenResponse?.refreshToken}")
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
        private lateinit var _googleCalendarClient: Calendar
        //var mGoogleSignInClient: GoogleSignInClient? = null
        var accesstoken: String? = null

        val googleCalendarClient: Calendar
            get() = _googleCalendarClient
    }
}
