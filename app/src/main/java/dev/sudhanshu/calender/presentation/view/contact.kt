package dev.sudhanshu.calender.presentation.view

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


@Composable
fun AddContactScreen() {
    var isContactDialogOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Button(
            onClick = { isContactDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(text = "Add Contact")
        }


        if (isContactDialogOpen) {
            AddContactDialog(onDismiss = { isContactDialogOpen = false })
        }
    }
}
@Composable
fun AddContactDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(20.dp)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add New Contact",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Name") }
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Email") }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Phone") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(text = "Cancel")
                    }
                    Button(onClick = {
                        val accessToken = GoogleSignInHelper.accesstoken
                        val formattedPhone = if (!phone.startsWith("+")) {
                            "+1$phone"
                        } else {
                            phone
                        }

                        Log.d("UserInput", "Name: $name, Email: $email, Phone: $phone")
                        Log.d("AccessToken", "Access Token: $accessToken")
                        if (accessToken != null) {
                            createContact(context, name, email, phone, accessToken)
                        }
                        onDismiss()
                    }) {
                        Text(text = "Submit")
                    }
                }
            }
        }
    }
}
interface PeopleService {
    @POST("/v1/people:createContact")
    fun createContact(
        @Body contact: GoogleContact,
        @Header("Authorization") accessToken: String
    ): Call<GoogleContactResponse>
}
data class GoogleContact(
    val names: List<Name>,
    val emailAddresses: List<Email>,
    val phoneNumbers: List<Phone>
)

data class Name(
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("givenName") val givenName: String?,
    @SerializedName("familyName") val familyName: String?
)

data class Email(val value: String)
data class Phone(val value: String)
data class GoogleContactResponse(
    @SerializedName("resourceName") val resourceName: String,
    @SerializedName("etag") val etag: String
)

fun createContact(context: Context, name: String, email: String, phone: String, accessToken: String) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://people.googleapis.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(PeopleService::class.java)

    val contact = GoogleContact(
        names = listOf(Name(givenName = name, familyName = "", displayName = name)),
        emailAddresses = listOf(Email(value = email)),
        phoneNumbers = listOf(Phone(value = phone))
    )


    service.createContact(contact, "Bearer $accessToken").enqueue(object :
        Callback<GoogleContactResponse> {
        override fun onResponse(call: Call<GoogleContactResponse>, response: Response<GoogleContactResponse>) {
            if (response.isSuccessful) {
                Log.d("GoogleContacts", "Contact created: ${response.body()?.resourceName}")
                Log.d("GoogleContacts", "API Response: ${response.body()?.resourceName}")

            } else {
                Log.e("GoogleContacts", "Failed to create contact: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<GoogleContactResponse>, t: Throwable) {
            Log.e("GoogleContacts", "Error creating contact: ${t.message}")
        }
    })
}