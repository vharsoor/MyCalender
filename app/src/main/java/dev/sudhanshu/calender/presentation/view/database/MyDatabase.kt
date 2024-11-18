package dev.sudhanshu.calender.presentation.view.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(entities = [ShoppingItem::class], version=1, exportSchema = false)
abstract class AppDatabase:RoomDatabase(){
    abstract fun shoppingItemDao():ShoppingItemDao
}