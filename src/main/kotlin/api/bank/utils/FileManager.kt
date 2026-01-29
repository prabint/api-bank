package api.bank.utils

import api.bank.models.RequestCollection
import api.bank.models.VariableCollection
import api.bank.notification.notifyException
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.koin.core.annotation.Singleton
import java.io.File
import javax.swing.tree.TreeModel

@Singleton
class FileManager(
    private val project: Project,
    private val json: Json,
    private val logger: Logger
) {
    private var isJsonReadOnly = false

    fun migrateAll() {
        migrate(getVariablesFile())
        migrate(getRequestsFile())
    }

    fun migrate(file: File) {
        var element = json.parseToJsonElement(file.readText())
        var version = getJsonSchemaVersion(element)
        var migrated = false
        logger.debug("migrate($file): $version -> $SUPPORTED_SCHEMA_VERSION")

        when {
            version == -1 -> {
                notifyException(project, "Failed to parse schema version for ${file.path}")
                return
            }

            version > SUPPORTED_SCHEMA_VERSION -> {
                isJsonReadOnly = true
                notifyException(project, "Plugin update required. ${file.path} was created for a newer version.")
                return
            }
        }

        while (version < SUPPORTED_SCHEMA_VERSION) {
            element = when (version) {
                0 -> migrate0to1(element)
                else -> {
                    notifyException(project, "Unsupported migration path: $version")
                    return
                }
            }
            version++
            migrated = true
        }

        if (migrated) {
            file.writeText(json.encodeToString(element))
            logger.debug("Migration complete")
        }
    }

    fun getVariablesFile(): File {
        return getJsonFile(ApiBankSettingsPersistentStateComponent.getInstance(project).state.envFilePath)
    }

    fun getVariableCollectionFromJson(): VariableCollection {
        return json.decodeFromString(getVariablesFile().readText())
    }

    fun getRequestDetailsFromJson(): RequestCollection {
        return json.decodeFromString<RequestCollection>(getRequestsFile().readText())
    }

    fun getRequestsFile(): File {
        return getJsonFile(ApiBankSettingsPersistentStateComponent.getInstance(project).state.requestFilePath)
    }

    fun save(envVarCollection: VariableCollection, model: TreeModel) {
        if (isJsonReadOnly) {
            notifyException(project, "Unable to save. Plugin update required.")
            return
        }

        getVariablesFile().writeText(json.encodeToString(envVarCollection))

        val collection = RequestCollection(
            schemaVersion = SUPPORTED_SCHEMA_VERSION,
            data = ArrayList(model.toGroupList()),
        )
        getRequestsFile().writeText(json.encodeToString(collection))
    }

    private fun getJsonSchemaVersion(jsonElement: JsonElement): Int {
        if (jsonElement is JsonArray) return 0
        if (jsonElement is JsonObject) return jsonElement[JSON_KEY_SCHEMA_VERSION]?.jsonPrimitive?.intOrNull ?: -1
        return -1
    }

    private fun getJsonFile(path: String): File {
        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
            file.appendText(
                """
                {
                    "$JSON_KEY_SCHEMA_VERSION": $SUPPORTED_SCHEMA_VERSION,
                    "$JSON_KEY_DATA": []
                }
                """.trimIndent()
            )
        }
        return file
    }

    private fun migrate0to1(element: JsonElement): JsonElement {
        logger.debug("migrate0to1")
        return buildJsonObject {
            put(JSON_KEY_SCHEMA_VERSION, 1)
            put(JSON_KEY_DATA, element)
        }
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val JSON_KEY_SCHEMA_VERSION = "schema_version"
        const val JSON_KEY_DATA = "data"
    }
}
