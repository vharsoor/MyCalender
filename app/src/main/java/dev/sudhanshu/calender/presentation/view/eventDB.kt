package dev.sudhanshu.calender.presentation.view

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import dev.sudhanshu.calender.presentation.view.InsertTask.ConferenceData
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.room.TypeConverters

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

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    suspend fun getAll(): List<Event>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    suspend fun findEventById(id: String): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg events: Event)

    @Query("SELECT * FROM events WHERE event_start > :currentTime ORDER BY event_start ASC LIMIT 1")
    suspend fun getNextEvent(currentTime: String): Event?
    @Delete
    suspend fun delete(event: Event)
}


@Database(entities = [Event::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}

class Converters {

    @TypeConverter
    fun fromConferenceData(conferenceData: ConferenceData?): String? {
        return Gson().toJson(conferenceData)
    }

    @TypeConverter
    fun toConferenceData(conferenceDataString: String?): ConferenceData? {
        return if (conferenceDataString == null) null else Gson().fromJson(conferenceDataString, object : TypeToken<ConferenceData>() {}.type)
    }
}