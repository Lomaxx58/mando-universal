package com.marcos.mandouniversal.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

/** Envoltorio del emisor de infrarrojos del móvil. */
class IrTransmitter(context: Context) {

    private val ir: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /** true si el móvil tiene emisor de infrarrojos. */
    val isAvailable: Boolean
        get() = try { ir?.hasIrEmitter() == true } catch (e: Exception) { false }

    /** Rangos de frecuencia que admite el emisor, p. ej. "30–60 kHz". */
    val frequencyRanges: String
        get() = try {
            ir?.carrierFrequencies?.joinToString(", ") { "${it.minFrequency / 1000}–${it.maxFrequency / 1000} kHz" }
                ?.ifEmpty { null } ?: "desconocido"
        } catch (e: Exception) { "desconocido" }

    /**
     * Envía la señal. Es una llamada bloqueante (dura lo que dura la señal),
     * así que hay que llamarla fuera del hilo principal.
     */
    fun transmit(signal: IrSignal): Boolean {
        val manager = ir ?: return false
        if (!isAvailable || signal.pattern.isEmpty()) return false
        return try {
            manager.transmit(signal.frequency, signal.pattern)
            true
        } catch (e: Exception) {
            Log.e("IrTransmitter", "Error al emitir", e)
            false
        }
    }
}
