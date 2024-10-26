package dev.sudhanshu.calender.presentation.view

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

import java.text.SimpleDateFormat

import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    private lateinit var settingsPreferences: SettingsPreferences
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var eventServer: EventServer? = null
    var myAccessToken: String? = null
    private lateinit var lockScreenReceiver: LockScreenReceiver

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

        startScreenPinning()

        //val messagingService = MyFirebaseMessagingService()
        //messagingService.regenerateToken()

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

        // Authenticate with Google Sign-In
        //authenticateWithGoogleSignIn()
        googleSignInHelper = GoogleSignInHelper(this)

        fun googleSignInTime() {

            // Initialize the ActivityResultLauncher
            signInLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val data = result.data
                    Log.d("CalendarIntegration", "Sign-in result code: ${result.resultCode}")
                    if (result.resultCode == RESULT_OK && data != null) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        Log.d("CalendarIntegration", "Result ok and Sign-in task: $task")
                        googleSignInHelper.handleSignInResult(
                            task,
                            ::onSignInSuccess,
                            ::onSignInError
                        )
                    } else {
                        Log.e(
                            "CalendarIntegration",
                            "Sign-in failed with resultCode: ${result.resultCode}"
                        )
                        googleSignInTime()
                    }
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        Log.d("CalendarIntegration", "Signed in successfully: ${account.email}")
                    } catch (e: ApiException) {
                        Log.e("CalendarIntegration", "Sign-in failed with error: ${e.statusCode}")
                        Log.e("CalendarIntegration", "Error details: ${e.localizedMessage}")
                    }
                }

            googleSignInHelper.initiateGoogleSignIn(signInLauncher)
        }

        val refreshToken = googleSignInHelper.loadRefreshToken()

        if (refreshToken != null) {
            // Use the refresh token to get a new access token
            Log.d("CalendarIntegration", "Refresh token available, will get new access token $refreshToken")
            googleSignInHelper.getNewAccessTokenFromRefreshToken(
                refreshToken,
                ::onSignInSuccess,
                ::onSignInError
            )
            Log.d("LockScreen", "Register Lockscreen Receiver")
            lockScreenReceiver = LockScreenReceiver()
            val filter = IntentFilter().apply{
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(lockScreenReceiver, filter)
        } else {
            // No refresh token available, initiate Google Sign-In
            Log.d("CalendarIntegration", "Logging in for the first time")
            //googleSignInHelper.initiateGoogleSignIn(signInLauncher)

            googleSignInTime()
        }
        Log.d("LockScreen", "Outside of GoogleSignInTime")



        FirebaseMessaging.getInstance().subscribeToTopic("event_notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Subscribed to topic successfully.")
                }
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
        myAccessToken = accessToken
        Log.d("Reminder", "myAccessToken: $myAccessToken")
        // Proceed with your app logic
//        val reminderScheduler = ReminderScheduler(this)
//        reminderScheduler.startTracking()
//        CoroutineScope(Dispatchers.Main).launch{
//            delay(2*60*1000)
//            reminderScheduler.stopTracking()
//            Log.d("Reminder EventScheduler", "Fetching events stopped after 2 minutes")
//        }
//        val notificationHelper = NotificationHelper(this);

        Log.d("Reminder", "accessToken = $myAccessToken")
        val eventMap: MutableMap<String, Event> = mutableMapOf()


        Log.d("Notification", "building db")
        val db = DatabaseProvider.getDatabase(this)
        val eventDao = db.eventDao()
        Log.d("Notification", "db is built")
        val fetchEvents = FetchEvents(this);
        CoroutineScope(Dispatchers.IO).launch {
            val result1 = withContext(Dispatchers.IO){

                fetchEvents.fetchEventsFromCloud(accessToken, eventMap)
            }
            Log.d("Notification", "fetch event from cloud: $result1")
            val result2 = withContext(Dispatchers.IO){
                fetchAllEventFromDB(eventMap, eventDao)
                fetchEventFromDB(eventMap, eventDao)
                fetchNextEventFromDB(eventDao)
                deleteAllEventsFromDB(eventDao)
            }

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
        requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR), 2)
        //requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR),REQUEST_CALENDAR_ACCESS)
    }

    private suspend fun fetchEventFromDB(eventMap: MutableMap<String, Event>, eventDao: EventDao){
        eventMap.forEach { (eventId, event) ->

            val eventById = eventDao.findEventById(eventId)
            if (eventById != null) {
                Log.d("EventSearch", "get event by id: " + eventById.toString())
            } else {
                Log.d("EventSearch", "Event not found.")
            }
        }
    }

    private suspend fun fetchAllEventFromDB(eventMap: MutableMap<String, Event>, eventDao: EventDao){
        eventMap.forEach { (eventId, event) ->
            eventDao.insertAll(event)
        }
        val allEvents = eventDao.getAll()
        if (allEvents.isNotEmpty()) {
            Log.d("EventSearch", "all events: " + allEvents.toString())
        } else {
            Log.d("EventSearch", "No events found.")
        }
    }
    private suspend fun fetchNextEventFromDB(eventDao: EventDao){
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("MST")
        val currentTime = dateFormat.format(System.currentTimeMillis())
        Log.d("Notification", "currentTime $currentTime")
        val nextEvent = eventDao.getNextEvent(currentTime)
        if(nextEvent != null){
            Log.d("EventSearch", "Next event: ${nextEvent.eventName}, Start time: ${nextEvent.eventStart}")
        }
        else{
            Log.d("EventSearch", "No upcoming events found")
        }
    }
    private suspend fun deleteAllEventsFromDB(eventDao: EventDao){
        val allEvents = eventDao.getAll()
        allEvents.forEach{event->
            eventDao.delete(event)
        }
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




