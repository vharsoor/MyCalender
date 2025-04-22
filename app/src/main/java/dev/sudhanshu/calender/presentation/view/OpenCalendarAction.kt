/**
 * OpenCalendarAction.kt
 *
 * Glance widget action that opens MainActivity with a selected day.
 * - Triggered from widget interaction.
 * - Passes the day as an intent extra for context-aware navigation.
 */


package dev.sudhanshu.calender.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import android.util.Log

public class OpenCalendarAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val day = parameters[ActionParameters.Key<String>("day")] ?: ""
        Log.d("CalendarWidget", "OpenCalendarAction triggered for day: $day")

        val intent = Intent(context, dev.sudhanshu.calender.presentation.view.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("selected_day", day)
        }
        context.startActivity(intent)
    }
}
