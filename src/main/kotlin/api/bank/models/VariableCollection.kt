package api.bank.models

import api.bank.utils.FileManager.Companion.JSON_KEY_DATA
import api.bank.utils.FileManager.Companion.JSON_KEY_SCHEMA_TYPE
import api.bank.utils.FileManager.Companion.JSON_KEY_SCHEMA_VERSION
import api.bank.utils.FileManager.Companion.SUPPORTED_SCHEMA_VERSION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single set of variables such as Staging or Production
 */
@Serializable
data class VariableCollection(
    @SerialName(JSON_KEY_SCHEMA_VERSION)
    val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,

    @SerialName(JSON_KEY_SCHEMA_TYPE)
    val type: String = SchemaType.VARIABLES.type,

    @SerialName(JSON_KEY_DATA)
    val data: ArrayList<VariableDetail> = ArrayList(),
)

@Serializable
data class VariableDetail(
    var id: String? = null,
    var name: String? = null,
    var variableItems: ArrayList<Array<String>> = ArrayList(),
    var isActive: Boolean? = null,
)
