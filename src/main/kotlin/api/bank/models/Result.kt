package api.bank.models

sealed interface Result<out T> {
    data class Success<out T>(
        val data: T? = null,
        val message: String? = null,
    ) : Result<T>

    data class Error(
        val exception: Exception? = null,
        val message: String? = null,
    ) : Result<Nothing>
}
