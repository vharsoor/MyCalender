package dev.sudhanshu.calender.presentation.view

import android.content.Intent
import android.os.Build
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import java.util.*
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.presentation.ui.theme.CalenderTheme
import dev.sudhanshu.calender.presentation.ui.theme.Typography
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel
import dev.sudhanshu.calender.util.SettingsPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle


@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    private lateinit var settingsPreferences: SettingsPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsPreferences = SettingsPreferences.getInstance(this)


        setContent {
            CalenderTheme {
                SetStatusBarColor()
                Scaffold (Modifier.background(MaterialTheme.colors.background)){ padding ->
                    padding.calculateTopPadding()
                    CalendarApp(settingsPreferences.getUserId())
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview
    @Composable
    fun CalenderPrev(){
        CalendarApp(settingsPreferences.getUserId())
    }


    @Composable
    private fun SetStatusBarColor(color: Color = MaterialTheme.colors.background) {
        window.statusBarColor = color.toArgb()
        val decorView = window.decorView
        if (color == Color.White || color == Color(0xFFE5FF7F)) {
            decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarApp(userId : Int) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var isDialogOpen by remember { mutableStateOf(false) }
    var lastSwipeTime by remember { mutableStateOf(0L) }
    val debouncePeriod = 300L // milliseconds

    var showShoppingDialog by remember { mutableStateOf(false) }
    var navigateToShoppingCart by remember { mutableStateOf(false) }
    val shoppingList = remember { mutableStateListOf<String>() }

    val handleSwipe: (Boolean) -> Unit = { toRight ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwipeTime > debouncePeriod) {
            lastSwipeTime = currentTime
            if (toRight) {
                currentMonth = currentMonth.minusMonths(1)
            } else {
                currentMonth = currentMonth.plusMonths(1)
            }
            selectedDate = if (currentMonth == YearMonth.now()) LocalDate.now() else currentMonth.atDay(1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)) {

            TopBar()
            Spacer(modifier = Modifier.height(16.dp))
            MonthNavigation(currentMonth, onPrevMonth = {
                currentMonth = currentMonth.minusMonths(1)
                selectedDate = if (currentMonth == YearMonth.now()) LocalDate.now() else currentMonth.atDay(1)
            }, onNextMonth = {
                currentMonth = currentMonth.plusMonths(1)
                selectedDate = if (currentMonth == YearMonth.now()) LocalDate.now() else currentMonth.atDay(1)
            })
            Spacer(modifier = Modifier.height(16.dp))
            CalendarView(currentMonth, selectedDate, onDateSelected = { date ->
                selectedDate = date
            }, onSwipeRight = {
                handleSwipe(true)

            }, onSwipeLeft = {
                handleSwipe(false)

            })
            CalenderTaskScreen(userId = userId, selectedDate.format(dateFormatter))

        }

        if(navigateToShoppingCart){
            val intent = Intent(context, ShoppingListActivity::class.java)
            context.startActivity(intent)
            navigateToShoppingCart = false
        }
        if(showShoppingDialog){
            AddItemDialog(
                onAddItem = {
                    itemName ->
                    shoppingList.add("$itemName")
                    showShoppingDialog = false
                },
                onDismiss = {showShoppingDialog = false}
            )
        }


        // Showing dialog when adding a task
        if (isDialogOpen) {
            AddTaskDialog(
                userId,
                date = selectedDate.format(dateFormatter),
                time = getCurrentTime(),
                onDismiss = { isDialogOpen = false },
                onSaveTask = {
                    isDialogOpen = false
                }
            )
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            onClick = { navigateToShoppingCart = true },
            containerColor = Color(0xFFC96868)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Shopping List", tint = Color.Black)
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            onClick = { isDialogOpen = true },
            containerColor = Color(0xFFE5FF7F)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.Black)
        }

    }


}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthNavigation(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(id = R.drawable.previous),
            contentDescription = "PrevMonth",
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .padding(4.dp)
                .clickable {
                    onPrevMonth()
                }
        )
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.year,
            style = Typography.h3,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
        Icon(
            painter = painterResource(id = R.drawable.next),
            contentDescription = "NextMonth",
            tint =   MaterialTheme.colors.onBackground,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .padding(4.dp)
                .clickable {
                    onNextMonth()
                }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)) {
        WeekDaysHeader()
        Spacer(modifier = Modifier.height(4.dp))
        DaysGrid(currentMonth, selectedDate, onDateSelected, onSwipeRight = {
            onSwipeRight()
        }, onSwipeLeft = {
            onSwipeLeft()
        })
    }
}

@Composable
fun WeekDaysHeader() {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                style = Typography.h2,
                color = MaterialTheme.colors.onBackground
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DaysGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val totalDays = lastDayOfMonth.dayOfMonth

    Column(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
            when {
                dragAmount > 0 -> onSwipeRight()
                dragAmount < 0 -> onSwipeLeft()
            }
        }
    }) {
        var day = 1
        for (week in 0..5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < firstDayOfWeek || day > totalDays) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f))
                    } else {
                        val date = currentMonth.atDay(day)
                        val isSelected = date == selectedDate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .background(
                                    color = if (isSelected) Color(0xFFE5FF7F) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onDateSelected(date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = Typography.h3,
                                color =  MaterialTheme.colors.onBackground
                            )
                        }
                        day++
                    }
                }
            }
        }
    }
}


@Composable
fun TopBar(){
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp)
            .background(androidx.compose.material.MaterialTheme.colors.background)
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){

            Text(
                text = "My Calender",
                style = Typography.h1,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onBackground
            )
            Icon(
                painter = painterResource(id = R.drawable.list),
                contentDescription = "List Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .padding(4.dp)
                    .clickable {
                        val intent = Intent(context, CalenderTasks::class.java)
                        context.startActivity(intent)
                    }
            )
        }

    }

}

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentTime(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    return currentTime.format(formatter)
}