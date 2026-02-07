package api.bank.multitab

import api.bank.models.Constants
import api.bank.models.Constants.COLOR_GREEN
import api.bank.models.Constants.COLOR_RED
import api.bank.models.RequestDetail
import api.bank.models.ResponseDetail
import api.bank.repository.CoreRepository
import api.bank.utils.*
import api.bank.utils.dispatcher.DispatcherProvider
import api.bank.utils.listener.SimpleDocumentListener
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import java.awt.*
import java.awt.Cursor.getPredefinedCursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.text.DefaultCaret
import kotlin.random.Random.Default.nextLong

/**
 * A form UI to create and edit request
 */
class EditorTab(
    dispatcherProvider: DispatcherProvider,
    val requestDetailInMemory: RequestDetail,
    private val gson: Gson,
    private val coreRepository: CoreRepository,
    private val getVariables: () -> ArrayList<Array<String>>,
    private val onDisplayNameUpdated: (RequestDetail) -> Unit,
    private val onMethodUpdated: (RequestDetail) -> Unit,
) {
    // UI components
    private lateinit var jHeader: TableView<Array<String>>
    private lateinit var jBody: JBTextArea
    private lateinit var jOutput: JBTextArea
    private lateinit var jOutputLabel: JBLabel
    private lateinit var jResponseHeader: JBTable

    // Data
    private val headerTableModel = getTableModel()
    private val responseTableModel = getTableModel()

    // Network
    private var networkJob: Job? = null
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(dispatcherProvider.main + job)

    internal var panel: JComponent = get()

    private fun get(): JComponent {
        val upperPanel = JPanel()
        upperPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = JBUI.insets(4, 0, 4, 4)

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
            addActionListener { } // Prevent closing dialog when Enter key is pressed
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
            addActionListener { } // Prevent closing dialog when Enter key is pressed
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
        upperPanel.add(JBLabel("Request header"), gbc)

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
        bodyPanel.add(JBLabel("Request body").apply {
            border = BorderFactory.createEmptyBorder(0, 0, 6, 0)
        })

        JBTextArea(1, 15).apply {
            jBody = this
            accessibleContext.accessibleName = "Request body text area"
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

        JBTextArea(1, 15).apply {
            jOutput = this
            accessibleContext.accessibleName = "Request output"
            font = monoSpacedFont
            isEditable = false
            text = """Click "Play" icon or double-tap list item to see output"""
            caret = DefaultCaret().apply { updatePolicy = DefaultCaret.NEVER_UPDATE }
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        return JBSplitter(true, 0.5f).apply {
            firstComponent = bodyPanel
            secondComponent = tabbedBodyAndHeaderPanel()
        }
    }

    /**
     * Similar to JBTabbedPane, except, the tab headers are small
     */
    fun tabbedBodyAndHeaderPanel(): JPanel {
        val outputWithController = createPanelWithLeftControls(
            createToggleButton("Wrap", AllIcons.Actions.ToggleSoftWrap) {
                jOutput.wrapStyleWord = it
                jOutput.lineWrap = it
            },
            minWidth = 250,
            bottomComponent = JBScrollPane(jOutput)
        )

        val cardLayout = CardLayout()
        val cardPanel = JPanel(cardLayout).apply {
            add(outputWithController, "_body")
            add(setUpResponseHeaderTable(), "_header")
        }

        val bodyLabel = JBLabel("Body")
        val headerLabel = JBLabel("Header")

        bodyLabel.apply {
            cursor = getPredefinedCursor(Cursor.HAND_CURSOR)
            foreground = JBColor.blue
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color.DARK_GRAY, Color.GRAY))
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    cardLayout.show(cardPanel, "_body")
                    // Swap border based on selected tab
                    headerLabel.border = null
                    border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color.DARK_GRAY, Color.GRAY))
                }
            })
        }

        headerLabel.apply {
            cursor = getPredefinedCursor(Cursor.HAND_CURSOR)
            foreground = JBColor.blue
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    cardLayout.show(cardPanel, "_header")
                    // Swap border based on selected tab
                    bodyLabel.border = null
                    border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color.DARK_GRAY, Color.GRAY))

                }
            })
        }

        JBLabel().apply {
            jOutputLabel = this
            accessibleContext.accessibleName = "Request output label"

            // Set these value so that jOutputLabel is properly pushed to right-end
            minimumSize = Dimension(200, preferredSize.height)
            preferredSize = Dimension(200, preferredSize.height)
            horizontalAlignment = SwingConstants.RIGHT
        }

        val headerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(bodyLabel)
            add(Box.createHorizontalStrut(10))
            add(headerLabel)
            add(Box.createHorizontalGlue())
            add(jOutputLabel)
            add(Box.createHorizontalStrut(6))
        }

        return JPanel(BorderLayout()).apply {
            add(headerPanel, BorderLayout.NORTH)
            add(cardPanel, BorderLayout.CENTER)
        }
    }

    private fun setUpHeaderTable(): JComponent {
        TableView<Array<String>>().apply {
            jHeader = this
            accessibleContext.accessibleName = "Request header table"
            model = headerTableModel
            applySharedTableDnd()
            applySharedTableDecorations()
            headerTableModel.items = requestDetailInMemory.header
        }

        return createPanelWithTopControls(
            createActionButton("Add", AllIcons.General.Add) { onAddRowClicked() }.apply {
                accessibleContext.accessibleName = "Add header row"
            },
            createActionButton("Remove", AllIcons.General.Remove) { onDeleteRowClicked() },
            minWidth = 350,
            bottomComponent = JBScrollPane(jHeader),
        )
    }

    private fun setUpResponseHeaderTable(): JBScrollPane {
        return JBScrollPane(
            TableView<Array<String>>().apply {
                jResponseHeader = this
                model = responseTableModel
                applySharedTableDecorations()
            }
        ).apply {
            border = CompoundBorder(
                JBUI.Borders.empty(3),
                LineBorder(JBColor.border(), 1),
            )
        }
    }

    private fun onAddRowClicked() {
        headerTableModel.addRow(arrayOf("", ""))
        jHeader.setRowSelectionInterval(
            headerTableModel.items.lastIndex,
            headerTableModel.items.lastIndex
        )
    }

    private fun onDeleteRowClicked() {
        val selectedRow = jHeader.selectedRow
        if (selectedRow == -1) return
        headerTableModel.removeRow(selectedRow)

        // Fix: Force selection of next row to avoid black cell issue
        if (headerTableModel.items.isNotEmpty()) {
            when (selectedRow) {
                0 -> jHeader.setRowSelectionInterval(0, 0)
                else -> jHeader.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
            }
        }
    }

    /**
     * Gson cannot pretty print if json contains variables like this:
     * {
     * "key": ${{count}}
     * }
     *
     * So, first replace ${{count}} with a long, e.g. 1234
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
        jOutputLabel.text = "[Loading...]"

        networkJob?.cancel()
        networkJob = uiScope.launch(CoroutineExceptionHandler { _, e ->
            jOutput.text = "${e.message}\n${e.stackTraceToString()}"
            jOutputLabel.text = "<html><font color='red'>[Exception]</font></html>"
        }) {
            jOutput.text = "[Loading...]"

            val output = coreRepository.executeRequest(
                requestDetail = requestDetailInMemory,
                variables = getVariables(),
            )

            setOutputBody(output)

            val color = when (output.code < 400) {
                true -> COLOR_GREEN
                false -> COLOR_RED
            }

            jOutputLabel.text = "<html><font color='$color'>[${output.code}</font> ${output.message}]</html>"
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

        // Populate response header table
        val data = ArrayList<Array<String>>()
        for ((key, values) in output.responseHeaders) {
            val value = values.joinToString("\n")
            data.add(arrayOf(key, value))
        }
        responseTableModel.items = data
    }
}
