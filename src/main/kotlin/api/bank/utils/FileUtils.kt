package api.bank.utils

import api.bank.models.RequestCollection
import api.bank.models.RequestDetail
import api.bank.models.VariableCollection
import api.bank.settings.ApiBankSettingsPersistentStateComponent
import com.intellij.openapi.project.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

const val SUPPORTED_SCHEMA_VERSION = 1
const val JSON_KEY_SCHEMA_VERSION = "schema_version"
const val JSON_KEY_DATA = "data"

fun migrate(project: Project, json: Json) {
    migrateIfNeeded(getVariablesFile(project), json)
    migrateIfNeeded(getRequestsFile(project), json)
}

fun getVariablesFile(project: Project): File {
    val path = ApiBankSettingsPersistentStateComponent.getInstance(project).state.envFilePath
    return getJsonFile(path)
}

fun getRequestsFile(project: Project): File {
    val path = ApiBankSettingsPersistentStateComponent.getInstance(project).state.requestFilePath
    return getJsonFile(path)
}

fun getVariableCollectionFromJson(json: Json, project: Project): VariableCollection {
    return json.decodeFromString(getVariablesFile(project).readText())
}

fun saveVariableCollectionAsJsonFile(json: Json, project: Project, value: VariableCollection) {
    getVariablesFile(project).writeText(json.encodeToString(value))
}

fun getRequestDetailsFromJson(json: Json, project: Project): List<RequestDetail> {
    return json.decodeFromString<RequestCollection>(getRequestsFile(project).readText()).data.flatMap { it.requests }
}

private fun migrateIfNeeded(file: File, json: Json) {
    val jsonData = file.readText()
    val element = json.parseToJsonElement(jsonData)
    if (element is JsonArray) {
        // Migrate from 0 to 1
        val updated = buildJsonObject {
            put(JSON_KEY_SCHEMA_VERSION, 1)
            put(JSON_KEY_DATA, element)
        }
        file.writeText(json.encodeToString(updated))
    }
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
