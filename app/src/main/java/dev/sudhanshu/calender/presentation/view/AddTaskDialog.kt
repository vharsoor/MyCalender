package dev.sudhanshu.calender.presentation.view


import android.app.TimePickerDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel

import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel

import android.util.Log
import android.widget.TimePicker

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.sql.Time

import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Date

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
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(("")) }
    var endTime by remember { mutableStateOf(("")) }

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
                Text(
                    text="Start Time: $startTime"
                )
                Text(
                    text="End Time: $endTime"
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
                Button(onClick={
                    val calendar = Calendar.getInstance()
                    val timePickerDialog = TimePickerDialog(
                        context,
                        {
                            _:TimePicker, hourOfDay: Int, minute: Int->
                            startTime = String.format("%02d:%2d", hourOfDay, minute)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    timePickerDialog.show()

                }){
                    Text(text="Pick start Time")
                }

                Button(onClick={
                    val calendar = Calendar.getInstance()
                    val timePickerDialog = TimePickerDialog(
                        context,
                        {
                                _: TimePicker, hourOfDay: Int, minute: Int->
                            endTime = String.format("%02d:%2d", hourOfDay, minute)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    timePickerDialog.show()

                }){
                    Text(text="Pick end Time")
                }
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
                            Log.d("add task", "date: $date")
                            Log.d("add task", "startTime: $startTime")
                            val startDateTime = convertToSimpleDate(date, startTime)
                            val endDateTime = convertToSimpleDate(date,endTime)

                            Log.d("add task", "date: $startDateTime")
                            Log.d("add task", "startTime: $endDateTime")

                            if (compareTimes(date, startTime, date, endTime)) {
                                Log.d("add task", "endTime is greater than startTime")
                            } else {
                                Log.d("add task", "endTime is not greater than startTime")
                                onDismiss()
                            }
                            val insertTask = InsertTask(context)
                            insertTask.insertEventToGoogleCalendar(
                                accessToken = GoogleSignInHelper.accesstoken ?: "",
                                summary = title,
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                createMeetLink = true,
                                onSuccess = { eventId ->
                                    Log.d("CalendarIntegration", "Event inserted with ID: $eventId")
                                    onSaveTask() // Update the UI or dismiss the dialog
                                },
                                onError = { errorCode ->
                                    Log.e("CalendarIntegration", "Error inserting event: $errorCode")
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

fun convertToSimpleDate(dateInput: String, timeInput: String): String {
    val inputDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
    val outputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    outputDateFormat.timeZone = TimeZone.getTimeZone("MST")
    val date = inputDateFormat.parse("$dateInput $timeInput")
    return outputDateFormat.format(date)
}

fun compareTimes(dateStart: String, startTime: String, dateEnd: String, endTime: String):Boolean{
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
    val startDateTime = dateFormat.parse("$dateStart $startTime")
    val endDateTime = dateFormat.parse("$dateEnd $endTime")
    return endDateTime.after(startDateTime)
}