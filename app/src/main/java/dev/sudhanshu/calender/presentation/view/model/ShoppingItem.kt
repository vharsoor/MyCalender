package dev.sudhanshu.calender.presentation.view.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
