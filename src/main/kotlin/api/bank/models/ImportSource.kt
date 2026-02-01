package api.bank.models

data class ImportSource(
    val path: String,
    val text: String,
    val write: (String) -> Unit
)
