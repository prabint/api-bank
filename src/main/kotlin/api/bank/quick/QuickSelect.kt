package api.bank.quick

import api.bank.actions.ShowEditorDialogAction
import api.bank.modules.pluginModule
import api.bank.notification.notifyException
import api.bank.notification.notifySuccess
import api.bank.notification.notifyWarning
import api.bank.repository.CoreRepository
import api.bank.services.ApiDetailPersistentService
import api.bank.services.VariableCollectionPersistentService
import com.intellij.ide.actions.QuickSwitchSchemeAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

class QuickSelect : QuickSwitchSchemeAction(), KoinComponent {

    private val coreRepository: CoreRepository by inject()

    override fun fillActions(project: Project?, group: DefaultActionGroup, dataContext: DataContext) {

        GlobalContext.getOrNull() ?: GlobalContext.startKoin { modules(pluginModule) }

        val items = ApiDetailPersistentService.getInstance(project!!).requestDetailListState.items
        val variableCollection = VariableCollectionPersistentService.getInstance(project).collection.items

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
