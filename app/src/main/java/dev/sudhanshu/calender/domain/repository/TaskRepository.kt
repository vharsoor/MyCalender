package dev.sudhanshu.calender.domain.repository

import dev.sudhanshu.calender.data.model.ApiSuccess
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest

interface TaskRepository {
    suspend fun storeTask(taskRequest: TaskRequest): ApiSuccess
    suspend fun getTaskList(userId: Int): List<Task>
    suspend fun deleteTask(userId: Int, taskId: Int): ApiSuccess
}
