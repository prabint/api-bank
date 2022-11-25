package api.bank.utils.dispatcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing

interface DispatcherProvider {
    val main
        get() = Dispatchers.Swing
    val io
        get() = Dispatchers.IO
}
