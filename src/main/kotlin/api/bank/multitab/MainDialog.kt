package api.bank.multitab

import api.bank.models.RequestDetail
import api.bank.models.VariableCollection
import api.bank.modules.pluginModule
import api.bank.repository.CoreRepository
import api.bank.services.ApiDetailPersistentService
import api.bank.services.VariableCollectionPersistentService
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.listener.SimpleWindowListener
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

/**
 * Main dialog that has 2 tabs: Requests and Variables
 */
class MainDialog(private val project: Project) : DialogWrapper(project), KoinComponent {

    init {
        GlobalContext.getOrNull() ?: GlobalContext.startKoin { modules(pluginModule) }
    }

    private val gson: Gson by inject()
    private val coreRepository: CoreRepository by inject()
    private val dispatchProvider: DispatcherProvider by inject()

    private val persistentDetails
        get() = ApiDetailPersistentService.getInstance(project).requestDetailListState

    private val persistentVariable
        get() = VariableCollectionPersistentService.getInstance(project).collection

    // Deep copy
    private val modifiableRequestDetails = ArrayList<RequestDetail>().apply {
        persistentDetails.items.map { item -> add(gson.fromJson(gson.toJson(item), RequestDetail::class.java)) }
    }

    // Deep copy
    private val modifiableVariable = ArrayList<VariableCollection>().apply {
        persistentVariable.items.map { item -> add(gson.fromJson(gson.toJson(item), VariableCollection::class.java)) }
    }

    private val applyAction = object : AbstractAction("Apply") {
        override fun actionPerformed(e: ActionEvent?) {
            save()
        }
    }

    private val variablesTab = VariablesTab(modifiableVariable, gson)
    private val requestsTab = RequestsTab(gson, coreRepository, dispatchProvider, modifiableRequestDetails) {
        return@RequestsTab variablesTab.getActive()
    }.get()

    init {
        init()
        title = "API Request Editor"
        getButton(okAction)?.text = "Save"
        getButton(cancelAction)?.text = "Close"
        window.addWindowListener(object : SimpleWindowListener {
            override fun windowClosed(e: WindowEvent?) {
                stopKoin()
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        return JBTabbedPane().apply {
            insertTab(
                "Requests",
                AllIcons.Ide.UpDown,
                requestsTab,
                "Collection of API",
                0,
            )

            insertTab(
                "Variables",
                AllIcons.General.InlineVariablesHover,
                variablesTab.get(),
                "Collection of environment variables",
                1
            )

            // Used to prevent bug where dialog appears in smaller size
            // because header, body and output UI fail to occupy space
            minimumSize = Dimension(700, 700)
        }
    }

    override fun createActions() = arrayOf(cancelAction, okAction, applyAction, helpAction)

    override fun doOKAction() {
        save()
        super.doOKAction()
    }

    private fun save() {
        persistentDetails.items.clear()
        persistentVariable.items.clear()

        persistentDetails.items.addAll(modifiableRequestDetails.map { it.copy() })
        persistentVariable.items.addAll(modifiableVariable.map { it.copy() })
    }
}
