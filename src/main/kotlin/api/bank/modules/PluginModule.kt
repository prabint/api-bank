package api.bank.modules

import api.bank.repository.CoreRepository
import api.bank.repository.DefaultCoreRepository
import api.bank.utils.FileManager
import api.bank.utils.dispatcher.DefaultDispatcherProvider
import api.bank.utils.dispatcher.DispatcherProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val pluginModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<CoreRepository> { DefaultCoreRepository(get()) }
    single<Gson> { GsonBuilder().setPrettyPrinting().create() }
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<Logger> { Logger.getInstance("api.bank") }
    single { (project: Project) -> FileManager(project, get(), get()) }
}
