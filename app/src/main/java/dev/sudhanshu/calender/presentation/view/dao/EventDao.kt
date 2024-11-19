package dev.sudhanshu.calender.presentation.view.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sudhanshu.calender.presentation.view.model.Event


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
