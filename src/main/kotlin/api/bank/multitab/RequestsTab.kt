package api.bank.multitab

import api.bank.list.ListTransferHandler
import api.bank.models.Constants.COLOR_BLUE
import api.bank.models.Constants.COLOR_GREEN
import api.bank.models.Constants.COLOR_RED
import api.bank.models.RequestDetail
import api.bank.repository.CoreRepository
import api.bank.utils.createActionButton
import api.bank.utils.createPanelWithTopControls
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.pushToNorthwest
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * UI that has list API requests on the left and editor on the right
 */
class RequestsTab(
    private val gson: Gson,
    private val coreRepository: CoreRepository,
    private val dispatchProvider: DispatcherProvider,
    private val modifiableItems: ArrayList<RequestDetail>,
    private val getVariables: () -> ArrayList<Array<String>>,
) {
    private lateinit var listAndEditorSplitter: JBSplitter
    private lateinit var jList: JBList<RequestDetail>

    private val listModel = DefaultListModel<RequestDetail>()

    // Stores UI in memory to emulate tab-like feature where the UI
    // components are maintained even after navigating to different tabs
    private val editUiTracker = HashMap<String, EditorTab>()

    private val listMouseAdapter = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e)) {
                jList.selectedIndex = jList.locationToIndex(e.point)
                setUpPopUpMenu(jList, e)
            }
        }

        override fun mouseClicked(e: MouseEvent?) {
            if (e?.clickCount == 2) {
                editUiTracker[jList.selectedValue.id]?.onRunClicked()
            }
        }
    }

    private val listSelectionListener = object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            val selectedValue = jList.selectedValue

            // selectedValue will be null when a list item is deleted and before next item is selected
            if (selectedValue == null) {
                if (listModel.isEmpty) {
                    listAndEditorSplitter.secondComponent = null
                }
                return
            }

            if (editUiTracker.containsKey(selectedValue.id)) {
                listAndEditorSplitter.secondComponent = editUiTracker[selectedValue.id]?.panel
                return
            }

            val editorTab = EditorTab(
                dispatcherProvider = dispatchProvider,
                requestDetailInMemory = jList.selectedValue,
                gson = gson,
                coreRepository = coreRepository,
                getVariables = getVariables,
                onDisplayNameUpdated = { detail -> onEditorUpdated(detail) },
                onMethodUpdated = { detail -> onEditorUpdated(detail) }
            )
            editUiTracker[jList.selectedValue?.id!!] = editorTab
            listAndEditorSplitter.secondComponent = editorTab.panel
        }
    }

    internal fun get(): JComponent {
        listAndEditorSplitter = JBSplitter(false, 0.3f, 0.2f, 0.7f).apply {
            firstComponent = createLeftPanel()
            secondComponent = null
        }

        // Initially select first item in the list, if available
        if (!listModel.isEmpty) {
            jList.selectedIndex = 0
        }

        return listAndEditorSplitter
    }

    private fun createLeftPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = 0
        gbc.gridy = 0
        mainPanel.add(setUpList(), gbc)

        gbc.pushToNorthwest(mainPanel)
        return mainPanel
    }

    private fun setUpList(): JPanel {
        JBList<RequestDetail>().apply {
            jList = this
            accessibleContext.accessibleName = "Requests list"
            model = listModel
            listModel.addAll(modifiableItems)

            cellRenderer = ListCellRenderer { _, value, _, _, _ ->
                val color = when (value?.method) {
                    "POST", "PUT" -> COLOR_BLUE
                    "DELETE" -> COLOR_RED
                    else -> COLOR_GREEN
                }
                JBLabel("<html><font color='${color}'>[${value?.method}]</font> ${value?.name}</html>")
            }
            fixedCellHeight = 25
            setEmptyText("""Click "+" to create new request""")

            // Setup DnD
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            dropMode = DropMode.INSERT
            dragEnabled = true
            transferHandler = ListTransferHandler<RequestDetail>(LIST_DATA_FLAVOR) { from, to ->
                val fromItem = modifiableItems[from]
                modifiableItems.removeAt(from)
                modifiableItems.add(to, fromItem)
                selectedIndex = to
            }

            addListSelectionListener(listSelectionListener)
            addMouseListener(listMouseAdapter)
        }

        return createPanelWithTopControls(
            createActionButton("Add", AllIcons.General.Add) { onAddNewListItemClicked() }.apply {
                accessibleContext.accessibleName = "Add new request"
            },
            createActionButton("Remove", AllIcons.General.Remove) { onDeleteListItemClicked() },
            createActionButton("Clone", AllIcons.Actions.Copy) { onCloneListItemClicked() }.apply {
                accessibleContext.accessibleName = "Clone request"
            },
            minWidth = 180,
            bottomComponent = JBScrollPane(jList),
            onSearch = { searchText -> onSearch(searchText) }
        )
    }

    private fun setUpPopUpMenu(list: JBList<RequestDetail>, e: MouseEvent) {
        val menu = JPopupMenu()
        val run = JMenuItem("Run", AllIcons.Actions.Execute).apply {
            addActionListener { }
        }
        val add = JMenuItem("Add", AllIcons.General.Add).apply {
            addActionListener { onAddNewListItemClicked() }
        }
        val clone = JMenuItem("Clone", AllIcons.Actions.Copy).apply {
            addActionListener { onCloneListItemClicked() }
        }
        val delete = JMenuItem("Delete", AllIcons.General.Remove).apply {
            addActionListener { onDeleteListItemClicked() }
        }

        menu.add(run)
        menu.add(add)
        menu.add(clone)
        menu.add(delete)
        menu.show(list, e.point.x, e.point.y)
    }

    private fun onEditorUpdated(requestDetail: RequestDetail) {
        val index = listModel.toArray().indexOfFirst { (it as RequestDetail).id == requestDetail.id }
        listModel.set(index, requestDetail)
    }

    private fun onDeleteListItemClicked() {
        val selectedIndex = jList.selectedIndex
        if (selectedIndex == -1) return

        val result: Int = Messages.showYesNoDialog(
            "Are you sure you want to delete ${listModel.get(selectedIndex).name}?",
            "Confirm Delete",
            AllIcons.General.Warning
        )

        if (result == MessageConstants.NO) return

        val idToBeDeleted = modifiableItems[selectedIndex].id
        val indexToBeDeleted = modifiableItems.indexOfFirst { it.id == idToBeDeleted }
        editUiTracker.remove(idToBeDeleted)
        // If list is filtered via search, [modifiableItems] and [listModel] differ as [listModel] only contains
        // filtered items whereas [modifiableItems] contains all items.
        // So, [indexToBeDeleted] is needed to delete correct item from [modifiableItems].
        modifiableItems.removeAt(indexToBeDeleted)
        listModel.remove(selectedIndex)
        // Select next item from the list
        if (listModel.isEmpty) return
        jList.selectedIndex = when (selectedIndex == jList.model.size) {
            true -> jList.model.size - 1
            false -> selectedIndex
        }
    }

    private fun onAddNewListItemClicked() {
        val dummy = RequestDetail(
            id = UUID.randomUUID().toString(),
            name = "(New Request)",
            url = "https://",
            method = "GET",
            header = ArrayList(),
            body = null,
        )

        // Add new item to top of list and select it
        modifiableItems.add(0, dummy)
        listModel.add(0, dummy)
        jList.selectedIndex = 0
    }

    private fun onCloneListItemClicked() {
        val selectedIndex = jList.selectedIndex
        if (selectedIndex == -1) return

        val selectedItem = jList.selectedValue
        val clone = gson.fromJson(gson.toJson(selectedItem), RequestDetail::class.java).apply {
            id = UUID.randomUUID().toString()
            name = "Copy of ${selectedItem.name}"
        }

        modifiableItems.add(0, clone)
        listModel.add(0, clone)

        jList.selectedIndex = 0
    }

    private fun onSearch(searchText: String) {
        val filteredList = modifiableItems.filter {
            it.name?.lowercase()?.contains(searchText.lowercase()) == true
        }
        listModel.removeAllElements()
        listModel.addAll(filteredList)
        if (filteredList.isNotEmpty()) jList.selectedIndex = 0
    }

    companion object {
        private val LIST_DATA_FLAVOR = DataFlavor(RequestDetail::class.java, "java/${RequestDetail::class.simpleName}")
    }
}
