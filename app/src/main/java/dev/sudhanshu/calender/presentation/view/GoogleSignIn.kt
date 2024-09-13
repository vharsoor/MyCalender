package dev.sudhanshu.calender.presentation.view

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getString
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.sudhanshu.calender.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL


@Composable
fun GoogleSignIn() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.web_client_id)

    // Automatically trigger Google Sign-In when the composable is loaded
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            signInWithGoogle(context, credentialManager, webClientId)
        }
    }
}

/*
private fun authenticateWithGoogleSignIn() {
    val account = GoogleSignIn.getLastSignedInAccount(this)
    if (account == null) {
        signIn()
    } else {
        authenticateWithCalendarAPI(account)
    }
}

private fun signIn() {
    val webClientId = getString(R.string.web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    Log.d("CalendarIntegration", "Trying to Google SignIn")
    try {
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d("CalendarIntegration", "Trying to Google SignIn2")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    } catch (e: ApiException) {
        Log.e("CalendarIntegration", "Error with Google Play services API: ${e.statusCode}")
        // Handle the error based on the status code
    }
    //fetchAndDisplayCalendarEvents()
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == RC_SIGN_IN) {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        handleSignInResult(task)
    }
}

private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
    try {
        val account = completedTask.getResult(ApiException::class.java)
        authenticateWithCalendarAPI(account!!)

        val email = account.email
        val displayName = account.displayName
        val uid = account.id
        val photoUrl = account.photoUrl

        // Now you can use email, displayName, uid, and photoUrl as needed

        // Now you can use email, displayName, uid, and photoUrl as needed
        Log.d("User info", "Email: $email")
        Log.d("User info", "Display Name: $displayName")
        Log.d("User info", "UID: $uid")

        if (photoUrl != null) {
            Log.d("User info", "Photo URL: $photoUrl")
        }
    } catch (e: ApiException) {
        Log.e("CalendarIntegration", "Google Sign-In failed: $e")
        // Handle sign-in failure
    }
}

private fun authenticateWithCalendarAPI(account: GoogleSignInAccount) {
    val credential = GoogleAccountCredential.usingOAuth2(
        applicationContext, setOf(CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR)
        //applicationContext, setOf( CalendarScopes.CALENDAR)
    )

    credential.selectedAccount = account.account

    Log.d("CalendarIntegration", "google calender in authenticateWithCalendarAPI")
    _googleCalendarClient = Calendar.Builder(
        NetHttpTransport(),
        JacksonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("com.example.proj5")
        .build()

}
*/

suspend fun signInWithGoogle(context: Context, credentialManager: CredentialManager, webClientId: String) {
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(
            request = request,
            context = context,
        )
        handleSignIn(result)
    } catch (e: GetCredentialException) {
        Log.d("Vishrut", "Get credential failed: ${e.message}")
    }
}

fun handleSignIn(result: GetCredentialResponse) {
    val credential = result.credential

    when (credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    // Use the Google ID token for Google Calendar API requests
                    val idToken = googleIdTokenCredential.idToken
                    Log.d("Vishrut", "Google ID Token: $idToken")
                    Log.d("Vishrut","No More Toekns after this")
                    addEventToGoogleCalendar(idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("Vishrut", "Received an invalid Google ID token", e)
                }
            } else {
                Log.e("Vishrut", "Unexpected credential type")
            }
        }
        else -> {
            Log.e("Vishrut", "Unexpected credential type")
        }
    }
}

fun addEventToGoogleCalendar(idToken: String) {
    val calendarEventUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
    val event = """
        {
            "summary": "New Event",
            "location": "Some Location",
            "description": "Description of the event",
            "start": {
                "dateTime": "2024-09-12T09:00:00-07:00",
                "timeZone": "America/Los_Angeles"
            },
            "end": {
                "dateTime": "2024-09-12T17:00:00-07:00",
                "timeZone": "America/Los_Angeles"
            }
        }
    """.trimIndent()

    val url = URL(calendarEventUrl)
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer $idToken")
        doOutput = true
        outputStream.write(event.toByteArray())
        outputStream.flush()

        val responseCode = responseCode
        Log.d("Vishrut", "Response Code : $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = inputStream.bufferedReader().use { it.readText() }
            Log.d("Vishrut", "Event created: $response")
        } else {
            Log.e("Vishrut", "Failed to create event")
        }
    }
}

/*
@Composable
fun GoogleSignIn1() {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.web_client_id)

    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true) // we will talk about this later
        .setServerClientId(webClientId) // Check point no. 9 & 10
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    coroutineScope.launch {
        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            handleSignIn(result)
            onGetCredentialResponse(result.credential)
        } catch (e: GetCredentialException) {
            Log.d("Vishrut", "Get credential failed")
        }
    }
}

fun handleSignIn1(result: GetCredentialResponse) {
    // Handle the successfully returned credential.
    val credential = result.credential

    when (credential) {

        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)
                    // Send googleIdTokenCredential to your server for validation and authentication
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("Vishrut", "Received an invalid google id token response", e)
                }
            } else {
                // Catch any unrecognized custom credential type here.
                Log.e("Vishrut", "Unexpected type of credential")
            }
        }
        else -> {
            // Catch any unrecognized credential type here.
            Log.e("Vishrut", "Unexpected type of credential")
        }
    }
}


fun onGetCredentialResponse(credential: Credential) {
    launchCatching {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            accountService.signInWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.e("Vishrut", UNEXPECTED_CREDENTIAL)
        }
    }
}

fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
    viewModelScope.launch(
        CoroutineExceptionHandler { _, throwable ->
            Log.d("Vishrut", throwable.message.orEmpty())
        },
        block = block
    )


override suspend fun signInWithGoogle(idToken: String) {
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
    Firebase.auth.signInWithCredential(firebaseCredential).await()
} */