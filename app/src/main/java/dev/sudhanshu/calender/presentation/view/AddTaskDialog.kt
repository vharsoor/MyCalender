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
                        }
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}




