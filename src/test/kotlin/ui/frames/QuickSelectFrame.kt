package ui.frames

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.quickSelectFrame(function: QuickSelectFrame.() -> Unit) {
    find(QuickSelectFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("QuickSelect Frame")
@DefaultXpath("type", "//div[@class='JRootPane']")
class QuickSelectFrame(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : CommonContainerFixture(remoteRobot, remoteComponent) {
    val list
        get() = jList(byXpath("//div[@class='MyList']"))
}
