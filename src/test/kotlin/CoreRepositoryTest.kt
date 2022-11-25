import api.bank.models.RequestDetail
import api.bank.repository.CoreRepository
import api.bank.repository.DefaultCoreRepository
import api.bank.utils.dispatcher.DispatcherProvider
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.mockito.kotlin.mock

// TODO: Write tests
class CoreRepositoryTest {
    private val dispatcherProvider: DispatcherProvider = mock()
    private val coreRepository: CoreRepository = DefaultCoreRepository(dispatcherProvider)

    @org.junit.jupiter.api.Test
    @Ignore
    fun `sample test`() = runBlocking {
        val requestDetail = RequestDetail(
            id = "id",
            name = "name",
            url = "http://google.com",
            method = "GET",
            header = arrayListOf(),
            body = null
        )
    }
}
