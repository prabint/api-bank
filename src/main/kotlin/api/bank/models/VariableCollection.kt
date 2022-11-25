package api.bank.models

/**
 * Represents a single set of variables such as Staging or Production
 */
data class VariableCollection(
    var id: String? = null,
    var name: String? = null,
    var variableItems: ArrayList<Array<String>> = ArrayList(),
    var isActive: Boolean? = null,
)
