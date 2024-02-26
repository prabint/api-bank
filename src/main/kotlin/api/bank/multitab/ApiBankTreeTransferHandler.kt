package api.bank.multitab

import api.bank.models.RequestDetail
import api.bank.utils.isGroupNode
import com.intellij.ui.treeStructure.Tree
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.math.min

private val dataFlavor =
    DataFlavor("${DataFlavor.javaJVMLocalObjectMimeType};class=\"${Array<DefaultMutableTreeNode>::class.java.name}\"")

class ApiBankTreeTransferHandler : TransferHandler() {
    private var selectionPath: TreePath? = null
    private var selectionIsFolder: Boolean = false
    private var selectionNode: DefaultMutableTreeNode? = null
    private var selectionIndex = -1
    private var selectionGroupExpanded = false

    private var dropNode: DefaultMutableTreeNode? = null
    private var dropIndex: Int = 0
    private var dropTreePathByLocation: TreePath? = null

    override fun getSourceActions(c: JComponent) = MOVE

    /**
     * Called once when an item drag is started. Useful for getting info about item being dragged.
     */
    override fun createTransferable(c: JComponent): Transferable {
        val tree = c as Tree
        selectionPath = tree.selectionPath!!
        selectionNode = selectionPath!!.lastPathComponent as DefaultMutableTreeNode
        selectionIndex = tree.model.getIndexOfChild(selectionNode!!.parent, selectionNode)
        selectionIsFolder = selectionNode?.userObject is String
        selectionGroupExpanded = selectionIsFolder && tree.isExpanded(TreePath(selectionPath!!.path))

        return object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(dataFlavor)
            override fun isDataFlavorSupported(p0: DataFlavor?) = dataFlavor.equals(p0)
            override fun getTransferData(p0: DataFlavor?) = selectionNode!!
        }
    }

    /**
     * This method is executed continuously during drag. Useful for checking if drop is allowed.
     * Return true to show drop indicator.
     */
    override fun canImport(support: TransferSupport): Boolean {
        val dropLocation = support.dropLocation as JTree.DropLocation
        dropNode = dropLocation.path.lastPathComponent as DefaultMutableTreeNode
        dropIndex = dropLocation.childIndex
        dropTreePathByLocation =
            (support.component as Tree).getPathForLocation(dropLocation.dropPoint.x, dropLocation.dropPoint.y)

        println(
            "\n------------\n" +
                    "selectionIndex = $selectionIndex\n" +
                    "dropLocation.childIndex = $dropIndex\n" +
                    "dropLocation.path.lastPathComponent = $dropNode\n" +
                    "dropTreePathByLocation = ${dropTreePathByLocation}\n" +
                    "dropLocation.path = ${dropLocation.path}\n"
        )

        if (isItemDraggedBelowLastItem(support.component as Tree, dropLocation)) {
            support.setShowDropLocation(true)
            return true
        }

        if (isDroppingOutsideRange()) return false

        /**
         *  E.g.:
         *  --------
         *  "folder 1":
         *      "item 1"
         *      "item 2"
         *  --------
         *  "item 1" is at index 0. If it is dragged slightly up limited between "folder 1" and "item 1" (index 0)
         *  or slightly down limited between "item 1" and "item 2" (index 1), no need to perform drag-and-drop.
         */
        if (dropLocation.path == selectionPath?.parentPath &&
            (dropIndex == selectionIndex || dropIndex == selectionIndex + 1)
        ) {
            return false
        }

        // Don't allow dropping a folder inside another folder
        if (dropNode?.userObject is String && selectionIsFolder) return false

        // Allow inserting an item into a folder but not an item into another item
        if (dropIndex == -1 && dropNode?.userObject is RequestDetail) return false

        if (dropNode?.isRoot == true && dropNode?.userObject is RequestDetail) return false

        support.setShowDropLocation(true)

        return true
    }

    /**
     * Called after item drop
     */
    override fun importData(support: TransferSupport) = true

    /**
     * Called after importData()
     * Its main responsibility is to remove the data that was exported from the source component
     */
    override fun exportDone(source: JComponent, data: Transferable?, action: Int) {
        if (action == MOVE) {
            var index = min(dropNode!!.childCount - 1, dropIndex)
            if (index < 0) index = 0

            val defaultListModel = (source as Tree).model as DefaultTreeModel

            // This is the drop node that considers dropping item at root-level of the tree
            var finalDropNode = dropNode

            val isDroppingRequestOntoRoot = finalDropNode?.isRoot == true && selectionNode?.isGroupNode() != true

            if (isDroppingRequestOntoRoot) {
                if (dropIndex == 0) { // If dropping above the first group
                    finalDropNode = finalDropNode?.getChildAt(0) as DefaultMutableTreeNode

                    defaultListModel.removeNodeFromParent(selectionNode)
                    defaultListModel.insertNodeInto(selectionNode, finalDropNode, 0) // Add it to first
                } else {
                    finalDropNode = finalDropNode?.getChildAt(dropIndex - 1) as DefaultMutableTreeNode

                    defaultListModel.removeNodeFromParent(selectionNode)
                    defaultListModel.insertNodeInto(selectionNode, finalDropNode, finalDropNode.childCount)
                }
            } else {
                if (isDroppingBelow()) {
                    index -= 1
                }
                defaultListModel.removeNodeFromParent(selectionNode)
                defaultListModel.insertNodeInto(selectionNode, finalDropNode, index)
            }

            // When a group is moved, swing auto collapses it. So, we expand it manually.
            if (selectionGroupExpanded) {
                source.expandPath(selectionPath)
            }

            // Select the new node. This will auto expand the destination group if it was collapsed
            val newSelectionNode = finalDropNode!!.path + arrayOf(selectionNode)
            source.selectionPath = TreePath(newSelectionNode)
        }
    }

    /**
     *  E.g.:
     *  --------
     *  "folder 1"
     *      "item 1a"
     *      "item 1b"
     *      "item 1c"
     *      "item 1d"
     *  "folder 2"
     *  "folder 3"
     *  --------
     *
     *  When "item 1a" is dropped between "item 1b" and "item 1c", we need to do -1 to the drop index.
     *  Otherwise, "item 1a" will drop below "item 1c". This is not an issue if an item is moved upwards the tree.
     */
    private fun isDroppingBelow(): Boolean {
        if (selectionNode?.isGroupNode() != true &&
            selectionPath?.parentPath != dropNode?.path?.let { TreePath(it) } // Dropping request within same group
        ) return false

        if (dropNode?.childCount == dropIndex) return false

        // Dropping node to a lower position in the tree
        return dropIndex > selectionIndex
    }

    /**
     *  E.g.:
     *  --------
     *  "folder 1"
     *      "item 1"
     *      "item 2"
     *  "folder 2"
     *  "folder 3"
     *  --------
     *
     * @return true if item is dragged below the last item in the tree, i.e. below "folder 3" in this case. If "folder 3"
     * is expanded, the last child will be considered.
     *
     * We will use this to allow dropping item at the end of the tree.
     */
    private fun isItemDraggedBelowLastItem(tree: Tree, dropLocation: JTree.DropLocation): Boolean {
        val rectangle = tree.getRowBounds(tree.rowCount - 1)
        val finalY = rectangle.y + rectangle.height
        return dropLocation.dropPoint.y >= finalY
    }

    /**
     *  E.g.:
     *  --------
     *  "folder 1"
     *      "item 1"
     *      "item 2"
     *  "folder 2"
     *  "folder 3"
     *  --------
     *
     * When "folder 2" is dragged between "folder 1"" and "item 1", it will drop below "folder 3"". This code fixes that.
     */
    private fun isDroppingOutsideRange() = dropNode?.isRoot == true && // item dropped at root-level
            dropIndex == dropNode?.childCount && // Item is being dropped below last item of root-level
            // The location is null or item is being dragged into a group
            (dropTreePathByLocation == null || dropTreePathByLocation?.pathCount == 2)
}
