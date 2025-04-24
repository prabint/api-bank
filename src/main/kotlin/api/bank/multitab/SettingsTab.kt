package api.bank.multitab

import api.bank.models.Constants
import api.bank.models.RequestGroup
import api.bank.models.VariableCollection
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.readText
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.annotations.VisibleForTesting

class SettingsTab(
    private val project: Project,
    private val gson: Gson,
    private val logger: Logger,
    private val settings: ApiBankSettingsPersistentStateComponent,
    private val onApply: () -> Unit
) {
    internal val requestTextField = JBTextField(settings.state.requestFilePath).apply { isEnabled = false }
    internal val envTextField = JBTextField(settings.state.envFilePath).apply { isEnabled = false }
    private val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(".json")

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

    data class Result(
        val success: Boolean,
        val message: String
    )

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
            FileChooser.chooseFile(fileChooserDescriptor, project, null)?.let { selectedFile ->
                val fileAsJsonString = selectedFile.readText()

                val result = when (selectionType) {
                    SelectionType.RequestGroups -> isValidRequestGroupFile(fileAsJsonString)
                    SelectionType.VariableCollection -> isValidEnvVarFile(fileAsJsonString)
                }

                val errorLabel = when (selectionType) {
                    SelectionType.RequestGroups -> requestGroupErrorLabel
                    SelectionType.VariableCollection -> variablesErrorLabel
                }

                val previousPath = when (selectionType) {
                    SelectionType.RequestGroups -> settings.state.requestFilePath
                    SelectionType.VariableCollection -> settings.state.envFilePath
                }

                if (result.success) {
                    errorLabel.isVisible = false
                    errorLabel.text = ""
                    filePathTextField.text = selectedFile.path
                } else {
                    errorLabel.text = result.message
                    errorLabel.isVisible = true
                    filePathTextField.text = previousPath
                }
            }
        }
    }

    @VisibleForTesting
    internal fun isValidEnvVarFile(jsonString: String): Result {
        val element = try {
            JsonParser.parseString(jsonString)
        } catch (e: JsonSyntaxException) {
            logger.error(e)
            return Result(success = false, message = "Parsing failed. ${e.stackTraceToString()}")
        }

        val fieldsPresent = element.isJsonArray && element.asJsonArray.all {
            it.isJsonObject &&
                    it.asJsonObject.has("id") &&
                    it.asJsonObject.has("name") &&
                    it.asJsonObject.has("variableItems") &&
                    it.asJsonObject.has("isActive")
        }

        if (!fieldsPresent) {
            logger.error("Missing fields in EnvVar json file: $jsonString")
            return Result(
                success = false,
                message = "Check your json file for missing id, name, variableItems or isActive fields"
            )
        }

        return try {
            gson.fromJson<List<VariableCollection>>(
                jsonString,
                object : TypeToken<List<VariableCollection>>() {}.type
            )
            Result(success = true, message = "")
        } catch (e: Exception) {
            logger.error(e)
            Result(success = false, message = "Parsing failed. ${e.stackTraceToString()}")
        }
    }

    @VisibleForTesting
    internal fun isValidRequestGroupFile(jsonString: String): Result {
        val element = try {
            JsonParser.parseString(jsonString)
        } catch (e: JsonSyntaxException) {
            logger.error(e)
            return Result(success = false, message = "Parsing failed. ${e.stackTraceToString()}")
        }

        val fieldsPresent = element.isJsonArray && element.asJsonArray.all {
            it.isJsonObject &&
                    it.asJsonObject.has("groupName") &&
                    it.asJsonObject.has("requests")
        }

        if (!fieldsPresent) {
            logger.error("Missing fields in RequestDetails json file: $jsonString")
            return Result(
                success = false,
                message = "Check your json file for missing groupName or requests fields"
            )
        }

        return try {
            gson.fromJson<List<RequestGroup>>(
                jsonString,
                object : TypeToken<List<RequestGroup>>() {}.type
            )
            Result(success = true, message = "")
        } catch (e: Exception) {
            logger.error(e)
            Result(success = false, message = "Parsing failed. ${e.stackTraceToString()}")
        }
    }
}
