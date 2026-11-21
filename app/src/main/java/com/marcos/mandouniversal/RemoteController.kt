package com.marcos.mandouniversal

import android.content.Context
import com.marcos.mandouniversal.data.SavedRemotesStore
import com.marcos.mandouniversal.data.Signal
import com.marcos.mandouniversal.ir.IrTransmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/** Envía las teclas en un hilo propio (en orden) y da acceso a los mandos guardados. */
class RemoteController(val context: Context) {

    private val transmitter = IrTransmitter(context)
    private val irDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + irDispatcher)

    val saved = SavedRemotesStore(context)

    init { com.marcos.mandouniversal.data.AcProtocols.init(context) }

    /** Bit "toggle" de RC5/RC6: cambia en cada pulsación nueva, no al mantener. */
    private var toggle = false

    val hasIrEmitter: Boolean get() = transmitter.isAvailable
    val emitterRanges: String get() = transmitter.frequencyRanges

    fun send(signal: Signal, isRepeat: Boolean = false) {
        if (!isRepeat) toggle = !toggle
        val t = toggle
        scope.launch {
            try {
                transmitter.transmit(signal.toIr(t))
            } catch (e: Exception) {
                android.util.Log.e("RemoteController", "No se pudo codificar ${signal.label}", e)
            }
        }
    }
}
