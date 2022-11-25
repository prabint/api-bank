package api.bank.list

import javax.swing.table.TableModel

fun TableModel.toListOfKeyValue(): ArrayList<Array<String>> {
    val result = ArrayList<Array<String>>()

    for (rowIndex in 0 until rowCount) {
        val key = getValueAt(rowIndex, 0) as String
        val value = getValueAt(rowIndex, 1) as String

        if (key.isBlank() && value.isBlank()) continue

        result.add(arrayOf(key, value))
    }

    return result
}
