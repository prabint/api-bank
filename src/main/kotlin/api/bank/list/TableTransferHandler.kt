package api.bank.list

import com.intellij.util.ui.ListTableModel
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.TransferHandler

/**
 * Table row item drag-and-reorder handler.
 * Very similar to [ListTransferHandler].
 */
class TableTransferHandler<T>(
    private val dataFlavor: DataFlavor,
    val onItemDropped: (from: Int, to: Int) -> Unit,
) : TransferHandler() {

    private var fromIndex: Int = -1
    private var toIndex: Int = -1
    private var draggedItem: T? = null

    override fun canImport(info: TransferSupport): Boolean {
        return info.isDataFlavorSupported(dataFlavor)
    }

    override fun getSourceActions(c: JComponent): Int {
        return MOVE
    }

    override fun createTransferable(c: JComponent?): Transferable? {
        fromIndex = (c as? JTable)?.selectedRow ?: -1
        val detail = ((c as? JTable)?.model as? ListTableModel<*>)?.getItem(fromIndex) ?: return null
        return ListItemTransferable(detail, dataFlavor)
    }

    override fun importData(info: TransferSupport): Boolean {
        val table = info.component as JTable
        val dropLocation = info.dropLocation as JTable.DropLocation

        if (!info.isDrop) {
            return false
        }

        // If an item is dropped way below past the last row, the [toIndex] equals the size of the list which is
        // index-out-of-bound. coerceAtMost fixes that.
        toIndex = dropLocation.row.coerceAtMost(table.rowCount)

        draggedItem = try {
            info.transferable.getTransferData(dataFlavor) as T
        } catch (e: Exception) {
            return false
        }

        return !(fromIndex == toIndex || fromIndex == -1 || toIndex == -1)
    }

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        if (fromIndex < toIndex) {
            toIndex -= 1
        }

        if (fromIndex == toIndex || fromIndex == -1 || toIndex == -1) {
            return
        }

        val defaultListModel = (source as JTable).model as ListTableModel<T>
        defaultListModel.removeRow(fromIndex)
        defaultListModel.insertRow(toIndex, draggedItem)
        onItemDropped(fromIndex, toIndex)
    }
}
