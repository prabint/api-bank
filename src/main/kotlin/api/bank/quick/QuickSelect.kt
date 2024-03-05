package api.bank.quick

import api.bank.actions.ShowEditorDialogAction
import api.bank.models.Constants
import api.bank.models.RequestDetail
import api.bank.models.RequestGroup
import api.bank.modules.pluginModule
import api.bank.notification.notifyException
import api.bank.notification.notifySuccess
import api.bank.notification.notifyWarning
import api.bank.repository.CoreRepository
import api.bank.services.getEnvFromJson
import api.bank.settings.ApiBankSettingsStateComponent
import api.bank.utils.migrateRequestDetailsXmlJson
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.actions.QuickSwitchSchemeAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import java.io.File

class QuickSelect : QuickSwitchSchemeAction(), KoinComponent {

    private val coreRepository: CoreRepository by inject()
    private val gson: Gson by inject()

    private fun getRequests(project: Project): List<RequestDetail> {
        val settingsStateComponent = ApiBankSettingsStateComponent.getInstance(project)
        val rootDir = File(settingsStateComponent.state.requestFilePath)
        migrateRequestDetailsXmlJson(project, rootDir, gson)
        val jsonFile = File(rootDir, Constants.FILE_API_DETAIL_PERSISTENT)

        return gson
            .fromJson<List<RequestGroup>>(jsonFile.readText(), object : TypeToken<List<RequestGroup>>() {}.type)
            .flatMap { it.requests }
    }

    override fun fillActions(project: Project?, group: DefaultActionGroup, dataContext: DataContext) {

        GlobalContext.getOrNull() ?: GlobalContext.startKoin { modules(pluginModule) }

        val items = getRequests(project!!)
        val variableCollection = getEnvFromJson(gson, project)

        for (item in items) {
            group.add(object : AnAction(item.name) {
                override fun actionPerformed(e: AnActionEvent) {
                    GlobalScope.launch(CoroutineExceptionHandler { _, t ->
                        println(t.stackTraceToString())
                        notifyException(project, t.message ?: "API call threw exception")
                    }) {
                        val keyValueArray =
                            variableCollection.find { it.isActive == true }?.variableItems ?: emptyList()

                        val response = withContext(Dispatchers.IO) {
                            coreRepository.executeRequest(item, keyValueArray)
                        }

                        if (response.code < 400) {
                            notifySuccess(project, "${response.code} ${response.message}")
                        }

                        if (response.code >= 400) {
                            notifyWarning(project, "${response.code} ${response.message}")
                        }
                    }
                }
            })
        }

        group.add(Separator())
        group.add(ShowEditorDialogAction())
    }

    override fun getPopupTitle(e: AnActionEvent): String {
        return "Make Api Request"
    }
}
