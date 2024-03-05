package api.bank.quick

import api.bank.actions.ShowEditorDialogAction
import api.bank.models.RequestDetail
import api.bank.models.VariableCollection
import api.bank.modules.pluginModule
import api.bank.notification.notifyException
import api.bank.notification.notifySuccess
import api.bank.notification.notifyWarning
import api.bank.repository.CoreRepository
import api.bank.utils.getRequestDetailsFromJson
import api.bank.utils.getVariableCollectionFromJson
import api.bank.utils.migrateEnvVarXmlJson
import api.bank.utils.migrateRequestDetailsXmlJson
import com.google.gson.Gson
import com.intellij.ide.actions.QuickSwitchSchemeAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

class QuickSelect : QuickSwitchSchemeAction(), KoinComponent {

    private val coreRepository: CoreRepository by inject()
    private val gson: Gson by inject()

    private fun getRequests(project: Project): List<RequestDetail> {
        migrateRequestDetailsXmlJson(project, gson)
        return getRequestDetailsFromJson(gson, project)
    }

    private fun getEnvVar(project: Project): ArrayList<VariableCollection> {
        migrateEnvVarXmlJson(project, gson)
        return getVariableCollectionFromJson(gson, project)
    }

    override fun fillActions(project: Project, group: DefaultActionGroup, dataContext: DataContext) {
        GlobalContext.getOrNull() ?: GlobalContext.startKoin { modules(pluginModule) }

        val requests = getRequests(project)
        val envVars = getEnvVar(project)

        for (item in requests) {
            group.add(object : AnAction(item.name) {
                override fun actionPerformed(e: AnActionEvent) {
                    GlobalScope.launch(CoroutineExceptionHandler { _, t ->
                        println(t.stackTraceToString())
                        notifyException(project, t.message ?: "API call threw exception")
                    }) {
                        val keyValueArray =
                            envVars.find { it.isActive == true }?.variableItems ?: emptyList()

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
