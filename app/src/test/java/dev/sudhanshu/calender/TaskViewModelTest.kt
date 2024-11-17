package dev.sudhanshu.calender

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dev.sudhanshu.calender.data.model.Task
import dev.sudhanshu.calender.data.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel
import dev.sudhanshu.calender.domain.usecases.DeleteTaskUseCase
import dev.sudhanshu.calender.domain.usecases.GetTaskListUseCase
import dev.sudhanshu.calender.domain.usecases.StoreTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var storeTaskUseCase: StoreTaskUseCase

    @Mock
    private lateinit var getTaskListUseCase: GetTaskListUseCase

    @Mock
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = TaskViewModel(storeTaskUseCase, getTaskListUseCase, deleteTaskUseCase)
    }

    @Test
    fun `storeTask invokes use case`() = runBlockingTest {
        val taskRequest = TaskRequest(8305, TaskModel ("title", "description", "date", "time"))
        viewModel.storeTask(taskRequest, {}, {})
        verify(storeTaskUseCase).invoke(taskRequest)
    }

    @Test
    fun `getTaskList invokes use case`() = runBlockingTest {
        viewModel.getTaskList(1, {}, {})
        verify(getTaskListUseCase).invoke(1)
    }

    @Test
    fun `deleteTask invokes use case`() = runBlockingTest {
        viewModel.deleteTask(1, 1, {}, {})
        verify(deleteTaskUseCase).invoke(1, 1)
    }
}
