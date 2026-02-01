package api.bank.models

import api.bank.utils.FileManager.Companion.JSON_KEY_DATA
import api.bank.utils.FileManager.Companion.JSON_KEY_SCHEMA_TYPE
import api.bank.utils.FileManager.Companion.JSON_KEY_SCHEMA_VERSION
import api.bank.utils.FileManager.Companion.SUPPORTED_SCHEMA_VERSION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

enum class SchemaType(val type: String) {
    REQUESTS("requests"),
    VARIABLES("variables"),
    UNKNOWN("unknown"),
}

@Serializable
data class RequestCollection(
    @SerialName(JSON_KEY_SCHEMA_VERSION)
    val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,

    @SerialName(JSON_KEY_SCHEMA_TYPE)
    val type: String = SchemaType.REQUESTS.type,

    @SerialName(JSON_KEY_DATA)
    val data: ArrayList<RequestGroup> = ArrayList(),
)

/**
 * Represents details about an API request
 */
@Serializable
data class RequestDetail(
    var id: String,
    var name: String = "(New Request)",
    var url: String = "",
    var method: String = "GET",
    var header: ArrayList<Array<String>> = ArrayList(),
    var body: String? = null,
) {
    companion object {
        val DEFAULT
            get() = RequestDetail(
                id = UUID.randomUUID().toString(),
                name = "(New Request)",
                url = "https://",
                method = "GET",
                header = ArrayList(),
                body = null,
            )
    }
}

@Serializable
data class RequestGroup(
    val groupName: String,
    val requests: List<RequestDetail> = emptyList(),
)
