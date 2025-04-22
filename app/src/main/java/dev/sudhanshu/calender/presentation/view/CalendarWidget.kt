/**
 * CalendarWidget.kt
 *
 * This file defines a Jetpack Glance-based home screen widget that displays a 24-hour daily view
 * of scheduled events. Each hour is represented as a row, and event blocks are visually highlighted.
 *
 * Features:
 * - Connects to the app's local database to fetch events for the current day.
 * - Highlights rows in the widget where events are scheduled, showing event names.
 * - Tapping the widget launches the MainActivity.
 * - Includes a fallback dummy event for testing.
 *
 * This widget offers a quick glance view of today's schedule directly from the home screen.
 */


package dev.sudhanshu.calender.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.background
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
//import androidx.glance.unit.dp
import dev.sudhanshu.calender.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import dev.sudhanshu.calender.presentation.view.MainActivity
import dev.sudhanshu.calender.presentation.view.DatabaseProvider
import dev.sudhanshu.calender.presentation.view.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime


/**
 * Represents an event that spans from [startHour] to [endHour],
 * meaning it covers each hour H where startHour <= H < endHour.
 */
data class EventBlock(
    val startHour: Int,
    val endHour: Int,
    val description: String
)

class DayViewWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val events = getTodayEventBlocksFromDb(context)        
        
        Log.d("DayViewWidget", "provideGlance() called for glanceId=$id")

        val calendarIntent = Intent(context, dev.sudhanshu.calender.presentation.view.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        Log.d("DayViewWidget", "Created Intent for MainActivity: $calendarIntent")

        val calendarAction = actionStartActivity(calendarIntent)
        Log.d("DayViewWidget", "Created Glance actionStartActivity for the intent")


        // Today's date
        val currentDate = LocalDate.now()

        // Sample event blocks
        //val events = sampleEvents()

        // Format today's date to show at the top
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
        val formattedDate = currentDate.format(dateFormatter)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(calendarAction)
                    .background(ColorProvider(R.color.white))  // entire widget background
                    .padding(R.dimen.widget_padding_8)
            ) {
                // Show today's date
                Text(
                    text = formattedDate,
                    style = TextStyle(color = ColorProvider(R.color.black))
                )

                Spacer(modifier = GlanceModifier.height(R.dimen.widget_padding_8))

                // Show a 24-hour schedule from 0..23
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    items(count = 24) { hour ->
                        // Check if there's an event that covers this hour
                        val matchedEvent = events.firstOrNull { event ->
                            hour >= event.startHour && hour < event.endHour
                        }

                        // If this hour is inside an event range, highlight the row
                        val rowColor = if (matchedEvent != null) {
                            // A highlight color for the event
                            ColorProvider(R.color.blue)
                        } else {
                            // White if no event
                            ColorProvider(R.color.white)
                        }

                        // Each hour row is displayed in a Column so we can
                        // show the row content + a dividing line below
                        Column {
                            // The hour row (0..23)
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .clickable(calendarAction)
                                    .height(40.dp)  // a bit of row height
                                    .background(rowColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                // Label: "HH - HH+1"
                                val timeLabel = String.format("%02d - %02d", hour, hour + 1)

                                if (matchedEvent != null) {
                                    // If there's an event, display the event text in a different color
                                    // or different text color
                                    Text(
                                        text = "$timeLabel: ${matchedEvent.description}",
                                        style = TextStyle(color = ColorProvider(R.color.white))
                                    )
                                } else {
                                    // Otherwise just the hour label plus a dash
                                    Text(
                                        text = "$timeLabel: —",
                                        style = TextStyle(color = ColorProvider(R.color.black))
                                    )
                                }
                            }

                            // Divider line at the bottom of each hour row
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ColorProvider(R.color.gray))
                            ) {
                                // Leave this block empty if you just want a line/divider
                            }

                        }
                    }
                }
            }
        }
    }

    private suspend fun getTodayEventBlocksFromDb(context: Context): List<EventBlock> {
        return withContext(Dispatchers.IO) {
            val dao = DatabaseProvider.getDatabase(context).eventDao()
            val allEvents: List<Event> = dao.getAll()
            Log.d("DayViewWidget", "All events in DB: $allEvents")

            val today = LocalDate.now()
            Log.d("DayViewWidget", "Checking events for today=$today")

            val todayEventBlocks = allEvents.mapNotNull { e ->
                try {
                    // Log the raw DB strings
                    Log.d("DayViewWidget", "Fetched DB eventId=${e.eventId}, rawStart=${e.eventStart}, rawEnd=${e.eventEnd}")

                    val startZdt = ZonedDateTime.parse(e.eventStart)
                    val endZdt   = ZonedDateTime.parse(e.eventEnd)

                    val localStartZdt = startZdt.withZoneSameInstant(java.time.ZoneId.systemDefault())
                    val localEndZdt   = endZdt.withZoneSameInstant(java.time.ZoneId.systemDefault())

                    // Log the local date/time after conversion
                    Log.d("DayViewWidget", "Local times => start=$localStartZdt, end=$localEndZdt")

                    // Compare to today's local date
                    if (localStartZdt.toLocalDate() == today) {
                        Log.d("DayViewWidget", " --> This event is happening TODAY! Will display in widget.")
                        EventBlock(
                            startHour   = localStartZdt.hour,
                            endHour     = localEndZdt.hour,
                            description = e.eventName ?: "No Title"
                        )
                    } else {
                        Log.d("DayViewWidget", " --> Event date is NOT today; skipping.")
                        null
                    }
                } catch (ex: Exception) {
                    Log.e("DayViewWidget", "Failed to parse eventId=${e.eventId} start/end. Error: ${ex.message}")
                    null
                }
            }

            // Example of adding your dummy event for debugging
            val dummyEvent = EventBlock(
                startHour   = 17, // 2 PM
                endHour     = 18, // 3 PM
                description = "Thesis Defense"
            )
            (todayEventBlocks + dummyEvent).also {
                Log.d("DayViewWidget", "Total events (real + dummy) for widget: ${it.size}")
            }
        }
    }




    /**
     * Sample data: 10-12, 16-18, etc.
     * If an event is from 10..12, it covers hours 10 and 11.
     */
    private fun sampleEvents(): List<EventBlock> {
        return listOf(
            // Covers hour 10 and 11
            EventBlock(startHour = 10, endHour = 12, description = "Morning Meeting"),
            // Covers hour 16 and 17
            EventBlock(startHour = 16, endHour = 18, description = "Doctor Appointment")
        )
    }

    class DayViewWidgetReceiver : GlanceAppWidgetReceiver() {
        override val glanceAppWidget: GlanceAppWidget
            get() = DayViewWidget()

        override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            super.onUpdate(context, appWidgetManager, appWidgetIds)
            val componentName = ComponentName(context, DayViewWidgetReceiver::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            widgetIds.forEach { widgetId ->
                // Typically no need for Glance to call notifyAppWidgetViewDataChanged
            }
            Log.d("DayViewWidget", "onUpdate called, widgetIds=${widgetIds.joinToString()}")
        }

        override fun onReceive(context: Context, intent: Intent) {
            super.onReceive(context, intent)
            if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, DayViewWidgetReceiver::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                widgetIds.forEach { widgetId ->
                    // Typically no need for Glance to call notifyAppWidgetViewDataChanged
                }
                Log.d("DayViewWidget", "onReceive: ACTION_APPWIDGET_UPDATE triggered")
            }
        }





    }
}
