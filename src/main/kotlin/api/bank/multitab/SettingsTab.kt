package api.bank.multitab

import api.bank.models.Constants
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class SettingsTab(
    project: Project,
    private val onApply: () -> Unit
) {
    private val settings = ApiBankSettingsPersistentStateComponent.getInstance(project)
    internal val requestTextField = JBTextField(settings.state.requestFilePath).apply { isEnabled = false }
    internal val envTextField = JBTextField(settings.state.envFilePath).apply { isEnabled = false }
    private val fileChooserDescriptor
        get() = FileChooserDescriptor(
            /* chooseFiles = */ true,
            /* chooseFolders = */ false,
            /* chooseJars = */ false,
            /* chooseJarsAsFiles = */ false,
            /* chooseJarContents = */ false,
            /* chooseMultiple = */ false
        )

    fun get() = panel {
        group("Paths") {
            row("Request file path: ") {
                cell(
                    TextFieldWithBrowseButton(requestTextField)
                        .apply {
                            addBrowseFolderListener(
                                null,
                                null,
                                null,
                                fileChooserDescriptor.withFileFilter { it.name == Constants.FILE_API_DETAIL_PERSISTENT },
                            )
                            this.alignmentX = JComponent.RIGHT_ALIGNMENT
                        }
                )
            }.rowComment("Must be ${Constants.FILE_API_DETAIL_PERSISTENT}")

            row("Environment file path: ") {
                cell(
                    TextFieldWithBrowseButton(envTextField)
                        .apply {
                            addBrowseFolderListener(
                                null,
                                null,
                                null,
                                fileChooserDescriptor.withFileFilter { it.name == Constants.FILE_VARIABLE_COLLECTION_PERSISTENT })
                        }
                )
            }.rowComment("Must be ${Constants.FILE_VARIABLE_COLLECTION_PERSISTENT}")

            row {
                button("Restart Plugin To Apply Changes") { onApply() }
            }
        }
    }
}
