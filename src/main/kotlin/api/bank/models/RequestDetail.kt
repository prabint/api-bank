package api.bank.models

/**
 * Represents details about an API request
 */
data class RequestDetail(
    var id: String? = null,
    var name: String? = null,
    var url: String? = null,
    var method: String? = null,
    var header: ArrayList<Array<String>> = ArrayList(),
    var body: String? = null,
)
