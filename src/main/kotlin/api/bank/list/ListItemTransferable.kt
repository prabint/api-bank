package api.bank.list

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

class ListItemTransferable<T : Any>(
    listItem: T,
    private val dataFlavor: DataFlavor
) : Transferable {
    private val item: T

    init {
        this.item = listItem
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(dataFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor.equals(dataFlavor)
    }

    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return item
    }
}