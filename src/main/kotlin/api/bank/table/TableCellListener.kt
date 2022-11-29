/**
 * @author Rob Camick
 * http://www.camick.com/java/source/TableCellListener.java
 */

package api.bank.table

import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.Action
import javax.swing.JTable
import javax.swing.SwingUtilities

/*
 *  This class listens for changes made to the data in the table via the
 *  TableCellEditor. When editing is started, the value of the cell is saved
 *  When editing is stopped the new value is saved. When the old and new
 *  values are different, then the provided Action is invoked.
 *
 *  The source of the Action is a TableCellListener instance.
 */
class TableCellListener : PropertyChangeListener, Runnable {
    /**
     * Get the table of the cell that was changed
     *
     * @return the table of the cell that was changed
     */
    val table: JTable
    private var action: Action? = null

    /**
     * Get the row that was last edited
     *
     * @return the row that was edited
     */
    var row = 0
        private set

    /**
     * Get the column that was last edited
     *
     * @return the column that was edited
     */
    var column = 0
        private set

    /**
     * Get the old value of the cell
     *
     * @return the old value of the cell
     */
    var oldValue: Any? = null
        private set

    /**
     * Get the new value in the cell
     *
     * @return the new value in the cell
     */
    var newValue: Any? = null
        private set

    /**
     * Create a TableCellListener.
     *
     * @param table  the table to be monitored for data changes
     * @param action the Action to invoke when cell data is changed
     */
    constructor(table: JTable, action: Action?) {
        this.table = table
        this.action = action
        this.table.addPropertyChangeListener(this)
    }

    /**
     * Create a TableCellListener with a copy of all the data relevant to
     * the change of data for a given cell.
     *
     * @param row      the row of the changed cell
     * @param column   the column of the changed cell
     * @param oldValue the old data of the changed cell
     * @param newValue the new data of the changed cell
     */
    private constructor(table: JTable, row: Int, column: Int, oldValue: Any?, newValue: Any?) {
        this.table = table
        this.row = row
        this.column = column
        this.oldValue = oldValue
        this.newValue = newValue
    }

    //
    //  Implement the PropertyChangeListener interface
    //
    override fun propertyChange(e: PropertyChangeEvent) {
        //  A cell has started/stopped editing
        if ("tableCellEditor" == e.propertyName) {
            if (table.isEditing) processEditingStarted() else processEditingStopped()
        }
    }

    /*
     * Save information of the cell about to be edited
     */
    private fun processEditingStarted() {
        //  The invokeLater is necessary because the editing row and editing
        //  column of the table have not been set when the "tableCellEditor"
        //  PropertyChangeEvent is fired.
        //  This results in the "run" method being invoked
        SwingUtilities.invokeLater(this)
    }

    /*
     * See above.
     */
    override fun run() {
        row = table.convertRowIndexToModel(table.editingRow)
        column = table.convertColumnIndexToModel(table.editingColumn)
        oldValue = table.model.getValueAt(row, column)
        newValue = null
    }

    /*
     * Update the Cell history when necessary
     */
    private fun processEditingStopped() {
        newValue = table.model.getValueAt(row, column)

        //  The data has changed, invoke the supplied Action
        if (newValue != oldValue) {
            //  Make a copy of the data in case another cell starts editing
            //  while processing this change
            val tcl = TableCellListener(
                table, row, column, oldValue, newValue
            )
            val event = ActionEvent(
                tcl,
                ActionEvent.ACTION_PERFORMED,
                ""
            )
            action!!.actionPerformed(event)
        }
    }
}