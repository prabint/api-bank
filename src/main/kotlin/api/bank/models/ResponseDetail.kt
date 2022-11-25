package api.bank.models

/**
 * Represents portion of useful data from a response
 */
data class ResponseDetail(
    val body: String,
    val code: Int,
    val message: String,
)
