package api.bank.multitab

import api.bank.models.Constants
import api.bank.settings.ApiBankSettingsStateComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.event.ActionEvent
import javax.swing.JPanel

class SettingsTab(
    project: Project,
    private val onApply: () -> Unit
) {
    private val settings = ApiBankSettingsStateComponent.getInstance(project)
    internal val requestTextField = JBTextField(settings.state.requestFilePath)
    internal val envTextField = JBTextField(settings.state.envFilePath)

    // todo: what if user enters random file name in textfield?
    fun get(): JPanel {
        return panel {
            group("Paths") {
                row("Request file path: ") {
                    textFieldWithBrowseButton(
                        fileChooserDescriptor = FileChooserDescriptor(
                            /* chooseFiles = */ true,
                            /* chooseFolders = */ false,
                            /* chooseJars = */ false,
                            /* chooseJarsAsFiles = */ false,
                            /* chooseJarContents = */ false,
                            /* chooseMultiple = */ false
                        ).withFileFilter { it.name == Constants.FILE_API_DETAIL_PERSISTENT } // Buggy! allows any file to be selected
                    ).align(AlignX.FILL)
                }.rowComment("Must be ${Constants.FILE_API_DETAIL_PERSISTENT}")

                row("Request file path: ") {
                    cell(
                        TextFieldWithBrowseButton(requestTextField)
                            .apply {
                                // Works
                                addBrowseFolderListener(
                                    null, null, null,
                                    FileChooserDescriptor(
                                        /* chooseFiles = */ true,
                                        /* chooseFolders = */ false,
                                        /* chooseJars = */ false,
                                        /* chooseJarsAsFiles = */ false,
                                        /* chooseJarContents = */ false,
                                        /* chooseMultiple = */ false
                                    ).withFileFilter { it.name == Constants.FILE_API_DETAIL_PERSISTENT },
                                )
                            }
                    ).align(AlignX.FILL)
                }.rowComment("Must be ${Constants.FILE_API_DETAIL_PERSISTENT}")

                row("Environment file path: ") {
                    cell(
                        TextFieldWithBrowseButton(envTextField)
                            .apply {
                                addBrowseFolderListener(null, null, null, FileChooserDescriptor(
                                    /* chooseFiles = */ true,
                                    /* chooseFolders = */ false,
                                    /* chooseJars = */ false,
                                    /* chooseJarsAsFiles = */ false,
                                    /* chooseJarContents = */ false,
                                    /* chooseMultiple = */ false
                                ).withFileFilter { it.name == Constants.FILE_VARIABLE_COLLECTION_PERSISTENT })
                            }
                    ).align(AlignX.FILL)
                }.rowComment("Must be ${Constants.FILE_VARIABLE_COLLECTION_PERSISTENT}")

                row {
                    button("Restart Plugin To Apply Changes") { event: ActionEvent ->
                        onApply()
                    }.align(AlignX.CENTER)
                }
            }
        }
    }
}
