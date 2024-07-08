package dev.sudhanshu.calender.domain.model

import com.google.gson.annotations.SerializedName
import dev.sudhanshu.calender.data.model.TaskModel

data class TaskRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("task")
    val task: TaskModel
)