package api.bank.utils

import api.bank.list.TableTransferHandler
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.datatransfer.DataFlavor
import javax.swing.DefaultCellEditor
import javax.swing.DropMode
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellEditor

private val TABLE_DATA_FLAVOR = DataFlavor(
    Array::class.java,
    "VariableRow"
)

fun TableView<Array<String>>.applySharedTableDnd() {
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    dropMode = DropMode.INSERT
    dragEnabled = true
    transferHandler = TableTransferHandler<Array<String>>(TABLE_DATA_FLAVOR) { from, to ->
        setRowSelectionInterval(to, to)
    }
}

fun TableView<Array<String>>.applySharedTableDecorations() {
    tableHeader.apply {
        font = font.deriveFont(java.awt.Font.BOLD)
        background = com.intellij.util.ui.UIUtil.getWindowColor()
    }

    autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
    columnModel.getColumn(0).preferredWidth = 300
    columnModel.getColumn(1).preferredWidth = 700
}

fun getTableModel(): ListTableModel<Array<String>> {
    return ListTableModel<Array<String>>(
        object : ColumnInfo<Array<String>, String>("Key") {
            override fun isCellEditable(item: Array<String>): Boolean = true

            override fun valueOf(item: Array<String>): String = item[0]

            override fun setValue(item: Array<String>, value: String) {
                item[0] = value
            }

            override fun getEditor(item: Array<String>?): TableCellEditor? {
                return DefaultCellEditor(JBTextField()).apply {
                    clickCountToStart = 2
                }
            }
        },
        object : ColumnInfo<Array<String>, String>("Value") {
            override fun isCellEditable(item: Array<String>): Boolean = true

            override fun valueOf(item: Array<String>): String = item[1]

            override fun setValue(item: Array<String>, value: String) {
                item[1] = value
            }

            override fun getEditor(item: Array<String>?): TableCellEditor? {
                return DefaultCellEditor(JBTextField()).apply {
                    clickCountToStart = 2
                }
            }
        }
    )
}
