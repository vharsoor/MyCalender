package dev.sudhanshu.calender.domain.usecases

import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.domain.repository.TaskRepository
import javax.inject.Inject


class GetTaskListUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(userId: Int): List<Task> {
        return repository.getTaskList(userId)
    }
}
