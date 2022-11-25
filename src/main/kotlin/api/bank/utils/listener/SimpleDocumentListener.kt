package api.bank.utils.listener

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

abstract class SimpleDocumentListener : DocumentListener {
    abstract fun update(e: DocumentEvent?)

    override fun insertUpdate(e: DocumentEvent?) {
        update(e)
    }

    override fun removeUpdate(e: DocumentEvent?) {
        update(e)
    }

    override fun changedUpdate(e: DocumentEvent?) {
        update(e)
    }
}
