package dev.sudhanshu.calender




import dev.sudhanshu.calender.data.remote.ApiService
import dev.sudhanshu.calender.data.repository.TaskRepositoryImpl
import dev.sudhanshu.calender.domain.model.TaskModel
import dev.sudhanshu.calender.domain.model.TaskRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TaskRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)

        repository = TaskRepositoryImpl(apiService)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test getTasks returns task list`() = runBlocking {
        val mockResponse = MockResponse()
        mockResponse.setBody("""{"tasks":[{"user_id": 1, "task_detail": {"title": "Test", "description": "Description", "date": "01/01/2022", "time": "12:00 PM"}}]}""")
        mockWebServer.enqueue(mockResponse)

        val tasks = repository.getTaskList(1)
        assert(tasks.isNotEmpty())
        assert(tasks[0].task_detail.title == "Test")
    }

    @Test
    fun `test storeTask stores task`() = runBlocking {
        val mockResponse = MockResponse()
        mockResponse.setBody("""{"message": "Task stored successfully"}""")
        mockWebServer.enqueue(mockResponse)

        val taskRequest = TaskRequest(1, dev.sudhanshu.calender.data.model.TaskModel("title", "description", "date", "time"))
        val response = repository.storeTask(taskRequest)
        assert(response.status == "Task stored successfully")
    }

    @Test
    fun `test deleteTask deletes task`() = runBlocking {
        val mockResponse = MockResponse()
        mockResponse.setBody("""{"message": "Task deleted successfully"}""")
        mockWebServer.enqueue(mockResponse)

        val response = repository.deleteTask(1, 1)
        assert(response.status == "Task deleted successfully")
    }
}
