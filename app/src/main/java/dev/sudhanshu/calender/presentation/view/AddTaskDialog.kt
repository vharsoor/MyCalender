package dev.sudhanshu.calender.presentation.view


import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import dev.sudhanshu.calender.presentation.view.MainActivity.Companion.REQUEST_AUTHORIZATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

private val job = Job()
private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

@Composable
fun AddTaskDialog(
    userId : Int,
    date : String,
    time : String,
    initialTitle: String = "",
    viewModel: TaskViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSaveTask: () -> Unit
) {
    //var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(mutableListOf<String>()) }
    var newItem by remember { mutableStateOf("") }
    var checkedStates by remember { mutableStateOf(MutableList(items.size) { false }) }
    var editedIndex by remember { mutableStateOf<Int?>(null) }

    var taskType = initialTitle
    //When taskType is changed, update title accordingly
    title = when (taskType) {
        "Shopping/Grocery List" -> "Grocery Shopping"
        "Schedule a task/reminder" -> "Reminder"
        else -> ""
    }

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

                if (taskType == "Shopping/Grocery List") {
                    Column {
                        if (checkedStates.size < items.size) {
                            checkedStates = MutableList(items.size) { false }
                        }

                        // Displaying and editing existing items in-place
                        items.mapIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = checkedStates.getOrElse(index) { false },
                                    onCheckedChange = { isChecked ->
                                        if (index in checkedStates.indices) {
                                            checkedStates[index] = isChecked
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                // Toggle between Text and OutlinedTextField for editing
                                if (editedIndex == index) {
                                    OutlinedTextField(
                                        value = item,
                                        onValueChange = { updatedItem: String ->
                                            items = items.toMutableList().apply { set(index, updatedItem) }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        label = { Text("Edit Item") },
                                        singleLine = true
                                    )
                                } else {
                                    Text(
                                        text = item,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { editedIndex = index } // Click to edit the item
                                            .padding(vertical = 8.dp)
                                    )
                                }

                                // Cross mark for removing the item
                                IconButton(onClick = {
                                    items = items.toMutableList().apply { removeAt(index) }
                                    checkedStates = checkedStates.toMutableList().apply { removeAt(index) }
                                    if (editedIndex == index) editedIndex = null
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Item")
                                }
                            }
                        }

                        // Input box for adding new items
                        OutlinedTextField(
                            value = newItem,
                            onValueChange = { updatedItem: String ->
                                newItem = updatedItem
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            label = { Text("Add Item") },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to add the new item to the list
                        Button(onClick = {
                            if (newItem.isNotBlank()) {
                                items = (items + newItem).toMutableList()
                                checkedStates = (checkedStates + false).toMutableList() // Add new checkbox state
                                newItem = ""
                            }
                        }) {
                            Text(text = "Add to List")
                        }
                    }
                } else { // For other task types
                    OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Description") }
                ) }

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
                                    Log.d("Vishrut", "before coroutineScope")
                                    coroutineScope.launch(Dispatchers.IO) {
                                        onSaveTask()
                                        Log.d("Vishrut", "inside coroutineScope")
                                        addEventToGoogleCalendar(MainActivity.googleCalendarClient, title)
                                }}, onError = {
                                    onDismiss()
                                })
                            }, onError = {
                                onDismiss()
                            })
                        }
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}


@Composable
fun TaskTypeDialog(
    onDismiss: () -> Unit,
    onTaskTypeSelected: (String) -> Unit
) {
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
                    text = "Select Task Type",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colors.onBackground
                )

                val taskTypes = listOf("Schedule a task/reminder", "Shopping/Grocery List", "Meal plans")

                taskTypes.forEach { taskType ->
                    Button(
                        onClick = {
                            onTaskTypeSelected(taskType)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = taskType)
                    }
                }

                TextButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}

private suspend fun addEventToGoogleCalendar(
    googleCalendarClient: Calendar,
    summary: String,
    startDate: String = "2024-09-06"
): Boolean {
    Log.d("Vishrut", "just started google calendar in addEventToGoogleCalendar")
    return try {
        Log.d("Vishrut", "in-between google calendar in addEventToGoogleCalendar")
        val event = Event().apply {
            this.summary = summary
            start = EventDateTime().setDateTime(DateTime(startDate))
            end = EventDateTime().setDateTime(DateTime(startDate))
        }

        Log.d("Vishrut", "google calendar in addEventToGoogleCalendar")
        googleCalendarClient.events().insert("primary", event).execute()
        true
    }catch (e: Exception) {
        Log.d("Vishrut", "something error google calendar in addEventToGoogleCalendar",e)
        e.printStackTrace()
        false
    }
}
