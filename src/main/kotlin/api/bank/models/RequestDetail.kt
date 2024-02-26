package api.bank.models

import java.util.*

/**
 * Represents details about an API request
 */
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

data class RequestGroup(
    val groupName: String,
    val requests: List<RequestDetail> = emptyList(),
)
