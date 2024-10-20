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

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val eventId: String,
    @ColumnInfo(name="event_name") val eventName: String?,
    @ColumnInfo(name="event_start") val eventStart: String?
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
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}