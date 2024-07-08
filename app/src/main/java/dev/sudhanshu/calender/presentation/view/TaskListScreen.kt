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
fun TaskListScreen(userId: Int,  viewModel: TaskViewModel = hiltViewModel()) {
    val tasks by viewModel.taskList.collectAsState()

    LaunchedEffect(userId) {
        viewModel.getTaskList(userId,  onSuccess = {

        }, onError = {})
    }

    Column(modifier = Modifier.background(color = MaterialTheme.colors.background)){
        Text(
            text = "Showing your added task ( ${tasks?.size} )",
            style = Typography.h2,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp)
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
                    modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colors.background),
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
                            text = "You haven't added a task yet",
                            style = Typography.h3,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(10.dp).background(color = MaterialTheme.colors.background)
                ) {
                    items(tasks!!) { task ->
                        TaskCard(task = task)
                    }
                }
            }
        }
    }


}




