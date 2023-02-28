package ui.frames

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.fixtures.JTableFixture
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
        get() = jList(byXpath("//div[@accessiblename='Requests list']"))

    val addNewRequestButton
        get() = button(byXpath("//div[@accessiblename='Add new request']"))

    val cloneRequestButton
        get() = button(byXpath("//div[@accessiblename='Clone request']"))

    val nameField
        get() = textField(byXpath("//div[@accessiblename='Request name']"))

    val searchField
        get() = textField(byXpath("//div[@accessiblename='Search field']"))

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
