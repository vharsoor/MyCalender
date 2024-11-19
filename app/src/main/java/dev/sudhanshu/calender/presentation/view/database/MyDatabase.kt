package dev.sudhanshu.calender.presentation.view.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.sudhanshu.calender.presentation.view.model.Converters
import dev.sudhanshu.calender.presentation.view.model.Event
import dev.sudhanshu.calender.presentation.view.dao.EventDao
import dev.sudhanshu.calender.presentation.view.dao.ShoppingItemDao
import dev.sudhanshu.calender.presentation.view.model.ShoppingItem

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}



@Database(entities = [Event::class, ShoppingItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun shoppingItemDao(): ShoppingItemDao
}