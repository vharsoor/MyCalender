package dev.sudhanshu.calender.data.remote

import dev.sudhanshu.calender.data.model.ApiGetTaskReq
import dev.sudhanshu.calender.data.model.ApiSuccess
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.data.model.TaskResponse
import dev.sudhanshu.calender.domain.model.TaskRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query


interface ApiService {
    @POST("/api/storeCalendarTask")
    suspend fun storeCalendarTask(@Body taskRequest: TaskRequest): ApiSuccess

    @POST("/api/getCalendarTaskList")
    suspend fun getCalendarTaskLists(@Body userId: ApiGetTaskReq): TaskResponse

    @POST("/api/deleteCalendarTask")
    suspend fun deleteCalendarTask(@Query("user_id") userId: Int, @Query("task_id") taskId: Int): ApiSuccess
}
