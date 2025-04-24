package api.bank.multitab

import api.bank.list.ListTransferHandler
import api.bank.list.toListOfKeyValue
import api.bank.models.VariableCollection
import api.bank.table.TableCellListener
import api.bank.table.TableColumnAdjuster
import api.bank.utils.createActionButton
import api.bank.utils.createPanelWithTopControls
import api.bank.utils.listener.SimpleDocumentListener
import api.bank.utils.pushToNorthwest
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.table.DefaultTableModel

/**
 * UI that has list of variable collection on the left and table on the right
 */
class VariablesTab(
    private val collectionsInMemory: ArrayList<VariableCollection>,
    private val gson: Gson,
) {
    private lateinit var jTable: JBTable
    private lateinit var jbSplitter: JBSplitter
    private lateinit var jDisplayName: JBTextField
    private lateinit var jList: JBList<VariableCollection>

    private val tableModel = DefaultTableModel()
    private val listModel = DefaultListModel<VariableCollection>()

    fun getActive() = collectionsInMemory.find { it.isActive == true }?.variableItems ?: arrayListOf()

    private val listDataListener = object : ListDataListener {
        override fun intervalAdded(e: ListDataEvent?) {
            // Only call createRightPanel() if an item was added to empty list
            if (listModel.size() == 1) {
                jbSplitter.secondComponent = createRightPanel()
            }

        }

        override fun intervalRemoved(e: ListDataEvent?) {
            // Only call createEmptyRightPanel() if list is empty
            if (jList.isEmpty) {
                jbSplitter.secondComponent = createEmptyRightPanel()
            }
        }

        override fun contentsChanged(e: ListDataEvent?) {
            // no-op
        }
    }

    private val listSelectionListener = object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            if (listModel.isEmpty || jList.selectedIndex == -1) return
            jDisplayName.text = collectionsInMemory[jList.selectedIndex].name
            setTableModel()
        }
    }

    private val displayNameSimpleDocumentListener = object : SimpleDocumentListener() {
        override fun update(e: DocumentEvent?) {
            val selectedIndex = jList.selectedIndex
            if (selectedIndex == -1) return

            val newDisplayName = jDisplayName.text
            collectionsInMemory[selectedIndex].name = newDisplayName
            listModel.set(selectedIndex, collectionsInMemory[selectedIndex])
        }
    }

    private val listMouseAdapter = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e)) {
                jList.selectedIndex = jList.locationToIndex(e.point)
                setUpPopUpMenu(jList, e)
            }
        }
    }

    internal fun get(): JPanel {
        jbSplitter = JBSplitter(false, 0.3f, 0.2f, 0.7f)
        jbSplitter.firstComponent = createLeftPanel()
        jbSplitter.secondComponent = when (listModel.isEmpty) {
            true -> createEmptyRightPanel()
            false -> createRightPanel()
        }
        return jbSplitter
    }

    private fun createLeftPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = 0
        gbc.gridy = 0

        mainPanel.add(
            createPanelWithTopControls(
                createActionButton("Add", AllIcons.General.Add) { onAddNewListItemClicked() }.apply {
                    accessibleContext.accessibleName = "Add new environment variable set"
                },
                createActionButton("Remove", AllIcons.General.Remove) { onDeleteListItemClicked() },
                createActionButton("Clone", AllIcons.Actions.Copy) { onCloneListItemClicked() },
                createActionButton("Set Active", AllIcons.Actions.SetDefault) { onSetActiveClicked() },
                minWidth = 250,
                bottomComponent = JBScrollPane(JBList<VariableCollection>().apply {
                    jList = this
                    model = listModel
                    listModel.addAll(collectionsInMemory)

                    cellRenderer = ListCellRenderer { _, value, _, _, _ ->
                        when (value.isActive) {
                            true -> JBLabel(
                                "${value?.name}",
                                AllIcons.Debugger.ThreadStates.Idle,
                                SwingConstants.LEFT
                            )

                            else -> JBLabel("${value?.name}")
                        }.apply {
                            border = JBUI.Borders.emptyLeft(4)
                        }
                    }
                    fixedCellHeight = 25
                    setEmptyText("Click '+' to create variable set")

                    // Initially select first item in the list if available
                    if (!listModel.isEmpty) {
                        selectedIndex = 0
                    }

                    // Setup DnD
                    selectionMode = ListSelectionModel.SINGLE_SELECTION
                    dropMode = DropMode.INSERT
                    dragEnabled = true
                    transferHandler = ListTransferHandler<VariableCollection>(LIST_DATA_FLAVOR) { from, to ->
                        val fromItem = collectionsInMemory[from]
                        collectionsInMemory.removeAt(from)
                        collectionsInMemory.add(to, fromItem)
                        selectedIndex = to
                    }

                    listModel.addListDataListener(listDataListener)
                    addListSelectionListener(listSelectionListener)
                    addMouseListener(listMouseAdapter)
                }),
            ), gbc
        )

        gbc.pushToNorthwest(mainPanel)

        return mainPanel
    }

    private fun createRightPanel(): JPanel {
        val mainPanel = JPanel()
        mainPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()

        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.NONE
        mainPanel.add(JBLabel("Display name:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        JBTextField().apply {
            jDisplayName = this
            accessibleContext.accessibleName = "Variable collection name"

            // Set initial value
            if (!listModel.isEmpty) {
                text = listModel.get(0).name
            }

            document.addDocumentListener(displayNameSimpleDocumentListener)
        }
        mainPanel.add(jDisplayName, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH
        JBTable().apply {
            jTable = this
            accessibleContext.accessibleName = "Variables table"
            model = tableModel

            // Set initial table
            setTableModel()

            // Listen for cell value changes
            TableCellListener(this, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    collectionsInMemory[jList.selectedIndex].variableItems = tableModel.toListOfKeyValue()
                }
            })

            autoResizeMode = JBTable.AUTO_RESIZE_OFF
            TableColumnAdjuster(jTable, 10).apply {
                adjustColumns()
                setDynamicAdjustment(true)
                setOnlyAdjustLarger(true)
                setColumnHeaderIncluded(true)
                setColumnDataIncluded(true)
            }
        }

        mainPanel.add(
            createPanelWithTopControls(
                createActionButton("Add", AllIcons.General.Add) { onAddNewRowClicked() }.apply {
                    accessibleContext.accessibleName = "Add new row"
                },
                createActionButton("Remove", AllIcons.General.Remove) { onDeleteRowClicked() }.apply {
                    accessibleContext.accessibleName = "Remove selected row"
                },
                minWidth = 350,
                bottomComponent = JBScrollPane(jTable),
            ), gbc
        )

        gbc.pushToNorthwest(mainPanel)

        return mainPanel.apply {
            preferredSize = Dimension(500, 700)
        }
    }

    private fun createEmptyRightPanel(): JPanel {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(
            JBLabel(
                "<html>" +
                        "Create environment variable sets for " +
                        "different stages like Staging, Production etc." +
                        "<br><br>" +
                        "Only one set can be active at a time marked by green check mark." +
                        "</html>"
            ), gbc
        )
        return mainPanel.apply {
            preferredSize = Dimension(500, 700)
        }
    }

    private fun setTableModel() {
        val selectedIndex = jList.selectedIndex
        if (selectedIndex == -1) return

        tableModel.setDataVector(
            collectionsInMemory[selectedIndex].variableItems.toTypedArray(),
            arrayOf("Key", "Value")
        )

        jTable.columnModel.getColumn(0).minWidth = 80
        jTable.columnModel.getColumn(1).minWidth = 300
    }

    private fun setUpPopUpMenu(list: JBList<VariableCollection>, e: MouseEvent) {
        val menu = JPopupMenu()

        val clone = JMenuItem("Clone", AllIcons.Actions.Copy).apply {
            accessibleContext.accessibleName = "Clone variable collection"
            addActionListener { onCloneListItemClicked() }
        }

        val delete = JMenuItem("Delete", AllIcons.General.Remove).apply {
            addActionListener { onDeleteListItemClicked() }
        }

        val setActive = JMenuItem("Set Active", AllIcons.Actions.SetDefault).apply {
            addActionListener { onSetActiveClicked() }
        }

        menu.add(clone)
        menu.add(setActive)
        menu.addSeparator()
        menu.add(delete)
        menu.show(list, e.point.x, e.point.y)
    }

    private fun onAddNewRowClicked() {
        val newRowData = arrayOf("", "")
        tableModel.addRow(newRowData)
        collectionsInMemory[jList.selectedIndex].variableItems.add(newRowData)
    }

    private fun onDeleteRowClicked() {
        val selectedRow = jTable.selectedRow
        if (selectedRow == -1) return
        tableModel.removeRow(selectedRow)
        collectionsInMemory[jList.selectedIndex].variableItems.removeAt(selectedRow)
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

        collectionsInMemory.removeAt(selectedIndex)
        listModel.remove(selectedIndex)

        if (listModel.isEmpty) {
            createEmptyRightPanel()
            return
        }

        when (selectedIndex == jList.model.size) {
            true -> jList.selectedIndex = jList.model.size - 1
            false -> jList.selectedIndex = selectedIndex
        }
    }

    private fun onAddNewListItemClicked() {
        val dummy = VariableCollection(
            id = UUID.randomUUID().toString(),
            name = "(New set of variables)",
            variableItems = ArrayList(),
            isActive = listModel.isEmpty,
        )

        collectionsInMemory.add(0, dummy)
        listModel.add(0, dummy)
        jList.selectedIndex = 0
    }

    private fun onSetActiveClicked() {
        val selectedIndex = jList.selectedIndex
        if (selectedIndex == -1) return

        val previousActiveIndex = collectionsInMemory.indexOfFirst { it.isActive == true }

        // Unset current
        if (previousActiveIndex == selectedIndex) {
            listModel[selectedIndex] = listModel[selectedIndex].copy(isActive = false)
            collectionsInMemory[selectedIndex].isActive = false
            return
        }

        // Unset previous selection if any
        if (previousActiveIndex != -1) {
            // Update object instead of just property so jList renders changes immediately
            listModel[previousActiveIndex] = listModel[previousActiveIndex].copy(isActive = false)
            collectionsInMemory[previousActiveIndex].isActive = false
        }

        // Set new selection
        listModel[selectedIndex] = listModel[selectedIndex].copy(isActive = true)
        collectionsInMemory[selectedIndex].isActive = true
    }

    private fun onCloneListItemClicked() {
        val selectedIndex = jList.selectedIndex
        if (selectedIndex == -1) return

        val selectedItem = jList.selectedValue
        val clone = gson.fromJson(gson.toJson(selectedItem), VariableCollection::class.java).apply {
            id = UUID.randomUUID().toString()
            name = "Copy of ${selectedItem.name}"
            isActive = false
        }

        collectionsInMemory.add(0, clone)
        listModel.add(0, clone)

        jList.selectedIndex = 0
    }

    companion object {
        private val LIST_DATA_FLAVOR = DataFlavor(
            VariableCollection::class.java,
            "java/${VariableCollection::class.simpleName}"
        )
    }
}
