package dev.sudhanshu.calender.presentation.view


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.lang.Exception
import android.util.Log
import dev.sudhanshu.calender.presentation.view.GoogleSignInHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

//val signInClient = GoogleSignInHelper.googleCalendarClient

private val job = Job()
private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

@Composable
fun AddTaskDialog(
    userId : Int,
    date : String,
    time : String,
    viewModel: TaskViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSaveTask: () -> Unit
) {

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(20.dp).background(color = MaterialTheme.colors.background),
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Your Task",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colors.onBackground
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Title") },
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Description") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.storeTask(TaskRequest(userId, TaskModel(title, description, date, time)), onSuccess = {
                                viewModel.getTaskListByDate(userId, date = date, onSuccess = {
                                    onSaveTask()
                                }, onError = {
                                    onDismiss()
                                })
                            }, onError = {
                                onDismiss()
                            })
                            //addEvent(title, date)
                            val startDateTime = "2023-09-10T10:00:00Z" // Set the event start time
                            val endDateTime = "2023-09-10T11:00:00Z"   // Set the event end time
                            GoogleSignInHelper.insertEventToGoogleCalendar(
                                accessToken = GoogleSignInHelper.accesstoken ?: "",
                                summary = title,
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                onSuccess = { title: String ->
                                    Log.d("CalendarIntegration", "Event inserted with Title: $title")
                                    onSaveTask() // Update the UI or dismiss the dialog
                                },
                                onError = { title: String ->
                                    Log.e("CalendarIntegration", "Error inserting event: $title")
                                    onDismiss()
                                }
                            )
                            Log.d("CalendarIntegration","Time : $date : $time")

                        }
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}



private fun addEventToCalendar(
    googleCalendarClient: Calendar,
    summary: String,
    startDate: String
): Boolean {
    return try {
        val event = Event().apply {
            this.summary = summary
            start = EventDateTime().setDateTime(DateTime(startDate))
            end = EventDateTime().setDateTime(DateTime(startDate))
        }

        googleCalendarClient.events().insert("primary", event).execute()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


private fun addEvent(summary: String, startDate: String) {
    // Call the function to add the event to the calendar
    Log.d("CalendarIntegration", "Inside AddEvents, lets start coroutine")
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val result = addEventToCalendar(GoogleSignInHelper.googleCalendarClient, summary, startDate)
            Log.d("CalendarIntegration", "Event added to calendar: $result")

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}