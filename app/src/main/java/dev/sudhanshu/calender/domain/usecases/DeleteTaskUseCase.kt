package dev.sudhanshu.calender.domain.usecases

import dev.sudhanshu.calender.data.model.ApiSuccess
import dev.sudhanshu.calender.domain.repository.TaskRepository
import javax.inject.Inject


class DeleteTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(userId: Int, taskId: Int): ApiSuccess {
        return repository.deleteTask(userId, taskId)
    }
}
