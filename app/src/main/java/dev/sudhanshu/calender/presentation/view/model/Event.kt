package dev.sudhanshu.calender.presentation.view.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import dev.sudhanshu.calender.presentation.view.model.Converters

@Entity(tableName = "events")
@TypeConverters(Converters::class)
data class Event(
    @PrimaryKey val eventId: String,
    @ColumnInfo(name="event_name") val eventName: String?,
    @ColumnInfo(name="event_start") val eventStart: String?,
    @ColumnInfo(name="event_end") val eventEnd: String?,
    @ColumnInfo(name="Link") val eventLink: String? = null,
    //@ColumnInfo(name="summary") val summary: String?
)