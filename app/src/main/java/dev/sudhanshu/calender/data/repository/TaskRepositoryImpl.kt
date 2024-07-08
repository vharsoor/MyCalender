package dev.sudhanshu.calender.data.repository

import android.util.Log
import dev.sudhanshu.calender.data.model.ApiDelteTaskReq
import dev.sudhanshu.calender.data.model.ApiGetTaskReq
import dev.sudhanshu.calender.data.model.ApiSuccess
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.data.remote.ApiService
import dev.sudhanshu.calender.domain.model.TaskRequest
import dev.sudhanshu.calender.domain.repository.TaskRepository
import javax.inject.Inject


class TaskRepositoryImpl @Inject constructor(private val apiService: ApiService) : TaskRepository {

    override suspend fun storeTask(taskRequest: TaskRequest): ApiSuccess {
        return try {
             apiService.storeCalendarTask(taskRequest)
        } catch (e: Exception) {
            Log.i("--Repository--", "AddTaskAPI >> ${e.message}" )
            ApiSuccess(e.message.toString())
        }
    }

    override suspend fun getTaskList(userId: Int): List<Task> {
        return try {
            val response = apiService.getCalendarTaskLists(ApiGetTaskReq(userId))
            response.tasks
        } catch (e: Exception) {
           Log.i("--Repository--", "GetTaskAPI >> ${e.message}" )
           listOf<Task>()
        }
    }

    override suspend fun deleteTask(userId: Int, taskId: Int): ApiSuccess {
        return try {
             apiService.deleteCalendarTask(ApiDelteTaskReq(userId, taskId))
        } catch (e: Exception) {
            Log.i("--Repository--", "DeleteTaskAPI >> ${e.message}" )
            ApiSuccess(e.message.toString())
        }
    }
}


