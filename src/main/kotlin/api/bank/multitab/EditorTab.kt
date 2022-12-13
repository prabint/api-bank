package api.bank.multitab

import api.bank.list.toListOfKeyValue
import api.bank.models.Constants
import api.bank.models.Constants.COLOR_GREEN
import api.bank.models.Constants.COLOR_RED
import api.bank.models.RequestDetail
import api.bank.models.ResponseDetail
import api.bank.repository.CoreRepository
import api.bank.table.TableCellListener
import api.bank.table.TableColumnAdjuster
import api.bank.utils.*
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.listener.SimpleDocumentListener
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.table.DefaultTableModel
import javax.swing.text.DefaultCaret
import kotlin.random.Random.Default.nextLong

/**
 * A form UI to create and edit request
 */
class EditorTab(
    dispatcherProvider: DispatcherProvider,
    private val requestDetailInMemory: RequestDetail,
    private val gson: Gson,
    private val coreRepository: CoreRepository,
    private val getVariables: () -> ArrayList<Array<String>>,
    private val onDisplayNameUpdated: (RequestDetail) -> Unit,
    private val onMethodUpdated: (RequestDetail) -> Unit,
) {
    // UI components
    private lateinit var jHeader: JBTable
    private lateinit var jBody: JBTextArea
    private lateinit var jOutput: JBTextArea
    private lateinit var jOutputLabel: JBLabel

    // Data
    private val headerTableModel = DefaultTableModel()

    // Network
    private var networkJob: Job? = null
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(dispatcherProvider.main + job)

    internal var panel: JComponent = get()

    private fun get(): JComponent {
        val upperPanel = JPanel()
        upperPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = Insets(4, 0, 4, 4)

        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER
        gbc.gridx = 0
        gbc.gridy = 0
        upperPanel.add(JBLabel("Name"), gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.gridwidth = 2
        upperPanel.add(JBTextField().apply {
            accessibleContext.accessibleName = "Request name"
            text = requestDetailInMemory.name
            document.addDocumentListener(object : SimpleDocumentListener() {
                override fun update(e: DocumentEvent?) {
                    requestDetailInMemory.name = text
                    onDisplayNameUpdated(requestDetailInMemory)
                }
            })
        }, gbc)

        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER
        gbc.gridx = 0
        gbc.gridy = 1
        upperPanel.add(JBLabel("Method"), gbc)

        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        gbc.weightx = 0.0
        upperPanel.add(ComboBox(Constants.METHODS).apply {
            accessibleContext.accessibleName = "Request method"
            selectedItem = requestDetailInMemory.method
            addItemListener {
                requestDetailInMemory.method = selectedItem as String
                onMethodUpdated(requestDetailInMemory)
            }
        }, gbc)

        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER
        gbc.gridx = 0
        gbc.gridy = 2
        upperPanel.add(JBLabel("Url"), gbc)

        gbc.gridx = 1
        gbc.gridy = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.NORTHWEST
        upperPanel.add(JBTextField().apply {
            accessibleContext.accessibleName = "Request url"
            text = requestDetailInMemory.url
            document.addDocumentListener(object : SimpleDocumentListener() {
                override fun update(e: DocumentEvent?) {
                    requestDetailInMemory.url = text
                }
            })
        }, gbc)

        gbc.gridwidth = 1
        gbc.gridx = 2
        gbc.gridy = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST
        upperPanel.add(
            createActionButton("Run", AllIcons.Actions.Execute) {
                onRunClicked()
            }.apply {
                accessibleContext.accessibleName = "Execute api"
            }, gbc
        )

        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.anchor = GridBagConstraints.NORTHWEST
        upperPanel.add(JBLabel("Header"), gbc)

        gbc.gridx = 0
        gbc.gridy = 4
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridwidth = 3
        upperPanel.add(setUpHeaderTable(), gbc)

        val lowerPanel = setUpBodyAndOutputPanel()
        gbc.pushToNorthwest(upperPanel)

        val mainPanel = JPanel(GridBagLayout()).apply {
            add(JBSplitter(true, 0.40f).apply {
                firstComponent = upperPanel
                secondComponent = lowerPanel
            }, gbc)
            preferredSize = Dimension(500, 700)
        }
        return mainPanel
    }

    private fun setUpBodyAndOutputPanel(): JBSplitter {
        val bodyPanel = JPanel()
        bodyPanel.layout = BoxLayout(bodyPanel, BoxLayout.Y_AXIS)
        bodyPanel.add(JBLabel("Body").apply {
            border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
        })

        JBTextArea(1, 15).apply {
            jBody = this
            accessibleContext.accessibleName = "Request body"
            tabSize = 2
            font = monoSpacedFont
            text = requestDetailInMemory.body
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            document.addDocumentListener(object : SimpleDocumentListener() {
                override fun update(e: DocumentEvent?) {
                    requestDetailInMemory.body = jBody.text
                }
            })
        }

        val bodyWithController = createPanelWithLeftControls(
            createToggleButton("Wrap", AllIcons.Actions.ToggleSoftWrap) {
                jBody.wrapStyleWord = it
                jBody.lineWrap = it
            }.apply {
                accessibleContext.accessibleName = "Wrap body"
            },
            createActionButton("Formatter", AllIcons.Actions.PrettyPrint) { beautifyJsonBody() }.apply {
                accessibleContext.accessibleName = "Format body"
            },
            minWidth = 250,
            bottomComponent = JBScrollPane(jBody)
        )

        bodyPanel.add(bodyWithController.apply {
            alignmentX = Component.LEFT_ALIGNMENT
        })

        val outputPanel = JPanel()
        outputPanel.layout = BoxLayout(outputPanel, BoxLayout.Y_AXIS)
        outputPanel.add(JBLabel("Output").apply {
            jOutputLabel = this
            accessibleContext.accessibleName = "Request output label"
            border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
        })

        JBTextArea(1, 15).apply {
            jOutput = this
            accessibleContext.accessibleName = "Request output"
            font = monoSpacedFont
            isEditable = false
            text = """Click "Play" icon or double-tap list item to see output"""
            caret = DefaultCaret().apply { updatePolicy = DefaultCaret.NEVER_UPDATE }
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        val outputWithController = createPanelWithLeftControls(
            createToggleButton("Wrap", AllIcons.Actions.ToggleSoftWrap) {
                jOutput.wrapStyleWord = it
                jOutput.lineWrap = it
            },
            minWidth = 250,
            bottomComponent = JBScrollPane(jOutput)
        )

        outputPanel.add(outputWithController.apply {
            alignmentX = Component.LEFT_ALIGNMENT
        })

        return JBSplitter(true, 0.5f).apply {
            firstComponent = bodyPanel
            secondComponent = outputPanel
        }
    }

    private fun setUpHeaderTable(): JComponent {
        JBTable().apply {
            jHeader = this
            accessibleContext.accessibleName = "Request header table"
            model = headerTableModel

            headerTableModel.setDataVector(
                requestDetailInMemory.header.toTypedArray(),
                arrayOf("Key", "Value")
            )

            jHeader.columnModel.getColumn(0).minWidth = 80
            jHeader.columnModel.getColumn(1).minWidth = 300

            // Listen for cell value changes
            TableCellListener(this, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    requestDetailInMemory.header = headerTableModel.toListOfKeyValue()
                }
            })

            autoResizeMode = JBTable.AUTO_RESIZE_OFF
            TableColumnAdjuster(jHeader, 10).apply {
                adjustColumns()
                setDynamicAdjustment(true)
                setOnlyAdjustLarger(true)
                setColumnHeaderIncluded(true)
                setColumnDataIncluded(true)
            }
        }

        return createPanelWithTopControls(
            createActionButton("Add", AllIcons.General.Add) { onAddRowClicked() }.apply {
                accessibleContext.accessibleName = "Add header row"
            },
            createActionButton("Remove", AllIcons.General.Remove) { onDeleteRowClicked() },
            minWidth = 350,
            bottomComponent = JBScrollPane(jHeader)
        )
    }

    private fun onAddRowClicked() {
        headerTableModel.addRow(arrayOf("", ""))
        requestDetailInMemory.header.add(arrayOf("", ""))
        if (headerTableModel.dataVector.isNotEmpty()) {
            jHeader.setRowSelectionInterval(
                headerTableModel.dataVector.lastIndex,
                headerTableModel.dataVector.lastIndex
            )
        }
    }

    private fun onDeleteRowClicked() {
        val selectedRow = jHeader.selectedRow
        if (selectedRow == -1) return
        headerTableModel.removeRow(selectedRow)

        // Fix: Force selection of next row to avoid black cell issue
        if (headerTableModel.dataVector.isNotEmpty()) {
            when (selectedRow) {
                0 -> jHeader.setRowSelectionInterval(0, 0)
                else -> jHeader.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
            }
        }

        if (headerTableModel.dataVector.size > 0) {
            jHeader.setRowSelectionInterval(1, 1)
        }

        requestDetailInMemory.header.removeAt(selectedRow)
    }

    /**
     * Gson cannot pretty print if json contains variables like this:
     * {
     * "key": ${{count}}
     * }
     *
     * So, first replace ${{count}} with a long, e.g 1234
     * {
     * "key": 1234
     * }
     *
     * Then, use gson to beautify it.
     * {
     *   "key": 1234
     * }
     *
     * Finally, revert 1234 back to ${{count}}
     * {
     *   "key": ${{count}}
     * }
     */
    private fun beautifyJsonBody() {
        try {
            if (jBody.text.isEmpty()) return

            // Get all the keys from env var
            val variables: ArrayList<Array<String>> = getVariables()

            // Generate unique longs
            val set = HashSet<Long>()
            while (set.size != variables.size) {
                set.add(nextLong(123_456_789, 987_876_765))
            }

            // Map each variable to a unique long
            // e.g.: mapOf("${{count}}" to 1234, ...)
            val map = HashMap<String, Long>()
            for (v in variables) {
                val key = v[0].toVariableRepresentation()
                if (!map.containsKey(key)) {
                    val long = set.first()
                    map[key] = long
                    set.remove(long)
                }
            }

            // Replace each variable with corresponding unique long, i.e. ${{count}} -> 1234
            var newBody = jBody.text
            for (variable in variables) {
                val key = variable[0].toVariableRepresentation()
                newBody = newBody.replace(key, map[key].toString(), false)
            }

            // Prettify
            jBody.text = gson.toJson(JsonParser.parseString(newBody))

            // Revert long numbers back variable, i.e, 1234 -> ${{count}}
            newBody = jBody.text
            for (variable in variables) {
                val key = variable[0].toVariableRepresentation()
                newBody = newBody.replace(map[key].toString(), key, false)
            }

            jBody.text = newBody
        } catch (e: Exception) {
            jOutput.text = e.stackTraceToString()
        }
    }

    internal fun onRunClicked() {
        jOutputLabel.text = "Loading..."

        networkJob?.cancel()
        networkJob = uiScope.launch(CoroutineExceptionHandler { _, e ->
            jOutput.text = "${e.message}\n${e.stackTraceToString()}"
            jOutputLabel.text = "<html><font color='red'>Exception</font></html>"
        }) {
            jOutput.text = "Loading..."

            val output = coreRepository.executeRequest(
                requestDetail = requestDetailInMemory,
                variables = getVariables(),
            )

            setOutputBody(output)

            val color = when (output.code < 400) {
                true -> COLOR_GREEN
                false -> COLOR_RED
            }

            jOutputLabel.text = "<html><font color='$color'>${output.code}</font> ${output.message}</html>"
        }
    }

    private fun setOutputBody(output: ResponseDetail) {
        val outputBody = output.body

        jOutput.text = if (outputBody.isNullOrBlank()) {
            outputBody
        } else {
            try {
                gson.toJson(JsonParser.parseString(outputBody))
            } catch (e: JsonSyntaxException) {
                outputBody
            }
        }
    }
}
