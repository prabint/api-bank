// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package ui.frames

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

fun RemoteRobot.newProjectFrame(function: NewProjectFrame.() -> Unit) {
    find(NewProjectFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

@FixtureName("NewProject Frame")
@DefaultXpath("type", "//*[contains(@title.key, 'title.new.project')]")
class NewProjectFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
    CommonContainerFixture(remoteRobot, remoteComponent) {
    val emptyProjectButton
        get() = findText("Empty Project")

    val projectNameFiled
        get() = textField(byXpath("//div[@class='JBTextField']"))

    val createButton
        get() = button(byXpath("//div[@text='Create']"))
}
