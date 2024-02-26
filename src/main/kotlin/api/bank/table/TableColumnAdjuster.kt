/**
 * @author Rob Camick
 * http://www.camick.com/java/source/TableColumnAdjuster.java
 */

package api.bank.table

import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableColumn
import javax.swing.table.TableModel

/*
 *	Class to manage the widths of columns in a table.
 *
 *  Various properties control how the width of the column is calculated.
 *  Another property controls whether column width calculation should be dynamic.
 *  Finally, various Actions will be added to the table to allow the user
 *  to customize the functionality.
 *
 *  This class was designed to be used with tables that use an auto resize mode
 *  of AUTO_RESIZE_OFF. With all other modes you are constrained as the width
 *  of the columns must fit inside the table. So if you increase one column, one
 *  or more of the other columns must decrease. Because of this the resize mode
 *  of RESIZE_ALL_COLUMNS will work the best.
 */
class TableColumnAdjuster(private val table: JTable, private val spacing: Int = 6) :
    PropertyChangeListener,
    TableModelListener {
    private var isColumnHeaderIncluded = false
    private var isColumnDataIncluded = false
    private var isOnlyAdjustLarger = false
    private var isDynamicAdjustment = false
    private val columnSizes: MutableMap<TableColumn, Int> = HashMap()

    /*
     *  Specify the table and spacing
     */
    /*
     *  Specify the table and use default spacing
     */
    init {
        setColumnHeaderIncluded(true)
        setColumnDataIncluded(true)
        setOnlyAdjustLarger(false)
        setDynamicAdjustment(false)
        installActions()
    }

    /*
     *  Adjust the widths of all the columns in the table
     */
    fun adjustColumns() {
        val tcm = table.columnModel
        for (i in 0 until tcm.columnCount) {
            adjustColumn(i)
        }
    }

    /*
     *  Adjust the width of the specified column in the table
     */
    fun adjustColumn(column: Int) {
        val tableColumn = table.columnModel.getColumn(column)
        if (!tableColumn.resizable) return
        val columnHeaderWidth = getColumnHeaderWidth(column)
        val columnDataWidth = getColumnDataWidth(column)
        val preferredWidth = Math.max(columnHeaderWidth, columnDataWidth)
        updateTableColumn(column, preferredWidth)
    }

    /*
     *  Calculated the width based on the column name
     */
    private fun getColumnHeaderWidth(column: Int): Int {
        if (!isColumnHeaderIncluded) return 0
        val tableColumn = table.columnModel.getColumn(column)
        val value = tableColumn.headerValue
        var renderer = tableColumn.headerRenderer
        if (renderer == null) {
            renderer = table.tableHeader.defaultRenderer
        }
        val c = renderer!!.getTableCellRendererComponent(table, value, false, false, -1, column)
        return c.preferredSize.width
    }

    /*
     *  Calculate the width based on the widest cell renderer for the
     *  given column.
     */
    private fun getColumnDataWidth(column: Int): Int {
        if (!isColumnDataIncluded) return 0
        var preferredWidth = 0
        val maxWidth = table.columnModel.getColumn(column).maxWidth
        for (row in 0 until table.rowCount) {
            preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column))

            //  We've exceeded the maximum width, no need to check other rows
            if (preferredWidth >= maxWidth) break
        }
        return preferredWidth
    }

    /*
     *  Get the preferred width for the specified cell
     */
    private fun getCellDataWidth(row: Int, column: Int): Int {
        //  Invoke the renderer for the cell to calculate the preferred width
        val cellRenderer = table.getCellRenderer(row, column)
        val c = table.prepareRenderer(cellRenderer, row, column)
        return c.preferredSize.width + table.intercellSpacing.width
    }

    /*
     *  Update the TableColumn with the newly calculated width
     */
    private fun updateTableColumn(column: Int, width: Int) {
        var innerWidth = width
        val tableColumn = table.columnModel.getColumn(column)
        if (!tableColumn.resizable) return
        innerWidth += spacing

        //  Don't shrink the column width
        if (isOnlyAdjustLarger) {
            innerWidth = innerWidth.coerceAtLeast(tableColumn.preferredWidth)
        }
        columnSizes[tableColumn] = tableColumn.width
        table.tableHeader.resizingColumn = tableColumn
        tableColumn.width = innerWidth
    }

    /*
     *  Restore the widths of the columns in the table to its previous width
     */
    fun restoreColumns() {
        val tcm = table.columnModel
        for (i in 0 until tcm.columnCount) {
            restoreColumn(i)
        }
    }

    /*
     *  Restore the width of the specified column to its previous width
     */
    private fun restoreColumn(column: Int) {
        val tableColumn = table.columnModel.getColumn(column)
        val width = columnSizes[tableColumn]
        if (width != null) {
            table.tableHeader.resizingColumn = tableColumn
            tableColumn.width = width
        }
    }

    /*
     *	Indicates whether to include the header in the width calculation
     */
    fun setColumnHeaderIncluded(isColumnHeaderIncluded: Boolean) {
        this.isColumnHeaderIncluded = isColumnHeaderIncluded
    }

    /*
     *	Indicates whether to include the model data in the width calculation
     */
    fun setColumnDataIncluded(isColumnDataIncluded: Boolean) {
        this.isColumnDataIncluded = isColumnDataIncluded
    }

    /*
     *	Indicates whether columns can only be increased in size
     */
    fun setOnlyAdjustLarger(isOnlyAdjustLarger: Boolean) {
        this.isOnlyAdjustLarger = isOnlyAdjustLarger
    }

    /*
     *  Indicate whether changes to the model should cause the width to be
     *  dynamically recalculated.
     */
    fun setDynamicAdjustment(isDynamicAdjustment: Boolean) {
        //  May need to add or remove the TableModelListener when changed
        if (this.isDynamicAdjustment != isDynamicAdjustment) {
            if (isDynamicAdjustment) {
                table.addPropertyChangeListener(this)
                table.model.addTableModelListener(this)
            } else {
                table.removePropertyChangeListener(this)
                table.model.removeTableModelListener(this)
            }
        }
        this.isDynamicAdjustment = isDynamicAdjustment
    }

    //
    //  Implement the PropertyChangeListener
    //
    override fun propertyChange(e: PropertyChangeEvent) {
        //  When the TableModel changes we need to update the listeners
        //  and column widths
        if ("model" == e.propertyName) {
            var model = e.oldValue as TableModel
            model.removeTableModelListener(this)
            model = e.newValue as TableModel
            model.addTableModelListener(this)
            adjustColumns()
        }
    }

    //
    //  Implement the TableModelListener
    //
    override fun tableChanged(e: TableModelEvent) {
        if (!isColumnDataIncluded) return

        //  Needed when table is sorted.
        SwingUtilities.invokeLater {

            //  A cell has been updated
            val column = table.convertColumnIndexToView(e.column)
            if (e.type == TableModelEvent.UPDATE && column != -1) {
                //  Only need to worry about an increase in width for this cell
                if (isOnlyAdjustLarger) {
                    val row = e.firstRow
                    val tableColumn = table.columnModel.getColumn(column)
                    if (tableColumn.resizable) {
                        val width = getCellDataWidth(row, column)
                        updateTableColumn(column, width)
                    }
                } else {
                    adjustColumn(column)
                }
            } else {
                adjustColumns()
            }
        }
    }

    /*
     *  Install Actions to give user control of certain functionality.
     */
    private fun installActions() {
        installColumnAction(true, true, "adjustColumn", "control ADD")
        installColumnAction(false, true, "adjustColumns", "control shift ADD")
        installColumnAction(true, false, "restoreColumn", "control SUBTRACT")
        installColumnAction(false, false, "restoreColumns", "control shift SUBTRACT")
        installToggleAction(true, false, "toggleDynamic", "control MULTIPLY")
        installToggleAction(false, true, "toggleLarger", "control DIVIDE")
    }

    /*
     *  Update the input and action maps with a new ColumnAction
     */
    private fun installColumnAction(
        isSelectedColumn: Boolean, isAdjust: Boolean, key: String, keyStroke: String
    ) {
        val action: Action = ColumnAction(isSelectedColumn, isAdjust)
        val ks = KeyStroke.getKeyStroke(keyStroke)
        table.inputMap.put(ks, key)
        table.actionMap.put(key, action)
    }

    /*
     *  Update the input and action maps with new ToggleAction
     */
    private fun installToggleAction(
        isToggleDynamic: Boolean, isToggleLarger: Boolean, key: String, keyStroke: String
    ) {
        val action: Action = ToggleAction(isToggleDynamic, isToggleLarger)
        val ks = KeyStroke.getKeyStroke(keyStroke)
        table.inputMap.put(ks, key)
        table.actionMap.put(key, action)
    }

    /*
     *  Action to adjust or restore the width of a single column or all columns
     */
    internal inner class ColumnAction(private val isSelectedColumn: Boolean, private val isAdjust: Boolean) :
        AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            //  Handle selected column(s) width change actions
            if (isSelectedColumn) {
                val columns = table.selectedColumns
                for (column in columns) {
                    if (isAdjust) adjustColumn(column) else restoreColumn(column)
                }
            } else {
                if (isAdjust) adjustColumns() else restoreColumns()
            }
        }
    }

    /*
     *  Toggle properties of the TableColumnAdjuster so the user can
     *  customize the functionality to their preferences
     */
    internal inner class ToggleAction(private val isToggleDynamic: Boolean, private val isToggleLarger: Boolean) :
        AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (isToggleDynamic) {
                setDynamicAdjustment(!isDynamicAdjustment)
                return
            }
            if (isToggleLarger) {
                setOnlyAdjustLarger(!isOnlyAdjustLarger)
            }
        }
    }
}