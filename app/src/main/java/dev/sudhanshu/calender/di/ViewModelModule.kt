package dev.sudhanshu.calender.di

import dev.sudhanshu.calender.domain.repository.TaskRepository


import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dev.sudhanshu.calender.domain.usecases.DeleteTaskUseCase
import dev.sudhanshu.calender.domain.usecases.GetTaskListUseCase
import dev.sudhanshu.calender.domain.usecases.StoreTaskUseCase
import dev.sudhanshu.calender.presentation.viewmodel.TaskViewModel

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideStoreTaskUseCase(taskRepository: TaskRepository): StoreTaskUseCase {
        return StoreTaskUseCase(taskRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetTaskListUseCase(taskRepository: TaskRepository): GetTaskListUseCase {
        return GetTaskListUseCase(taskRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideDeleteTaskUseCase(taskRepository: TaskRepository): DeleteTaskUseCase {
        return DeleteTaskUseCase(taskRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideTaskViewModel(
        storeTaskUseCase: StoreTaskUseCase,
        getTaskListUseCase: GetTaskListUseCase,
        deleteTaskUseCase: DeleteTaskUseCase
    ): TaskViewModel {
        return TaskViewModel(storeTaskUseCase, getTaskListUseCase, deleteTaskUseCase)
    }
}
