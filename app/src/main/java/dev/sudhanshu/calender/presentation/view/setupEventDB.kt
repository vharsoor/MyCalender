/**
 * DatabaseProvider.kt
 *
 * Singleton object to provide a Room database instance.
 * - Ensures only one instance of AppDatabase is created.
 * - Uses fallbackToDestructiveMigration for simplicity in schema changes.
 */

package dev.sudhanshu.calender.presentation.view

import android.content.Context
import androidx.room.Room

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