package api.bank.utils.listener

import java.awt.event.WindowEvent
import java.awt.event.WindowListener

interface SimpleWindowListener : WindowListener {
    override fun windowDeiconified(e: WindowEvent?) {
    }

    override fun windowIconified(e: WindowEvent?) {
    }

    override fun windowOpened(e: WindowEvent?) {
    }

    override fun windowActivated(e: WindowEvent?) {
    }

    override fun windowClosing(e: WindowEvent?) {
    }

    override fun windowDeactivated(e: WindowEvent?) {
    }
}
