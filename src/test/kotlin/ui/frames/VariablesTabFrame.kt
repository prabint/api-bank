package ui.frames

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.fixtures.JTableFixture
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.variablesTabFrame(function: VariablesTabFrame.() -> Unit) {
    find(VariablesTabFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("Variables Frame")
@DefaultXpath("title API Request Editor", "//div[@title='API Request Editor' and @class='MyDialog']")
class VariablesTabFrame(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : CommonContainerFixture(remoteRobot, remoteComponent) {
    val tab
        get() = jLabel("Variables")

    val addNewEnvironmentButton
        get() = button(byXpath("//div[@accessiblename='Add new environment variable set']"))

    val nameField
        get() = textField(byXpath("//div[@accessiblename='Variable collection name']"))

    val collectionList
        get() = jList(byXpath("//div[@class='JBList']"))

    val addNewRowButton
        get() = button(byXpath("//div[@accessiblename='Add new row']"))

    val variablesTable
        get() = remoteRobot.find(JTableFixture::class.java, byXpath("//div[@accessiblename='Variables table']"))
}
