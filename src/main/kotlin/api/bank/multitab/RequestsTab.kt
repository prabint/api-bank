package api.bank.multitab

import api.bank.models.Constants
import api.bank.models.RequestDetail
import api.bank.repository.CoreRepository
import api.bank.utils.*
import api.bank.utils.dispatcher.DispatcherProvider
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.*

/**
 * UI that has list API requests on the left and editor on the right
 */
class RequestsTab(
    private val gson: Gson,
    private val treeModel: DefaultTreeModel,
    private val coreRepository: CoreRepository,
    private val dispatchProvider: DispatcherProvider,
    private val getVariables: () -> ArrayList<Array<String>>,
) {
    private lateinit var listAndEditorSplitter: JBSplitter
    internal lateinit var tree: Tree
        private set

    private val requestDetails: MutableList<RequestDetail> = treeModel.toRequests()

    /**
     * Stores UI in memory to emulate tab-like feature where the UI components are maintained even after navigating to
     * different tabs
     */
    private val editUiTracker = HashMap<String, EditorTab>()

    private val treeItemClickMouseAdapter = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            // https://coderanch.com/t/461207/java/JTree-row-selection-mouseclick
            // Allow right-clicking anywhere in the entire JTree row, not just the text
            if (SwingUtilities.isRightMouseButton(e)) {
                val closestRow = tree.getClosestRowForLocation(e.x, e.y)
                val closestRowBounds = tree.getRowBounds(closestRow)
                if (e.y >= closestRowBounds.getY() && e.y < closestRowBounds.getY() + closestRowBounds.getHeight()) {
                    if (e.x > closestRowBounds.getX() && closestRow < tree.rowCount) {
                        showTreeItemClickedPopUpMenu(tree, e)
                    }
                }
            }
        }

        override fun mouseClicked(e: MouseEvent?) {
            if (e?.clickCount == 2) {
                executeRequest()
            }
        }
    }

    private val treeModelListener = object : TreeModelListener {
        override fun treeNodesRemoved(p0: TreeModelEvent?) {
            if ((tree.model.root as DefaultMutableTreeNode).childCount == 0) {
                listAndEditorSplitter.secondComponent = createEmptyRightPanel()
            }
        }

        override fun treeNodesChanged(p0: TreeModelEvent?) = Unit
        override fun treeNodesInserted(p0: TreeModelEvent?) = Unit
        override fun treeStructureChanged(p0: TreeModelEvent?) = Unit
    }

    private val treeItemSelectionListener = TreeSelectionListener {
        val selectedValue = (it.path.lastPathComponent as? DefaultMutableTreeNode)?.getRequestDetail()
            ?: return@TreeSelectionListener

        if (editUiTracker.containsKey(selectedValue.id)) {
            listAndEditorSplitter.secondComponent = editUiTracker[selectedValue.id]?.panel
            return@TreeSelectionListener
        }

        val editorTab = EditorTab(
            gson = gson,
            getVariables = getVariables,
            coreRepository = coreRepository,
            dispatcherProvider = dispatchProvider,
            requestDetailInMemory = selectedValue,
            onDisplayNameUpdated = { detail -> onEditorUpdated(detail) },
            onMethodUpdated = { detail -> onEditorUpdated(detail) }
        )

        editUiTracker[selectedValue.id] = editorTab
        listAndEditorSplitter.secondComponent = editorTab.panel
    }

    private val addNewItemToolbarMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(mouseEvent: MouseEvent) {
            val popupMenu = JBPopupMenu()

            JBMenuItem("New Request", AllIcons.FileTypes.Json).apply {
                addActionListener { addNewRequestAction(tree.selectionPath!!) }
                if (!tree.isEmpty) {
                    popupMenu.add(this)
                    popupMenu.addSeparator()
                }
            }

            JBMenuItem("New Group", AllIcons.Actions.NewFolder).apply {
                addActionListener { renameGroup(true) }
                popupMenu.add(this)
            }

            popupMenu.show(tree, mouseEvent.x, mouseEvent.y)
        }
    }

    internal fun get(): JComponent {
        listAndEditorSplitter = JBSplitter(false, 0.3f, 0.2f, 0.7f).apply {
            firstComponent = setUpLeftPanel()
            secondComponent = null
        }

        // Initially select first item in the list, if available
        findFirstRequestDetailNode(tree.model.root as DefaultMutableTreeNode)?.let {
            tree.selectionPath = TreePath(it.path)
        }

        return listAndEditorSplitter
    }

    internal fun getModel() = tree.model

    private fun setUpTree(): JPanel {
        Tree(treeModel).apply {
            tree = this
            this.isRootVisible = false
            this.dragEnabled = true
            this.dropMode = DropMode.ON_OR_INSERT
            this.showsRootHandles = true
            this.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            this.transferHandler = ApiBankTreeTransferHandler()
            this.addTreeSelectionListener(treeItemSelectionListener)
            this.addMouseListener(treeItemClickMouseAdapter)
            this.model.addTreeModelListener(treeModelListener)
            this.expandTree()

            this.emptyText.appendLine(
                /* text = */ "Create new request group",
                /* attrs = */ SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
            ) { renameGroup(true) }

            val treeSpeedSearch = TreeSpeedSearch.installOn(tree, true) {
                (it.lastPathComponent as DefaultMutableTreeNode).getRequestDetail()?.name.orEmpty()
            }

            this.cellRenderer = TreeCellRenderer { _, p1, _, _, _, _, _ ->
                val label = when (val userObject = (p1 as DefaultMutableTreeNode).userObject) {
                    is String -> {
                        val groupName = p1.userObject as String
                        JBLabel("<html><b>${groupName}</b></html>")
                    }

                    is RequestDetail -> {
                        val color = when (userObject.method) {
                            "POST", "PUT" -> Constants.COLOR_BLUE
                            "DELETE" -> Constants.COLOR_RED
                            else -> Constants.COLOR_GREEN
                        }

                        val jLabel =
                            JBLabel("<html><font color='${color}'>[${userObject.method}] </font>${userObject.name}</html>")

                        val prefix = treeSpeedSearch.enteredPrefix

                        if (!prefix.isNullOrBlank() && userObject.name.contains(prefix, true)) {
                            jLabel.border = BorderFactory.createLineBorder(JBColor.GRAY)
                        }

                        jLabel
                    }

                    else -> {
                        JBLabel("Error! Please report.")
                    }
                }

                val desiredWidth = tree.width
                val desiredHeight: Int = label.getPreferredSize().height
                label.size = Dimension(desiredWidth, desiredHeight)

                label
            }
        }

        return createPanelWithTopControls(
            createActionButton("Add", AllIcons.General.Add) { }.apply {
                addMouseListener(addNewItemToolbarMouseAdapter)
                accessibleContext.accessibleName = ADD_NEW_REQUEST_TEXT
            },
            createActionButton("Remove", AllIcons.General.Remove) {
                onDeleteListItemClicked()
            },
            createActionButton("Clone", AllIcons.Actions.Copy) {
                onCloneItemClicked()
            },
            minWidth = 180,
            bottomComponent = JBScrollPane(tree),
            onSearch = null,
        )
    }

    private fun setUpLeftPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = 0
        gbc.gridy = 0
        mainPanel.add(setUpTree(), gbc)

        gbc.pushToNorthwest(mainPanel)
        return mainPanel
    }

    private fun createEmptyRightPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(JBLabel("""Click "+" to create new request"""), gbc)
        return mainPanel.apply {
            preferredSize = Dimension(500, 700)
        }
    }

    private fun executeRequest() {
        val requestDetail =
            (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.getRequestDetail()
        editUiTracker[requestDetail?.id]?.onRunClicked()
    }

    private fun onEditorUpdated(requestDetail: RequestDetail) {
        val index = requestDetails.indexOfFirst { it.id == requestDetail.id }
        requestDetails[index] = requestDetail
    }

    private fun onDeleteListItemClicked() {
        val node = tree.selectionPath!!.lastPathComponent as DefaultMutableTreeNode
        if (node.isGroupNode()) {
            deleteGroup()
        } else {
            deleteRequest()
        }
    }

    private fun showTreeItemClickedPopUpMenu(tree: Tree, mouseEvent: MouseEvent) {
        val isRoot = tree.selectionPath?.pathCount == 1
        val isRequestDetail =
            (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject is RequestDetail
        val isGroup = !isRoot && !isRequestDetail

        val popupMenu = JBPopupMenu()
        JBMenuItem(ADD_NEW_REQUEST_TEXT, AllIcons.General.Add).apply {
            if (isGroup) {
                addActionListener { addNewRequestAction(tree.selectionPath!!) }
                popupMenu.add(this)
            }
        }

        JBMenuItem("Rename group", AllIcons.Actions.Edit).apply {
            if (isGroup) {
                addActionListener { renameGroup(isCreateNewGroup = false) }
                popupMenu.addSeparator()
                popupMenu.add(this)
            }
        }

        JMenuItem("Clone", AllIcons.Actions.Copy).apply {
            if (isGroup) {
                addActionListener { onCloneItemClicked() }
                popupMenu.addSeparator()
                popupMenu.add(this)
            }
        }

        JBMenuItem("Delete group", AllIcons.Actions.DeleteTagHover).apply {
            if (isGroup) {
                addActionListener { deleteGroup() }
                popupMenu.addSeparator()
                popupMenu.add(this)
            }
        }

        JMenuItem("Run", AllIcons.Actions.Execute).apply {
            if (isRequestDetail) {
                addActionListener { executeRequest() }
                popupMenu.add(this)
            }
        }

        JMenuItem("Clone", AllIcons.Actions.Copy).apply {
            if (isRequestDetail) {
                addActionListener { onCloneItemClicked() }
                popupMenu.addSeparator()
                popupMenu.add(this)
            }
        }

        JBMenuItem("Delete request", AllIcons.General.Remove).apply {
            if (isRequestDetail) {
                addActionListener { deleteRequest() }
                popupMenu.addSeparator()
                popupMenu.add(this)
            }
        }

        popupMenu.show(tree, mouseEvent.x, mouseEvent.y)
    }

    private fun onCloneItemClicked() {
        val newNode: DefaultMutableTreeNode

        val treeModel = tree.model as DefaultTreeModel
        val selectionPath = tree.selectionPath!!
        val selectionNode = selectionPath.lastPathComponent as DefaultMutableTreeNode
        val selectionParentNode = selectionNode.parent as DefaultMutableTreeNode
        val selectionIndex = tree.model.getIndexOfChild(selectionNode.parent, selectionNode)
        val selectionGroupExpanded = tree.isExpanded(TreePath((selectionNode).path))

        // If cloning a request
        val requestDetail = selectionNode.getRequestDetail()
        if (requestDetail != null) {
            val requestDetailClone = requestDetail.copy(
                id = UUID.randomUUID().toString(),
                name = requestDetail.name + " (Copy)"
            )

            newNode = DefaultMutableTreeNode(requestDetailClone)
            requestDetails.add(requestDetailClone)
        } else { // Cloning a group (and its children)
            val groupName = selectionNode.getGroupName() + " (Copy)"
            newNode = DefaultMutableTreeNode(groupName)

            // Clone the children
            selectionNode
                .children()
                .asSequence()
                .forEach { treeNode ->
                    val requestDetailClone = treeNode.getRequestDetail()!!.copy(
                        id = UUID.randomUUID().toString(),
                    )
                    requestDetails.add(requestDetailClone)
                    newNode.add(DefaultMutableTreeNode(requestDetailClone))
                }
        }

        // Add the new node below the currently selected node
        treeModel.insertNodeInto(newNode, selectionParentNode, selectionIndex + 1)

        // Select the new node
        tree.selectionPath = TreePath(newNode.path)

        // Expand the newly cloned group if the original node was also expanded
        if (selectionGroupExpanded) {
            tree.expandPath(tree.selectionPath)
        }
    }

    private fun renameGroup(isCreateNewGroup: Boolean) {
        val node = (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)
        val allGroupNames = tree.getAllGroupNames()

        val name = Messages.showInputDialog(
            /* message = */ "Enter group name",
            /* title = */ "Group Name",
            /* icon = */ AllIcons.Actions.NewFolder,
            /* initialValue = */ if (isCreateNewGroup) "" else (node?.userObject as? String).orEmpty(),
            /* validator = */ object : InputValidator {
                override fun checkInput(inputString: String?) =
                    !inputString.isNullOrBlank() && !allGroupNames.contains(inputString)

                override fun canClose(inputString: String?) = true
            }
        )

        if (name.isNullOrBlank()) return

        if (isCreateNewGroup) {
            // Add the new group to the model
            val newGroup = DefaultMutableTreeNode(name)
            (tree.model as DefaultTreeModel).insertNodeInto(
                newGroup,
                tree.model.root as DefaultMutableTreeNode,
                0
            )

            // Mark this new group as selected so that new request is added to it
            tree.selectionPath = TreePath(newGroup)
            addNewRequestAction(tree.selectionPath!!)
            tree.expandPath(TreePath(tree.model.root))
        } else {
            (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.let {
                it.userObject = name
                (tree.model as DefaultTreeModel).reload(it)
            }
        }
    }

    private fun deleteGroup() {
        val selectionPath = tree.selectionPath ?: return
        val selectedNode = selectionPath.lastPathComponent as DefaultMutableTreeNode
        val groupName = selectedNode.userObject as String

        val confirmDialog = Messages.showOkCancelDialog(
            "Delete '${groupName}' and all its content?",
            "Delete Group",
            "Delete",
            "Cancel",
            AllIcons.General.WarningDialog
        )

        if (confirmDialog == Messages.CANCEL) return
        tree.selectionPath = findNextRequest() ?: findPreviousRequest()
        (tree.model as DefaultTreeModel).removeNodeFromParent(selectedNode)
    }

    private fun deleteRequest() {
        val selectionPath = tree.selectionPath ?: return
        val selectedNode = selectionPath.lastPathComponent as DefaultMutableTreeNode

        val requestDetail = (selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject as RequestDetail

        val confirmDialog = Messages.showOkCancelDialog(
            "Delete '${requestDetail.name}'?",
            "Group Request",
            "Delete",
            "Cancel",
            AllIcons.General.WarningDialog
        )

        if (confirmDialog == Messages.CANCEL) return

        tree.selectionPath = findNextRequest() ?: findPreviousRequest()
        (tree.model as DefaultTreeModel).removeNodeFromParent(selectedNode)
    }

    private fun addNewRequestAction(selectionPath: TreePath) {
        val selectedNode = selectionPath.lastPathComponent as DefaultMutableTreeNode
        val selectedIndex = if (selectedNode.userObject is String) {
            0
        } else {
            tree.model.getIndexOfChild(selectedNode.parent, selectedNode) + 1
        }

        val parentNode = if (selectedNode.userObject is String) {
            selectedNode
        } else {
            selectedNode.parent as DefaultMutableTreeNode
        }

        val newDefaultRequest = RequestDetail.DEFAULT

        val model = tree.model as DefaultTreeModel
        val newNode = DefaultMutableTreeNode(newDefaultRequest)

        model.insertNodeInto(newNode, parentNode, selectedIndex)
        tree.selectionPath = TreePath(newNode.path)
        requestDetails.add(newDefaultRequest)
        tree.expandPath(tree.selectionPath)
    }

    private fun findNextRequest(): TreePath? {
        var nextNode: DefaultMutableTreeNode? = tree.selectionPath?.lastPathComponent as DefaultMutableTreeNode
        nextNode = when (nextNode?.isGroupNode()) {
            true -> nextNode.nextSibling
            else -> nextNode?.nextNode
        }

        while (nextNode != null) {
            if (nextNode.userObject is RequestDetail) {
                return TreePath(nextNode.path)
            }
            nextNode = (nextNode as? DefaultMutableTreeNode)?.nextNode
        }

        return null
    }

    private fun findPreviousRequest(): TreePath? {
        var nextNode: DefaultMutableTreeNode? = tree.selectionPath?.lastPathComponent as DefaultMutableTreeNode
        if (nextNode?.isGroupNode() == true) {
            nextNode = nextNode.previousSibling
            nextNode = nextNode?.lastChild as? DefaultMutableTreeNode
        } else {
            nextNode = nextNode?.previousNode
        }
        while (nextNode != null) {
            if (nextNode.userObject is RequestDetail) {
                return TreePath(nextNode.path)
            }
            nextNode = (nextNode as? DefaultMutableTreeNode)?.previousNode
        }

        return null
    }

    private fun findFirstRequestDetailNode(node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        return when (node.userObject) {
            is RequestDetail -> node
            else -> {
                for (i in 0 until node.childCount) {
                    val child = findFirstRequestDetailNode(node.getChildAt(i) as DefaultMutableTreeNode)
                    if (child != null) {
                        return child
                    }
                }
                null
            }
        }
    }

    companion object {
        private const val ADD_NEW_REQUEST_TEXT = "Add new request"
    }
}