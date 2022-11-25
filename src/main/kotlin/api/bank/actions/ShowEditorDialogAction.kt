package api.bank.actions

import api.bank.multitab.MainDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowEditorDialogAction : AnAction("Request Editor") {
    override fun actionPerformed(action: AnActionEvent) {
        MainDialog(action.project!!).show()
    }
}
