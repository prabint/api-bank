package api.bank.multitab

import api.bank.models.Constants
import api.bank.models.ImportSource
import api.bank.models.Result.Error
import api.bank.models.Result.Success
import api.bank.models.SchemaType
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import api.bank.utils.FileManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel

class SettingsTab(
    private val project: Project,
    private val logger: Logger,
    private val settings: ApiBankSettingsPersistentStateComponent,
    private val fileManager: FileManager,
    private val onApply: () -> Unit,
) {
    internal val requestTextField = JBTextField(settings.state.requestFilePath).apply { isEnabled = false }
    internal val envTextField = JBTextField(settings.state.envFilePath).apply { isEnabled = false }
    private val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")

    private val requestGroupErrorLabel = JBLabel().apply {
        foreground = JBColor.RED
        isVisible = false
    }

    private val variablesErrorLabel = JBLabel().apply {
        foreground = JBColor.RED
        isVisible = false
    }

    enum class SelectionType {
        RequestGroups,
        VariableCollection,
    }

    fun get() = panel {
        group("Paths") {
            row("Request filepath: ") {
                cell(getTextFieldWithBrowseButton(SelectionType.RequestGroups))
            }.rowComment("Example: ${Constants.FILE_API_DETAIL_PERSISTENT}")

            row { cell(requestGroupErrorLabel) }

            row("Environment filepath: ") {
                cell(getTextFieldWithBrowseButton(SelectionType.VariableCollection))
            }.rowComment("Example: ${Constants.FILE_VARIABLE_COLLECTION_PERSISTENT}")

            row { cell(variablesErrorLabel) }

            row {
                button("Restart Plugin To Apply Changes") { onApply() }
            }
        }
    }

    private fun getTextFieldWithBrowseButton(selectionType: SelectionType): TextFieldWithBrowseButton {
        val filePathTextField: JBTextField = when (selectionType) {
            SelectionType.RequestGroups -> requestTextField
            SelectionType.VariableCollection -> envTextField
        }

        return TextFieldWithBrowseButton(filePathTextField) {
            FileChooser.chooseFile(fileChooserDescriptor, project, null)?.let { selectedFile: VirtualFile ->
                val text = selectedFile.readText()
                val detectedSchemaType = fileManager.getSchemaType(
                    text = text,
                    filePath = selectedFile.path
                )

                val errorLabel = when (selectionType) {
                    SelectionType.RequestGroups -> requestGroupErrorLabel
                    SelectionType.VariableCollection -> variablesErrorLabel
                }

                val previousPath = when (selectionType) {
                    SelectionType.RequestGroups -> settings.state.requestFilePath
                    SelectionType.VariableCollection -> settings.state.envFilePath
                }

                fun setErrorLabel(message: String?) {
                    errorLabel.text = message
                    errorLabel.isVisible = true
                    errorLabel.foreground = JBColor.RED
                    filePathTextField.text = previousPath
                }

                val expectedType =
                    if (selectionType == SelectionType.RequestGroups && detectedSchemaType != SchemaType.REQUESTS) {
                        SchemaType.REQUESTS
                    } else if (selectionType == SelectionType.VariableCollection && detectedSchemaType != SchemaType.VARIABLES) {
                        SchemaType.VARIABLES
                    } else {
                        null
                    }

                if (expectedType != null) {
                    logger.error("Expected $expectedType but was $detectedSchemaType")
                    setErrorLabel("Did you mean to import a $expectedType json file? Detected type was $detectedSchemaType")
                    return@TextFieldWithBrowseButton
                }

                val result = fileManager.importBank(
                    importSource = ImportSource(
                        path = selectedFile.path,
                        text = text,
                        write = { selectedFile.writeText(it) }
                    ),
                )

                when (result) {
                    is Error -> {
                        logger.error("Settings tab import error. ${result.message}", result.exception)
                        setErrorLabel(result.message)
                    }

                    is Success<*> -> {
                        logger.debug("Settings tab import success")
                        errorLabel.isVisible = true
                        errorLabel.foreground = JBColor.GREEN
                        errorLabel.text = "Import success"
                        filePathTextField.text = selectedFile.path
                    }
                }
            }
        }
    }
}
