package dev.sudhanshu.calender.presentation.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.presentation.ui.theme.Typography
import okhttp3.internal.wait
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalenderTaskScreen(userId: Int, date: String? = null,  viewModel: TaskViewModel = hiltViewModel()) {
    val tasks by viewModel.taskList.collectAsState()

    LaunchedEffect(userId, date) {
        viewModel.getTaskListByDate(userId, date, onSuccess = {

        }, onError = {})
    }

    Column{
        Text(
            text = "Tasks on selected date ( ${tasks?.size} )",
            style = Typography.h2,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
        )
        when {
            tasks == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            tasks!!.isEmpty() -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.no_task),
                            contentDescription = "No Tasks",
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tasks available for selected date",
                            style = Typography.h3,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks!!) { task ->
                        TaskCard(task = task)
                    }
                }
            }
        }
    }


}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskCard(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { /* Handle click if needed */ } ,
        backgroundColor = Color(0xFF4DF6E9),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.task_detail.title ?: "",
                    style = Typography.h3,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDateTime(task.task_detail.date, task.task_detail.time) ?: "",
                    modifier = Modifier
                        .background(Color.White, shape = RoundedCornerShape(6.dp)).padding(4.dp),
                    color = Color.Black,
                    fontSize = 12.sp,
                    style = Typography.h3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.task_detail.description ?: "",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                fontSize = 12.sp
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
fun formatDateTime(date: String?, time: String?): String {
    return try {
        val inputDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
        val outputDateFormatter = DateTimeFormatter.ofPattern("dd MMMM", Locale.ENGLISH)
        val localDate = LocalDate.parse(date, inputDateFormatter)
        val formattedDate = localDate.format(outputDateFormatter)

        val formattedTime = if (time != null) {
            val inputTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            val localTime = LocalTime.parse(time, inputTimeFormatter)
            localTime.format(inputTimeFormatter)
        } else {
            ""
        }

        if (formattedTime.isNotEmpty()) {
            "$formattedDate, $formattedTime"
        } else {
            formattedDate
        }
    } catch (e: Exception) {
        date ?: ""
    }
}