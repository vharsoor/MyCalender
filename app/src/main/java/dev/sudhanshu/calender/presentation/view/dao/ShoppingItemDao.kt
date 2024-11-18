package dev.sudhanshu.calender.presentation.view.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sudhanshu.calender.presentation.view.model.ShoppingItem

@Dao
interface ShoppingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Query("SELECT * FROM items")
    suspend fun getAllItems():List<ShoppingItem>

    @Delete
    suspend fun deleteItem(item: ShoppingItem)
}

