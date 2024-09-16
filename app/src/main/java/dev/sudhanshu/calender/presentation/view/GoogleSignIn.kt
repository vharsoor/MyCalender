package dev.sudhanshu.calender.presentation.view

import dev.sudhanshu.calender.R

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class GoogleSignInHelper(private val context: Context) {

    private var mGoogleSignInClient: GoogleSignInClient? = null

    // Function to start Google Sign-In
    fun initiateGoogleSignIn(signInLauncher: ActivityResultLauncher<Intent>) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestScopes(Scope("https://www.googleapis.com/auth/admin.directory.resource.calendar"))
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
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
            val authCode = account?.serverAuthCode
            if (authCode != null) {
                Log.w("GoogleSignInHelper", "authCode: $authCode")
                getRefreshToken(authCode, onSuccess, onError)
            }
        } catch (e: ApiException) {
            onError(e.statusCode)
            Log.e("GoogleSignInHelper", "signInResult:failed code=${e.statusCode}")
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
                    Log.d("GoogleSignInHelper", "Access Token: ${tokenResponse?.accessToken}")
                    onSuccess(tokenResponse?.accessToken ?: "")
                } else {
                    Log.e("GoogleSignInHelper", "Error in token response")
                    onError(response.code())
                }
            }

            override fun onFailure(call: Call<GoogleTokenResponse>, t: Throwable) {
                Log.e("GoogleSignInHelper", "Error fetching token: ${t.message}")
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

    // Data model for the Google Token Response
    data class GoogleTokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("refresh_token") val refreshToken: String?,
        @SerializedName("scope") val scope: String?
    )
}
