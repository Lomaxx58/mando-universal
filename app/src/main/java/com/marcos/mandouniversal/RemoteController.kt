package com.marcos.mandouniversal

import android.content.Context
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Brands
import com.marcos.mandouniversal.data.CodeSet
import com.marcos.mandouniversal.ir.IrCodec
import com.marcos.mandouniversal.ir.IrCommand
import com.marcos.mandouniversal.ir.IrTransmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Envía comandos en un hilo propio (en orden) y recuerda la marca elegida.
 */
class RemoteController(context: Context) {

    private val transmitter = IrTransmitter(context)
    private val prefs = context.getSharedPreferences("mando", Context.MODE_PRIVATE)
    private val irDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + irDispatcher)

    /** Bit "toggle" de Philips: cambia en cada pulsación nueva, no al mantener. */
    private var toggle = false

    val hasIrEmitter: Boolean get() = transmitter.isAvailable

    fun send(command: IrCommand, isRepeat: Boolean = false) {
        if (!isRepeat) toggle = !toggle
        val signal = IrCodec.encode(command, toggle)
        scope.launch { transmitter.transmit(signal) }
    }

    // ---- Marca guardada ----

    fun savedBrand(): Brand? = Brands.byId(prefs.getString("brand", null))

    fun savedCodeSet(brand: Brand): CodeSet {
        val index = prefs.getInt("codeset_${brand.id}", 0)
        return brand.codeSets.getOrElse(index) { brand.codeSets.first() }
    }

    fun save(brand: Brand, codeSet: CodeSet) {
        prefs.edit()
            .putString("brand", brand.id)
            .putInt("codeset_${brand.id}", brand.codeSets.indexOf(codeSet).coerceAtLeast(0))
            .apply()
    }
}
