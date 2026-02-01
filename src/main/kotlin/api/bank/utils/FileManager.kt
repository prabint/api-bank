package api.bank.utils

import api.bank.models.*
import api.bank.notification.notifyException
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import api.bank.utils.FileManager.Companion.JSON_KEY_SCHEMA_TYPE
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.annotations.VisibleForTesting
import org.koin.core.annotation.Singleton
import java.io.File
import javax.swing.tree.TreeModel

@Singleton
class FileManager(
    private val project: Project,
    private val json: Json,
    private val logger: Logger
) {
    fun migrateAll() {
        migrate(getVariablesFile())
        migrate(getRequestsFile())
    }

    @VisibleForTesting
    fun migrate(file: File) {
        val importSource = ImportSource(
            path = file.path,
            text = file.readText(),
            write = { file.writeText(it) }
        )

        val result = importBank(importSource)

        when (result) {
            is Result.Error -> {
                logger.error(result.message, result.exception)
                notifyException(project, result.message ?: "Parsing error: ${result.exception?.stackTraceToString()}")
            }

            is Result.Success<String> -> logger.debug("Migration OK. ${result.message}")
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
        if (getJsonSchemaVersion(getVariablesFile()) == SUPPORTED_SCHEMA_VERSION) {
            getVariablesFile().writeText(json.encodeToString(envVarCollection))
        } else {
            notifyException(project, "Unable to save. Plugin update required.")
        }

        if (getJsonSchemaVersion(getRequestsFile()) == SUPPORTED_SCHEMA_VERSION) {
            val collection = RequestCollection(data = ArrayList(model.toGroupList()))
            getRequestsFile().writeText(json.encodeToString(collection))
        } else {
            notifyException(project, "Unable to save. Plugin update required.")
        }
    }

    fun importBank(importSource: ImportSource): Result<String> {
        logger.debug("importBank(path: ${importSource.path})")

        val result = getJsonElement(
            text = importSource.text,
            path = importSource.path
        )

        var jsonElement = when (result) {
            is Result.Success<JsonElement> -> result.data as JsonElement
            is Result.Error -> return Result.Error(result.exception, result.message)
        }
        var schemaVersion = getJsonSchemaVersion(jsonElement)

        logger.debug("Detected schema version = $schemaVersion")

        when {
            schemaVersion < 0 -> {
                return Result.Error(message = "Unable to parse $JSON_KEY_SCHEMA_VERSION; was -1")
            }

            schemaVersion > SUPPORTED_SCHEMA_VERSION -> {
                return Result.Error(message = "Plugin update required. Schema was $schemaVersion but plugin supports $SUPPORTED_SCHEMA_VERSION")
            }

            schemaVersion == SUPPORTED_SCHEMA_VERSION -> {
                return Result.Success(message = "Migration not required.")
            }
        }

        val schemaType = getJsonSchemaType(jsonElement)

        logger.debug("Detected schema type = $schemaType")

        if (schemaType == SchemaType.UNKNOWN) {
            return Result.Success(message = "Unknown schema type.")
        }

        while (schemaVersion < SUPPORTED_SCHEMA_VERSION) {
            jsonElement = when (schemaVersion) {
                0 -> {
                    val result = migrate0to1(jsonElement, schemaType)
                    when (result) {
                        is Result.Error -> return Result.Error(message = "v0 to v1 migration failed")
                        is Result.Success<JsonElement> -> result.data as JsonElement
                    }
                }

                else -> {
                    return Result.Error(message = "Unsupported migration path: $schemaVersion")
                }
            }

            schemaVersion++
        }

        importSource.write(json.encodeToString(jsonElement))

        return Result.Success(message = "Migration success for schema type $schemaType")
    }

    @VisibleForTesting
    fun getJsonSchemaVersion(file: File): Int {
        val result = getJsonElement(
            text = file.readText(),
            path = file.path,
        )

        return when (result) {
            is Result.Success<JsonElement> -> {
                getJsonSchemaVersion(result.data as JsonElement)
            }

            is Result.Error -> -1
        }
    }

    @VisibleForTesting
    fun getJsonSchemaVersion(jsonElement: JsonElement): Int {
        if (jsonElement is JsonArray) return 0
        if (jsonElement is JsonObject) return jsonElement[JSON_KEY_SCHEMA_VERSION]?.jsonPrimitive?.intOrNull ?: -1
        return -1
    }

    @VisibleForTesting
    fun getJsonFile(path: String): File {
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

    @VisibleForTesting
    fun migrate0to1(
        element: JsonElement,
        schemaType: SchemaType,
    ): Result<JsonElement> {
        logger.debug("Migrate from v0 to v1")
        return try {
            Result.Success(
                data = buildJsonObject {
                    put(JSON_KEY_SCHEMA_VERSION, 1)
                    put(JSON_KEY_DATA, element)
                    put(JSON_KEY_SCHEMA_TYPE, schemaType.type)
                }
            )
        } catch (e: Exception) {
            Result.Error(e, "Failed to migration v0 -> v1")
        }
    }


    fun getSchemaType(text: String, filePath: String): SchemaType {
        val result = getJsonElement(text, filePath)
        return when (result) {
            is Result.Success<JsonElement> -> {
                getJsonSchemaType(result.data as JsonElement)
            }

            is Result.Error -> SchemaType.UNKNOWN
        }
    }

    /**
     * For v0, there is no single property to distinguish between [SchemaType].
     * From v1 onwards, [JSON_KEY_SCHEMA_TYPE] can be used.
     * @return true if the json is in fact of matching [SchemaType].
     */
    fun getJsonSchemaType(jsonElement: JsonElement): SchemaType {
        if (getJsonSchemaVersion(jsonElement) == 0) {
            if (jsonElement is JsonArray && jsonElement.all {
                    it is JsonObject &&
                            it.containsKey("groupName") &&
                            it.containsKey("requests")
                }) return SchemaType.REQUESTS

            if (jsonElement is JsonArray && jsonElement.all {
                    it is JsonObject &&
                            it.containsKey("id") &&
                            it.containsKey("name") &&
                            it.containsKey("variableItems") &&
                            it.containsKey("isActive")
                }) return SchemaType.VARIABLES
        } else {
            val type = (jsonElement as? JsonObject)?.let {
                it[JSON_KEY_SCHEMA_TYPE]?.jsonPrimitive?.contentOrNull
            }
            if (type == SchemaType.REQUESTS.type) return SchemaType.REQUESTS
            if (type == SchemaType.VARIABLES.type) return SchemaType.VARIABLES
        }
        return SchemaType.UNKNOWN
    }

    private fun getJsonElement(text: String, path: String): Result<JsonElement> {
        return try {
            Result.Success(data = json.parseToJsonElement(text))
        } catch (e: SerializationException) {
            return Result.Error(e, "Invalid json file: $path")
        } catch (e: Exception) {
            return Result.Error(e, "Unknown error, file: $path")
        }
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val JSON_KEY_SCHEMA_VERSION = "schema_version"
        const val JSON_KEY_SCHEMA_TYPE = "type"
        const val JSON_KEY_DATA = "data"
    }
}
