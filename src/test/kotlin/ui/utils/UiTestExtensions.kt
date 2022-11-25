package ui.utils

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.JTableFixture
import com.intellij.remoterobot.utils.keyboard

internal fun JTableFixture.fillTable(
    remoteRobot: RemoteRobot,
    keyValuePairs: List<List<String>>,
) = with(remoteRobot) {
    for (i in keyValuePairs.indices) {
        val pair = keyValuePairs[i]
        val key = pair[0]
        val value = pair[1]

        clickCell(i, 0)
        keyboard { enterText(key, 0) }

        clickCell(i, 1)
        keyboard { enterText(value, 0) }
    }
}
