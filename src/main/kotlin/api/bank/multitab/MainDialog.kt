package api.bank.multitab

import api.bank.models.RequestGroup
import api.bank.modules.pluginModule
import api.bank.repository.CoreRepository
import api.bank.services.getEnvFromJson
import api.bank.services.saveEnvToJsonFile
import api.bank.settings.ApiBankSettingsState
import api.bank.settings.ApiBankSettingsStateComponent
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.listener.SimpleWindowListener
import api.bank.utils.migrateEnvVarXmlJson
import api.bank.utils.migrateRequestDetailsXmlJson
import api.bank.utils.saveAsJsonFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.io.File
import java.nio.file.Paths
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Main dialog that has 2 tabs: Requests and Variables
 */
class MainDialog(private val project: Project) : DialogWrapper(project), KoinComponent {
    private val ideaDir = File(Paths.get(project.basePath!!, ".idea").toString())

    init {
        GlobalContext.getOrNull() ?: GlobalContext.startKoin { modules(pluginModule) }
    }

    private val gson: Gson by inject()
    private val coreRepository: CoreRepository by inject()
    private val dispatchProvider: DispatcherProvider by inject()

    init {
        migrateEnvVarXmlJson(project, ideaDir, gson)
        migrateRequestDetailsXmlJson(project, ideaDir, gson)
    }

    private val applyAction = object : AbstractAction("Apply") {
        override fun actionPerformed(e: ActionEvent?) {
            save()
        }
    }

    private val envVarCollection = getEnvFromJson(gson, project)

    private val settingsTab = SettingsTab(project) {
        save()
        close(0, true)
    }

    private val variablesTab = VariablesTab(envVarCollection, gson)

    private val requestsTab = RequestsTab(
        gson = gson,
        coreRepository = coreRepository,
        dispatchProvider = dispatchProvider,
        treeModel = constructTreeModel(gson),
        getVariables = { variablesTab.getActive() },
    )

    private val requestTabComponent = requestsTab.get()

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
        isModal = false
    }

    override fun createCenterPanel(): JComponent {
        return JBTabbedPane().apply {
            insertTab(
                /* title = */ "Requests",
                /* icon = */ AllIcons.Ide.UpDown,
                /* component = */ requestTabComponent,
                /* tip = */ "Collection of API",
                /* index = */ 0,
            )

            insertTab(
                /* title = */ "Variables",
                /* icon = */ AllIcons.General.InlineVariablesHover,
                /* component = */ variablesTab.get(),
                /* tip = */ "Collection of environment variables",
                /* index = */ 1
            )

            insertTab(
                /* title = */ "Settings",
                /* icon = */ AllIcons.General.Settings,
                /* component = */ settingsTab.get(),
                /* tip = */ "Settings",
                /* index = */ 2
            )

            // Used to prevent bug where dialog appears in smaller size
            // because header, body and output UI fail to occupy space
            minimumSize = Dimension(700, 700)
        }
    }

    override fun createActions() = arrayOf(cancelAction, okAction, applyAction, helpAction)

    override fun getPreferredFocusedComponent() = requestsTab.tree

    override fun doOKAction() {
        save()
        super.doOKAction()
    }

    private fun constructTreeModel(gson: Gson): DefaultTreeModel {
        val root = DefaultMutableTreeNode()

        val jsonFile = File(ApiBankSettingsStateComponent.getInstance(project).state.requestFilePath)
        val jsonString = jsonFile.readText()
        val requestGroups: List<RequestGroup> = gson.fromJson(
            jsonString,
            object : TypeToken<List<RequestGroup>>() {}.type
        )

        requestGroups.forEach { requestGroup ->
            val parent = DefaultMutableTreeNode(requestGroup.groupName)
            root.add(parent)

            requestGroup.requests.forEach { request ->
                parent.add(DefaultMutableTreeNode(request))
            }
        }

        return DefaultTreeModel(root)
    }

    private fun save() {
        ApiBankSettingsStateComponent
            .getInstance(project)
            .loadState(
                ApiBankSettingsState(
                    requestFilePath = settingsTab.requestTextField.text,
                    envFilePath = settingsTab.envTextField.text,
                )
            )

        saveEnvToJsonFile(
            gson = gson,
            project = project,
            value = envVarCollection
        )

        requestsTab.getModel().saveAsJsonFile(
            project = project,
            gson = gson,
            rootDir = File(ApiBankSettingsStateComponent.getInstance(project).state.requestFilePath)
        )
    }
}
