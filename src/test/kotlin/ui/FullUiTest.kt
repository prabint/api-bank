package ui

import api.bank.models.Constants.COLOR_GREEN
import api.bank.utils.toVariableRepresentation
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.JButtonFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ui.frames.*
import ui.utils.RemoteRobotExtension
import ui.utils.TestData.formattedBody
import ui.utils.TestData.headerData
import ui.utils.TestData.unFormattedBody
import ui.utils.TestData.uuidRegex
import ui.utils.TestData.varBaseUrl
import ui.utils.TestData.variablesData
import ui.utils.fillTable
import java.awt.event.KeyEvent
import java.time.Duration.ofSeconds

/**
 * TODO: Update
 *
 * To run the test:
 * 1. ./gradlew clean && ./gradlew runIdeForUiTests & ./gradlew test --tests "FullUiTest.verify requests and variables data"
 * 2. Sit back and let the test complete
 */
@ExtendWith(RemoteRobotExtension::class)
class FullUiTest {

    @BeforeEach
    fun waitForIde(remoteRobot: RemoteRobot) {
        waitForIgnoringError(ofSeconds(30)) { remoteRobot.callJs("true") }
    }

    @Test
    fun `verify requests and variables data`(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {

        welcomeFrame { createNewProjectLink.click() }

        newProjectFrame {
            emptyProjectButton.click()
            projectNameFiled.text = ""
            projectNameFiled.text = "api-bank-ui-test"
            finishButton.click()

            try {
                while (fileAlreadyExistLabel.isVisible()) {
                    yesButton.click()
                }
            } catch (ignore: Exception) {
            }
        }

        idea {
            isDumbMode().not()
            showMainDialog(remoteRobot)
        }

        variablesTabFrame {
            tab.click()
            addNewEnvironmentButton.click()

            nameField.text = "Gamma"
            assertEquals("Gamma", collectionList.collectItems()[0])

            repeat(variablesData.size) { addNewRowButton.click() }
            variablesTable.fillTable(remoteRobot, variablesData)
        }

        requestsTabFrame {
            tab.click()
            addNewRequestButton.click()

            nameField.text = "Create user"
            methodField.selectItem("POST")

            assertEquals("[POST] Create user", requestsList.collectItems()[0])

            urlField.text = "${varBaseUrl.toVariableRepresentation()}/posts"

            headerTableAddRowButton.click()
            headerTable.fillTable(remoteRobot, headerData)

            bodyField.text = unFormattedBody

            formatBodyButton.click()
            assertEquals(formattedBody, bodyField.text)

            executeButton.click()
            verifyOutput(this)
        }

        find(JButtonFixture::class.java, byXpath("//div[@text='Save']")).click()

        // Test quick select popup
        showQuickSelectPopup(this)
        quickSelectFrame { list.clickItemAtIndex(0) }
        idea { assert(notification201Created.findAllText()[0].text == "201 Created") }

        // Open editor from quick select popup
        showQuickSelectPopup(this)
        quickSelectFrame { list.clickItemAtIndex(1) }

        // Verify all data in editor is retained and valid
        requestsTabFrame {
            assertEquals("[POST] Create user", requestsList.collectItems()[0])
            assertEquals("Create user", nameField.text)
            assertEquals("POST", methodField.selectedText())
            assertEquals("${varBaseUrl.toVariableRepresentation()}/posts", urlField.text)
            assertArrayEquals(headerData.toTypedArray(), headerTable.collectItems().toTypedArray())
            assertEquals(formattedBody, bodyField.text)

            executeButton.click()
            verifyOutput(this)
        }

        variablesTabFrame {
            tab.click()
            assertEquals("Gamma", collectionList.collectItems()[0])
            assertEquals("Gamma", nameField.text)
            assertEquals(variablesData, variablesTable.collectItems())
        }

        // Clone then search
        requestsTabFrame {
            // Clone
            tab.click()
            cloneRequestButton.click()
            nameField.text = "Get All Data"

            // Search
            searchField.click()
            keyboard { enterText("all", 0) }
            assertEquals(requestsList.collectItems().size, 1)
            assertEquals("[POST] Get All Data", requestsList.collectItems()[0])
        }

        find(JButtonFixture::class.java, byXpath("//div[@text='Save']")).click()
    }

    @AfterEach
    fun closeProject(remoteRobot: RemoteRobot) {
        // FIXME: This closes dev IDE as well
        //  idea { menuBar.select("File", "Close Project") }
    }

    private fun showMainDialog(remoteRobot: RemoteRobot) {
        remoteRobot.keyboard { hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, KeyEvent.VK_E) }
    }

    private fun showQuickSelectPopup(remoteRobot: RemoteRobot) {
        remoteRobot.keyboard { hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, KeyEvent.VK_S) }
    }

    private fun verifyOutput(requestsTabFrame: RequestsTabFrame) {
        waitFor(ofSeconds(5)) {
            requestsTabFrame.outputLabel.value == "<html><font color='${COLOR_GREEN}'>201</font> Created</html>"
        }

        val lines = requestsTabFrame.outputField.text.lines()
        assertEquals("{", lines[0])
        assertEquals("""  "body": "Hello World",""", lines[1])
        assertEquals("""  "isReal": true,""", lines[2])
        assertEquals("""  "count": 599,""", lines[3])
        assertEquals("""  "id": 101""", lines[5])
        assertEquals("}", lines[6])

        val tokenUUID = lines[4].split(":")
        assert(tokenUUID[0] == "  \"token\"")
        assert(tokenUUID[1].drop(2).dropLast(2).matches(uuidRegex))
    }
}
