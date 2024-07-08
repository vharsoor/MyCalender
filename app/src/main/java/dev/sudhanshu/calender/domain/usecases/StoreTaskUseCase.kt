package dev.sudhanshu.calender.domain.usecases

import dev.sudhanshu.calender.data.model.ApiSuccess
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest
import dev.sudhanshu.calender.domain.repository.TaskRepository
import javax.inject.Inject


class StoreTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(taskRequest: TaskRequest): ApiSuccess {
        return repository.storeTask(taskRequest)
    }
}
