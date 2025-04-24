package ui.frames

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.requestsTabFrame(function: RequestsTabFrame.() -> Unit) {
    find(RequestsTabFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Requests Tab Frame")
@DefaultXpath("title API Request Editor", "//div[@title='API Request Editor' and @class='MyDialog']")
class RequestsTabFrame(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : CommonContainerFixture(remoteRobot, remoteComponent) {
    val tab
        get() = jLabel("Requests")

    val requestsList
        get() = find(
            JTreeFixture::class.java,
            byXpath("//div[@class='Tree']")
        )

    val addNewRequestButton
        get() = button(byXpath("//div[@accessiblename='Add new request']"))

    val addNewGroupPopUpButton
        get() = button(byXpath("//div[@class='JBMenuItem']"))

    val cloneRequestButton
        get() = button(byXpath("//div[@myicon='copy.svg']"))

    // textField(...) not working for dialogs
    val groupNameField
        get() = remoteRobot.find(
            JTextFieldFixture::class.java,
            byXpath("//div[@class='JTextField']")
        )

    val okButton
        get() = remoteRobot.find(
            JButtonFixture::class.java,
            byXpath("//div[@text='OK']")
        )

    val nameField
        get() = textField(byXpath("//div[@accessiblename='Request name']"))

    val executeButton
        get() = button(byXpath("//div[@accessiblename='Execute api']"))

    val methodField
        get() = comboBox(byXpath("//div[@accessiblename='Request method']"))

    val urlField
        get() = textField(byXpath("//div[@accessiblename='Request url']"))

    val headerTable
        get() = remoteRobot.find(JTableFixture::class.java, byXpath("//div[@accessiblename='Request header table']"))

    val headerTableAddRowButton
        get() = button(byXpath("//div[@accessiblename='Add header row']"))

    val formatBodyButton
        get() = button(byXpath("//div[@accessiblename='Format body']"))

    val bodyField
        get() = textField(byXpath("//div[@accessiblename='Request body']"))

    val outputLabel
        get() = jLabel(byXpath("//div[@accessiblename='Request output label']"))

    val outputField
        get() = textArea(byXpath("//div[@accessiblename='Request output']"))
}
