package api.bank.utils

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

fun createToggleButton(text: String, icon: Icon, onClick: (Boolean) -> Unit): ActionButton {
    var localState = false

    return ActionButton(
        object : ToggleAction() {
            override fun isSelected(e: AnActionEvent): Boolean {
                return localState
            }

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                localState = state
                onClick(state)
            }
        },
        Presentation().apply {
            this.icon = icon
            this.text = text
        },
        ActionPlaces.UNKNOWN,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
}

/**
 * Helper method to quickly create an action button.
 */
fun createActionButton(text: String, icon: Icon, onClick: () -> Unit): ActionButton {
    return ActionButton(
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                onClick()
            }
        },
        Presentation().apply {
            this.icon = icon
            this.text = text
        },
        ActionPlaces.UNKNOWN,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
}

/** Creates UI with buttons on top and main component below it
----------------------
|A| |B|              |
----------------------
|                    |
|                    |
|                    |
|        MAIN        |
|                    |
|                    |
|                    |
|                    |
----------------------
 */
fun createPanelWithTopControls(
    vararg actionButtons: ActionButton,
    bottomComponent: JComponent,
    minWidth: Int,
): JPanel {
    val mainPanel = JPanel()
    val width = actionButtons.size
    mainPanel.layout = GridBagLayout()

    val rootGbc = GridBagConstraints()

    // ---------------
    // Top controllers
    // ---------------
    val controllerPanel = JPanel(GridBagLayout())
    val controllerConstraints = GridBagConstraints()
    controllerConstraints.fill = GridBagConstraints.NONE
    controllerConstraints.anchor = GridBagConstraints.NORTHWEST

    repeat(actionButtons.size) {
        controllerConstraints.gridx = it
        controllerPanel.add(actionButtons[it], controllerConstraints)
    }

    controllerPanel.border = BorderFactory.createMatteBorder(1, 1, 0, 1, JBColor.border())

    controllerConstraints.pushToNorthwest(controllerPanel)

    // Add controller to main panel
    rootGbc.gridwidth = width
    rootGbc.fill = GridBagConstraints.BOTH
    mainPanel.add(controllerPanel, rootGbc)

    // ------------
    // Bottom panel
    // ------------
    rootGbc.fill = GridBagConstraints.BOTH
    rootGbc.gridx = 0
    rootGbc.gridy = 1
    rootGbc.gridwidth = width

    bottomComponent.border = BorderFactory.createLineBorder(JBColor.border())
    mainPanel.add(bottomComponent, rootGbc)

    rootGbc.pushToNorthwest(mainPanel)

    mainPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
    mainPanel.minimumSize = Dimension(minWidth, 0)
    return mainPanel
}

/** Creates UI with buttons on left and main component to right
----------------------
| A |                 |
| B |                 |
|   |                 |
|   |                 |
|   |     MAIN        |
|   |                 |
|   |                 |
|   |                 |
|   |                 |
-----------------------
 */
fun createPanelWithLeftControls(
    vararg actionButtons: ActionButton,
    bottomComponent: JComponent,
    minWidth: Int,
): JPanel {
    val mainPanel = JPanel()
    val height = actionButtons.size
    mainPanel.layout = GridBagLayout()

    val rootGbc = GridBagConstraints()

    // ---------------
    // Left controllers
    // ---------------
    val controllerPanel = JPanel(GridBagLayout())
    val controllerConstraints = GridBagConstraints()
    controllerConstraints.fill = GridBagConstraints.NONE
    controllerConstraints.anchor = GridBagConstraints.NORTHWEST

    repeat(actionButtons.size) {
        controllerConstraints.gridy = it
        controllerPanel.add(actionButtons[it], controllerConstraints)
    }

    controllerPanel.border = BorderFactory.createMatteBorder(1, 1, 1, 0, JBColor.border())

    controllerConstraints.pushToNorthwest(controllerPanel)

    // Add controller to main panel
    rootGbc.gridheight = height
    rootGbc.fill = GridBagConstraints.BOTH
    mainPanel.add(controllerPanel, rootGbc)

    // ------------
    // Right panel
    // ------------
    rootGbc.fill = GridBagConstraints.BOTH
    rootGbc.gridx = 1
    rootGbc.gridy = 0
    rootGbc.gridwidth = height

    bottomComponent.border = BorderFactory.createLineBorder(JBColor.border())
    mainPanel.add(bottomComponent, rootGbc)

    rootGbc.pushToNorthwest(mainPanel)

    mainPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
    mainPanel.minimumSize = Dimension(minWidth, 0)
    return mainPanel
}

/**
 * Helper method to push all contents of GridBagLayout to top-left.
 * By default, the contents are centered.
 */
fun GridBagConstraints.pushToNorthwest(panel: JPanel) {
    weighty = 1.0
    weightx = 1.0
    panel.add(Box.createGlue(), this)
}

val monoSpacedFont: JBFont = JBFont.create(Font(JBFont.MONOSPACED, Font.PLAIN, 12))
