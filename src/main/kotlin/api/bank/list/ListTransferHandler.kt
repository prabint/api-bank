package api.bank.list

import com.intellij.ui.components.JBList
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

/**
 * List item drag-and-reorder handler
 */
class ListTransferHandler<T>(
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
        fromIndex = (c as? JBList<*>)?.selectedIndex ?: -1
        val detail = (c as? JBList<*>)?.selectedValue ?: return null
        return ListItemTransferable(detail, dataFlavor)
    }

    override fun importData(info: TransferSupport): Boolean {
        if (!info.isDrop) {
            return false
        }

        toIndex = (info.dropLocation as JList.DropLocation).index

        draggedItem = try {
            info.transferable.getTransferData(dataFlavor) as T
        } catch (e: Exception) {
            return false
        }

        if (fromIndex == toIndex || fromIndex == -1 || toIndex == -1) {
            return false
        }

        return true
    }

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        if (fromIndex < toIndex) {
            toIndex -= 1
        }

        if (fromIndex == toIndex || fromIndex == -1 || toIndex == -1) {
            return
        }

        val defaultListModel = (source as JList<*>).model as DefaultListModel<T>
        defaultListModel.removeElementAt(fromIndex)
        defaultListModel.add(toIndex, draggedItem)
        onItemDropped(fromIndex, toIndex)
    }
}