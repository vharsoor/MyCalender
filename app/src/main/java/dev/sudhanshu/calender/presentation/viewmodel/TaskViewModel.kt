package dev.sudhanshu.calender.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.usecases.DeleteTaskUseCase
import dev.sudhanshu.calender.domain.usecases.GetTaskListUseCase
import dev.sudhanshu.calender.domain.usecases.StoreTaskUseCase
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewModelScope
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.domain.model.TaskRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


@HiltViewModel
class TaskViewModel @Inject constructor(
    private val storeTaskUseCase: StoreTaskUseCase,
    private val getTaskListUseCase: GetTaskListUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {


    private val _task = MutableStateFlow<List<Task>?>(null)
    val taskList: StateFlow<List<Task>?> = _task


    fun storeTask(taskRequest: TaskRequest, onSuccess: () -> Unit, onError: () -> Unit)  {
        viewModelScope.launch {
            try {
                storeTaskUseCase.invoke(taskRequest)
                onSuccess()
            }catch (e : Exception){
                onError()
                Log.i("--ViewModel--", "AddTaskList >> ${e.message}")
            }
        }
    }

    fun getTaskListByDate(userId: Int, date: String? = null, onSuccess: (List<Task>) -> Unit, onError: (String) -> Unit)  {
        viewModelScope.launch {
            try {
                val tasks = getTaskListUseCase(userId)
                _task.value = filterTasksByDate(tasks, date)
                onSuccess(tasks)
            } catch (e: Exception) {
                Log.i("--ViewModel--", "getTaskList >> ${e.message}")
                onError(e.message ?: "Failed to get user tasks")
            }
        }
    }

    fun getTaskList(userId: Int, onSuccess: (List<Task>) -> Unit, onError: (String) -> Unit)  {
        viewModelScope.launch {
            try {
                val tasks = getTaskListUseCase(userId)
                _task.value = tasks
                onSuccess(tasks)
            } catch (e: Exception) {
                Log.i("--ViewModel--", "getTaskList >> ${e.message}")
                onError(e.message ?: "Failed to get user tasks")
            }
        }
    }

    fun deleteTask(userId: Int, taskId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase.invoke(userId, taskId)
                onSuccess()
            }catch (e : Exception){
                Log.i("--ViewModel--", "getTaskList >> ${e.message}")
                onError(e.message.toString())
            }
        }
    }

    private fun filterTasksByDate(tasks: List<Task>, date: String?): List<Task> {
        return if (date.isNullOrEmpty()) {
            Log.i("--ViewModel--", "filter task >> data is null")
            tasks
        } else {
            Log.i("--ViewModel--", "filter task >> $date  ${tasks.size}")
            tasks.filter { it.task_detail.date == date }
        }
    }
}
