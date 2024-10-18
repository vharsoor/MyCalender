package dev.sudhanshu.calender.presentation.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
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
import dagger.hilt.android.AndroidEntryPoint
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.presentation.ui.theme.CalenderTheme
import dev.sudhanshu.calender.presentation.ui.theme.Typography
import dev.sudhanshu.calender.util.SettingsPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.time.LocalDateTime
import java.time.ZonedDateTime



@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    private lateinit var settingsPreferences: SettingsPreferences
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var eventServer: EventServer? = null

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
//

        }

//        startScreenPinning()


        coroutineScope.launch(Dispatchers.Main) {

            // Request Google Calendar permission after notification permission is granted
            if (!hasCalendarPermission()) {
                // If not, request the user to grant calendar permission
                Log.d("CalendarIntegration", "No calendar permission, will request one")
                requestCalendarPermission()

                // Wait for the calendar permission result
                while (!hasCalendarPermission()) {
                    delay(1000) // Delay for 1 second before checking again
                }
            }
        }


        googleSignInHelper = GoogleSignInHelper(this)

        // Initialize the ActivityResultLauncher
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                googleSignInHelper.handleSignInResult(task, ::onSignInSuccess, ::onSignInError)
            } else {
                Log.e("CalendarIntegration", "Sign-in failed with resultCode: ${result.resultCode}")
            }
        }

//         Start Google Sign-In
        googleSignInHelper.initiateGoogleSignIn(signInLauncher)
        Log.d("Reminder", "Call getRetrofitInstance")

        val currentTime = LocalDateTime.now()
        val scheduleTime = EventDateTime(dateTime = currentTime.plusMinutes(3).toString(), timeZone = ZonedDateTime.now().toString())
        val scheduleEndTime = EventDateTime(dateTime = currentTime.plusMinutes(10).toString(), timeZone = ZonedDateTime.now().toString())
        Log.d("Notification", "scheduleTime $scheduleTime")
        val calendarEvent = GoogleCalendarEvent(
            id = "54321",
            start = scheduleTime,
            summary = "test event",
            end = scheduleEndTime
        )


        val notificationHelper = NotificationHelper(this)
        notificationHelper.scheduleNotification(calendarEvent)

        eventServer = EventServer(8443)
        try{
            eventServer?.start()
            Log.d("Notification", "Event server started on port 8443")
        }
        catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventServer?.stop()
    }
    // sdk > 12 requires requesting alarm permission at runtime
    fun requestAlarmPermission(){
        AlertDialog.Builder(this)
            .setTitle("Request Exact Alarm Permission")
            .setMessage("This app needs permission to schedule exact alarms. Please grant permission")
            .setPositiveButton("Open Settings"){
                    _,_ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun onSignInSuccess(accessToken: String) {
        Log.d("CalendarIntegration", "Sign-in success with access token: $accessToken")
        // Proceed with your app logic
        val reminderScheduler = ReminderScheduler(this)
        reminderScheduler.startTracking()
        CoroutineScope(Dispatchers.Main).launch{
            delay(2*60*1000)
            reminderScheduler.stopTracking()
            Log.d("Reminder EventScheduler", "Fetching events stopped after 2 minutes")
        }
    }

    // Callback when sign-in fails
    private fun onSignInError(errorCode: Int) {
        Log.e("CalendarIntegration", "Sign-in failed with error code: $errorCode")
        // Handle error cases accordingly
    }

    private fun startScreenPinning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            //if (devicePolicyManager.isLockTaskPermitted(packageName)) {
            startLockTask()
            //else {
              //  Log.d("Vishrut","No startScreenPinning")


            //}
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

    private var backPressedTime: Long = 0

    override fun onBackPressed() {
        if (shouldTriggerPinVerification()) {
            if (System.currentTimeMillis() - backPressedTime < 2000) {
                Log.d("Vishrut","Enterted Back triggered")
                stopScreenPinning()
            } else {
                Toast.makeText(this, "Press back again to unpin", Toast.LENGTH_SHORT).show()
                backPressedTime = System.currentTimeMillis()
            }
        } else {
            // Perform default back action
            super.onBackPressed()
        }
    }

    private fun shouldTriggerPinVerification(): Boolean {
        // Define your condition for when to trigger PIN verification
        return true // Replace with actual condition
    }


    private fun stopScreenPinning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Launch the custom PIN verification activity
            Log.d("Vishrut","Enterted stopScreenPinning")
            val intent = Intent(this, PinVerificationActivity::class.java)
            startActivity(intent)
        }
    }


    private fun hasCalendarPermission(): Boolean {
        // Check if the app has both read and write permissions for Google Calendar
        val readPermission = checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val writePermission = checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        Log.d("CalendarIntegration", "readPermission=$readPermission and writePermission=$writePermission")
        return readPermission && writePermission
    }

    private fun requestCalendarPermission() {
        // Request both read and write permissions for Google Calendar
        Log.d("CalendarIntegration", "requestCalendarPermission")
        requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR), 2)
        //requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR),REQUEST_CALENDAR_ACCESS)
    }


}






@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarApp(userId : Int) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var isDialogOpen by remember { mutableStateOf(false) }
    var lastSwipeTime by remember { mutableStateOf(0L) }
    val debouncePeriod = 300L // milliseconds

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
                //currentMonth = currentMonth.minusMonths(1)
                //selectedDate = if (currentMonth == YearMonth.now()) LocalDate.now() else currentMonth.atDay(1)

            }, onSwipeLeft = {
                handleSwipe(false)
                //currentMonth = currentMonth.plusMonths(1)
                //selectedDate = if (currentMonth == YearMonth.now()) LocalDate.now() else currentMonth.atDay(1)

            })
            CalenderTaskScreen(userId = userId, selectedDate.format(dateFormatter))

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


class PinVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Vishrut","Enterted PinVerification")
        setContentView(R.layout.activity_pin_verification)
        Log.d("Vishrut","After PinVerification")

        val pinEditText: EditText = findViewById(R.id.pinEditText)
        val verifyButton: Button = findViewById(R.id.verifyButton)

        verifyButton.setOnClickListener {
            val enteredPin = pinEditText.text.toString()
            if (isPinCorrect(enteredPin)) {
                // Exit screen pinning
                stopLockTask()
                finish()
            } else {
                // Show error message
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPinCorrect(enteredPin: String): Boolean {
        // Replace with your own PIN verification logic
        val storedPin = "1234" // This should be securely stored and retrieved
        return enteredPin == storedPin
    }
}